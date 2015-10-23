package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Consultant class allows you to retrieve the configuration for your application from Consul, and at the same
 * time subscribe to changes to that configuration.
 */
public class Consultant {

	/**
	 * Allows you to build a custom Consultant object.
	 */
	public static class Builder {

		private ScheduledExecutorService executor;
		private String host;
		private ServiceIdentifier id;
		private ConfigListener listener;
		private ConfigValidator validator;
		private CloseableHttpClient http;

		private Builder() {
			// Prevent instantiation.
		}

		/**
		 * States that Consultant should use a specific executor service for listening to configuration updates. If
		 * no executor service is specified, Consultant will create a private executor service. This executor service
		 * will automatically be cleaned up when the JVM terminates, or when shutdown() is called on the Consultant
		 * object.
		 *
		 * @param executor The ScheduledExecutorService to use to schedule jobs on.
		 * @return The Builder instance.
		 */
		public Builder usingExecutor(ScheduledExecutorService executor) {
			this.executor = executor;
			return this;
		}

		/**
		 * Specifies where the Consul REST API can be reached on. Alternatively you can specify this through the
		 * environment variable <code>CONSUL_HOST=http://localhost</code>.
		 *
		 * @param host The host where the Consul REST API can be found.
		 * @return The Builder instance.
		 */
		public Builder withConsulHost(String host) {
			this.host = host;
			return this;
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you must define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code> and
		 * <code>SERVICE_HOST</code>.
		 *
		 * @param serviceName The name of this service.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName) {
			return identifyAs(serviceName, fromEnvironment("SERVICE_DC"),
					fromEnvironment("SERVICE_HOST"),
					Optional.ofNullable(fromEnvironment("SERVICE_INSTANCE"))
							.orElse(UUID.randomUUID().toString()));
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you must define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code> and
		 * <code>SERVICE_HOST</code>.
		 *
		 * @param serviceName The name of this service.
		 * @param datacenter The name of the datacenter where this service is running in.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName, String datacenter) {
			return identifyAs(serviceName, datacenter, fromEnvironment("SERVICE_HOST"),
					Optional.ofNullable(fromEnvironment("SERVICE_INSTANCE"))
							.orElse(UUID.randomUUID().toString()));
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you must define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code> and
		 * <code>SERVICE_HOST</code>.
		 *
		 * @param serviceName The name of this service.
		 * @param datacenter The name of the datacenter where this service is running in.
		 * @param hostname The name of the host where this service is running on.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName, String datacenter, String hostname) {
			return identifyAs(serviceName, datacenter, hostname,
					Optional.ofNullable(fromEnvironment("SERVICE_INSTANCE"))
							.orElse(UUID.randomUUID().toString()));
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you must define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code>,
		 * <code>SERVICE_HOST</code>, and <code>SERVICE_INSTANCE</code>.
		 *
		 * @param serviceName The name of this service.
		 * @param datacenter The name of the datacenter where this service is running in.
		 * @param hostname The name of the host where this service is running on.
		 * @param instanceName The name/role of this service instance.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName, String datacenter, String hostname, String instanceName) {
			checkArgument(!isNullOrEmpty(serviceName), "You must specify a 'serviceName'!");
			this.id = new ServiceIdentifier(serviceName, datacenter, hostname, instanceName);
			return this;
		}

		/**
		 * States that Consultant should use a specific HTTP client. If no HTTP client is specified, Consultant will
		 * create one itself.
		 *
		 * @param httpClient The CloseableHttpClient to use to communicate with Consul's API.
		 * @return The Builder instance.
		 */
		@VisibleForTesting
		Builder usingHttpClient(CloseableHttpClient httpClient) {
			this.http = httpClient;
			return this;
		}

		/**
		 * Specifies the callback listener which is notified of whenever a new and valid configuration is detected
		 * in Consul.
		 *
		 * @param listener The listener to call when a new valid configuration is detected.
		 * @return The Builder instance.
		 */
		public Builder onValidConfig(ConfigListener listener) {
			this.listener = listener;
			return this;
		}

		/**
		 * Specifies the validator which is used to determine if a new configuration detected in Consul is valid,
		 * and may be published through the ConfigListener callback.
		 *
		 * @param validator The validator to call when a new configuration is detected.
		 * @return The Builder instance.
		 */
		public Builder validateConfigWith(ConfigValidator validator) {
			this.validator = validator;
			return this;
		}

		/**
		 * Builds a new instance of the Consultant class using the specified arguments.
		 *
		 * @return The constructed Consultant object.
		 */
		public Consultant build() {
			if (isNullOrEmpty(host)) {
				host = fromEnvironment("CONSUL_HOST");
				if (isNullOrEmpty(host)) {
					host = "http://localhost:8500";
				}
			}

			boolean executorSpecified = true;
			if (executor == null) {
				executorSpecified = false;
				executor = new ScheduledThreadPoolExecutor(1);
			}

			if (http == null) {
				PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
				manager.setMaxTotal(5);
				manager.setDefaultMaxPerRoute(5);

				http = HttpClientBuilder.create()
						.setConnectionManager(manager)
						.build();
			}

			if (id == null) {
				id = new ServiceIdentifier(
						checkNotNull(fromEnvironment("SERVICE_NAME"),
								"You must specify the name of the service using SERVICE_NAME=<service_name>"),
						fromEnvironment("SERVICE_DC"),
						fromEnvironment("SERVICE_HOST"),
						Optional.ofNullable(fromEnvironment("SERVICE_INSTANCE"))
								.orElse(UUID.randomUUID().toString()));
			}

			Consultant consultant = new Consultant(executor, host, id, listener, validator, http);
			consultant.init();

			if (!executorSpecified) {
				Runtime.getRuntime().addShutdownHook(new Thread(consultant::shutdown));
			}

			return consultant;
		}

		private String fromEnvironment(String key) {
			String property = System.getProperty(key);
			if (property != null) {
				return property;
			}
			property = System.getenv(key);
			if (property != null) {
				return property;
			}
			return null;
		}
	}

	/**
	 * @return A Builder for the Consultant class.
	 */
	public static Builder builder() {
		return new Builder();
	}

	private static Logger log = LoggerFactory.getLogger(Consultant.class);

	private final CloseableHttpClient http;
	private final ScheduledExecutorService executor;
	private final String host;
	private final ServiceIdentifier id;
	private final ObjectMapper mapper;
	private final ConfigListener listener;
	private final ConfigValidator validator;
	private final Properties validated;

	private Consultant(ScheduledExecutorService executor, String host, ServiceIdentifier identifier,
			ConfigListener listener, ConfigValidator validator, CloseableHttpClient http) {

		this.mapper = new ObjectMapper();
		this.listener = listener;
		this.validator = validator;
		this.executor = executor;
		this.host = host;
		this.id = identifier;
		this.validated = new Properties();
		this.http = http;
	}

	private void init() {
		log.info("Fetching initial configuration from Consul...");
		ConfigUpdater poller = new ConfigUpdater(executor, http, host, null, id, mapper, null, (properties) -> {
			if (validator == null) {
				updateValidatedConfig(properties);
			}
			else {
				try {
					validator.validateConfig(properties);
					updateValidatedConfig(properties);
				}
				catch (RuntimeException e) {
					log.warn("New config did not pass validation: " + e.getMessage(), e);
				}
			}
		});

		try {
			executor.submit(poller).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private void updateValidatedConfig(Properties newConfig) {
		SetView<String> added = Sets.difference(newConfig.stringPropertyNames(), validated.stringPropertyNames());
		SetView<String> removed = Sets.difference(validated.stringPropertyNames(), newConfig.stringPropertyNames());

		added.forEach(key -> validated.setProperty(key, newConfig.getProperty(key)));
		removed.forEach(validated::remove);

		if (listener != null) {
			listener.onConfigUpdate(validated);
		}
	}

	/**
	 * Tears any outstanding resources down.
	 */
	public void shutdown() {
		executor.shutdownNow();
		try {
			http.close();
		}
		catch (IOException e) {
			log.error("Error occurred on shutdown: " + e.getMessage(), e);
		}
	}

	/**
	 * @return The identity of this service or application.
	 */
	public ServiceIdentifier getServiceIdentifier() {
		return id;
	}

	/**
	 * @return The current valid configuration.
	 */
	public Properties getProperties() {
		return validated;
	}

}
