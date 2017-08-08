package me.magnet.consultant;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigUpdater implements Runnable {


	private static class Setting {

		private final ServiceIdentifier identifier;
		private final String value;

		public Setting(ServiceIdentifier identifier, String value) {
			this.identifier = identifier;
			this.value = value;
		}

		public ServiceIdentifier getIdentifier() {
			return identifier;
		}

		public String getValue() {
			return value;
		}

	}

	private static final Logger log = LoggerFactory.getLogger(ConfigUpdater.class);

	private static final String PREFIX = "config";

	private final CloseableHttpClient httpClient;
	private final ScheduledExecutorService executor;
	private final URI consulURI;
	private final ServiceIdentifier identifier;
	private final ObjectMapper objectMapper;
	private final Properties config;
	private final ConfigListener listener;
	private final String kvPrefix;
	private final AtomicBoolean shutdownBegun = new AtomicBoolean();
	private final AtomicReference<HttpGet> request = new AtomicReference<>();
	private String consulIndex;

	ConfigUpdater(ScheduledExecutorService executor, CloseableHttpClient httpClient, URI consulURI,
			String consulIndex, ServiceIdentifier identifier, ObjectMapper objectMapper,
			Properties config, ConfigListener listener, String kvPrefix) {

		this.httpClient = httpClient;
		this.consulIndex = consulIndex;
		this.objectMapper = objectMapper;
		this.executor = executor;
		this.consulURI = consulURI;
		this.identifier = identifier;
		this.listener = listener;
		this.config = Optional.ofNullable(config).orElse(new Properties());
		this.kvPrefix = Optional.ofNullable(kvPrefix).orElse(PREFIX);
	}

	@Override
	public void run() {
		if (shutdownBegun.get()) {
			log.info("Not retrieving new config since we're shutting down");
			return;
		}
		long timeout = 500;
		try {
			String url = consulURI + "/v1/kv/" + kvPrefix + "/" + identifier.getServiceName() + "/?recurse=true";
			if (consulIndex != null) {
				url += "&index=" + consulIndex;
			}

			request.set(new HttpGet(url));
			try (CloseableHttpResponse response = httpClient.execute(request.get())) {
				Properties newConfig = new Properties();
				int status = response.getStatusLine().getStatusCode();
				switch (status) {
					case 200:
						InputStream content = response.getEntity().getContent();
						TypeReference<List<KeyValueEntry>> type = new TypeReference<List<KeyValueEntry>>() {
						};
						List<KeyValueEntry> keys = objectMapper.readValue(content, type);
						newConfig = updateConfig(keys);
						onNewConfig(newConfig);

						consulIndex = response.getFirstHeader("X-Consul-Index").getValue();
						break;
					case 404:   // Not Found
						timeout = 5_000;
						onNewConfig(newConfig);
						break;
					case 204:   // No Content
					case 504:   // Gateway Timeout
						break;
					default:
						timeout = 60_000;
						String body = EntityUtils.toString(response.getEntity());
						throw new RuntimeException("Failed to retrieve new config", new ConsulException(status, body));
				}
			}
		}
		catch (IOException | RuntimeException e) {
			if (interruptedBecauseShutdown(e)) {
				return;
			}
			log.error("Error occurred while retrieving/publishing new config from Consul: " + e.getMessage(), e);
		}
		finally {
			if (!shutdownBegun.get()) {
				executor.schedule(this, timeout, TimeUnit.MILLISECONDS);
			}
		}
	}

	private boolean interruptedBecauseShutdown(Exception e) {
		return (e instanceof IllegalStateException
				&& "Connection pool shut down".equals(e.getMessage())
				&& shutdownBegun.get())
				|| e instanceof InterruptedException;
	}

	private void onNewConfig(Properties newConfig) {
		if (!config.equals(newConfig)) {
			PropertiesUtil.sync(newConfig, config);
			log.debug("New config detected in Consul: \n{}", newConfig.entrySet().stream()
					.map(entry -> "\t" + entry.getKey() + ": " + entry.getValue())
					.collect(Collectors.joining("\n")));

			if (listener != null) {
				listener.onConfigUpdate(config);
			}
		}
	}

	private Properties updateConfig(List<KeyValueEntry> entries) {
		Map<String, Setting> newConfig = Maps.newHashMap();

		for (KeyValueEntry entry : entries) {
			Path path = PathParser.parse(kvPrefix, entry.getKey());
			if (path == null || path.getKey() == null) {
				continue;
			}

			ServiceIdentifier id = path.getId();
			if (id.appliesTo(identifier)) {
				String settingKey = path.getKey();
				if (settingKey.isEmpty()) {
					continue;
				}

				Setting setting = newConfig.get(settingKey);
				if (setting == null || id.moreSpecificThan(setting.getIdentifier())) {
					String stringValue = new String(Base64.getDecoder().decode(entry.getValue()));
					newConfig.put(settingKey, new Setting(id, stringValue));
				}
			}
		}

		Properties properties = new Properties();
		newConfig.forEach((key, value) -> properties.setProperty(key, value.getValue()));
		return properties;
	}

	/**
	 * Shuts down any HTTP calls or scheduled calls to update the config.
	 */
	public void shutDown() {
		shutdownBegun.set(true);
		request.getAndUpdate(http -> {
			if (http != null) {
				try {
					request.get().abort();
				}
				catch (RuntimeException e) {
					log.error("Could not abort request", e);
				}
			}
			return null;
		});
	}
}
