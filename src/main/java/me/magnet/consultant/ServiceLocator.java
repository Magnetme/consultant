package me.magnet.consultant;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class ServiceLocator {

	private final URI consulUri;
	private final ObjectMapper objectMapper;
	private final CloseableHttpClient http;
	private final Optional<String> datacenter;

	ServiceLocator(Optional<String> datacenter, URI consulUri, ObjectMapper objectMapper, CloseableHttpClient http) {
		this.datacenter = datacenter;
		this.consulUri = consulUri;
		this.objectMapper = objectMapper;
		this.http = http;
	}

	public Optional<String> getDatacenter() {
		return datacenter;
	}

	public List<ServiceInstance> listInstances(String serviceName) {
		return listInstances(serviceName, null);
	}

	public List<ServiceInstance> listInstances(String serviceName, String datacenter) {
		String url = consulUri + "/v1/health/service/" + serviceName + "?passing&near=_agent";
		if (!Strings.isNullOrEmpty(datacenter)) {
			url += "&dc=" + datacenter;
		}

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

	public List<String> listDatacenters() {
		String url = consulUri + "/v1/catalog/datacenters";

		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", "Consultant");
		try (CloseableHttpResponse response = http.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 400) {
				InputStream content = response.getEntity().getContent();
				return objectMapper.readValue(content, new TypeReference<List<String>>() {});
			}
			throw new ConsultantException("Could not locate datacenters. Consul returned: " + statusCode);
		}
		catch (IOException | RuntimeException e) {
			throw new ConsultantException(e);
		}
	}

}
