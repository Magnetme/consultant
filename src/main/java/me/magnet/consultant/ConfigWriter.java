package me.magnet.consultant;

import static me.magnet.consultant.Consultant.CONFIG_PREFIX;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigWriter {

	private static final Logger log = LoggerFactory.getLogger(ConfigWriter.class);

	private final CloseableHttpClient httpClient;
	private final URI consulURI;
	private final String token;
	private final String kvPrefix;

	ConfigWriter(CloseableHttpClient httpClient, URI consulURI, String token, String kvPrefix) {
		this.httpClient = httpClient;
		this.consulURI = consulURI;
		this.token = token;
		this.kvPrefix = Optional.ofNullable(kvPrefix).orElse(CONFIG_PREFIX);
	}

	public boolean setConfig(ServiceIdentifier identifier, String key, String value) {
		try {
			if (value != null) {
				return put(identifier, key, value);
			}
			return delete(identifier, key);
		}
		catch (IOException | RuntimeException e) {
			log.error("Error occurred while pushing new config from Consul: " + e.getMessage(), e);
			return false;
		}
	}

	private boolean put(ServiceIdentifier identifier, String key, String value) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(kvPrefix)
				.append("/")
				.append(identifier.getServiceName())
				.append("/");

		String identifierPrefix = createServiceIdentifierPrefix(identifier);
		if (!Strings.isNullOrEmpty(identifierPrefix)) {
			builder.append(identifierPrefix);
		}

		builder.append(key);

		String path = URLEncoder.encode(builder.toString(), "UTF-8");
		String url = consulURI + "/v1/kv/" + path;

		HttpPut request = new HttpPut(url);
		request.setEntity(new StringEntity(value));
		if (!Strings.isNullOrEmpty(token)) {
			request.setHeader("X-Consul-Token", token);
		}

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			String body = EntityUtils.toString(response.getEntity());
			return "true".equalsIgnoreCase(body);
		}
	}

	private boolean delete(ServiceIdentifier identifier, String key) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(kvPrefix)
				.append("/")
				.append(identifier.getServiceName())
				.append("/");

		String identifierPrefix = createServiceIdentifierPrefix(identifier);
		if (!Strings.isNullOrEmpty(identifierPrefix)) {
			builder.append(identifierPrefix);
		}

		builder.append(key);

		String path = URLEncoder.encode(builder.toString(), "UTF-8");
		String url = consulURI + "/v1/kv/" + path;

		HttpDelete request = new HttpDelete(url);
		if (!Strings.isNullOrEmpty(token)) {
			request.setHeader("X-Consul-Token", token);
		}

		try (CloseableHttpResponse response = httpClient.execute(request)) {
			String body = EntityUtils.toString(response.getEntity());
			return "true".equalsIgnoreCase(body);
		}
	}

	private String createServiceIdentifierPrefix(ServiceIdentifier identifier) {
		List<String> parts = Lists.newArrayList();

		identifier.getDatacenter()
				.map(value -> "dc=" + value)
				.ifPresent(parts::add);

		identifier.getHostName()
				.map(value -> "host=" + value)
				.ifPresent(parts::add);

		identifier.getInstance()
				.map(value -> "instance=" + value)
				.ifPresent(parts::add);

		if (parts.isEmpty()) {
			return null;
		}

		return parts.stream()
				.collect(Collectors.joining(",", "[", "]"));
	}

}
