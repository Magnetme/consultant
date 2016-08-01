package me.magnet.consultant;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class ServiceLocator {

	private final URI consulUri;
	private final ObjectMapper objectMapper;
	private final CloseableHttpClient http;

	ServiceLocator(URI consulUri, ObjectMapper objectMapper, CloseableHttpClient http) {
		this.consulUri = consulUri;
		this.objectMapper = objectMapper;
		this.http = http;
	}

	public List<ServiceInstance> listInstances(String serviceName) {
		String url = consulUri + "/v1/health/service/" + serviceName + "?passing&near=_agent";

		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", "Consultant");
		try (CloseableHttpResponse response = http.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 400) {
				InputStream content = response.getEntity().getContent();
				return objectMapper.readValue(content, new TypeReference<List<ServiceInstance>>() {});
			}
			throw new ConsultantException("Could not locate service: " + serviceName + ". Consul returned: " + statusCode);
		}
		catch (IOException | RuntimeException e) {
			throw new ConsultantException(e);
		}
	}

}
