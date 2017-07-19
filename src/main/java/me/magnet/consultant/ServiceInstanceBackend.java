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
import org.apache.http.util.EntityUtils;

/**
 * A class which can be used to retrieve lists of instances or datacenters from Consul over its HTTP API.
 */
public class ServiceInstanceBackend {

	private final URI consulUri;
	private final ObjectMapper objectMapper;
	private final CloseableHttpClient http;
	private final Optional<String> datacenter;

	/**
	 * Constructs a new ServiceInstanceBackend object.
	 *
	 * @param datacenter   The datacenter as it is defined in the ServiceIdentifier.
	 * @param consulUri    The URI where Consul's API can be found.
	 * @param objectMapper The ObjectMapper which can be used to deserialize JSON.
	 * @param http         The HTTP client to use.
	 */
	ServiceInstanceBackend(Optional<String> datacenter, URI consulUri, ObjectMapper objectMapper,
			CloseableHttpClient http) {

		this.datacenter = datacenter;
		this.consulUri = consulUri;
		this.objectMapper = objectMapper;
		this.http = http;
	}

	/**
	 * @return The name of the local datacenter.
	 */
	public Optional<String> getDatacenter() {
		return datacenter;
	}

	/**
	 * Retrieves a list of service instances from Consul ordered by network distance (nearest to farthest) in the
	 * local datacenter.
	 *
	 * @param serviceName The name of the service to list.
	 * @return A List of service instances located in the local datacenter.
	 */
	public List<ServiceInstance> listInstances(String serviceName) {
		return listInstances(serviceName, null);
	}

	/**
	 * Retrieves a list of service instances from Consul ordered by network distance (nearest to farthest) in the
	 * specified datacenter.
	 *
	 * @param serviceName The name of the service to list.
	 * @param datacenter  The name of the datacenter to search.
	 * @return A List of service instances located in the specified datacenter.
	 */
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

			String body = EntityUtils.toString(response.getEntity());
			throw new ConsultantException("Could not locate service: " + serviceName,
					new ConsulException(statusCode, body));
		}
		catch (IOException | RuntimeException e) {
			throw new ConsultantException(e);
		}
	}

	/**
	 * @return A list of datacenters as registered in Consul.
	 */
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
			String body = EntityUtils.toString(response.getEntity());
			throw new ConsultantException("Could not locate datacenters", new ConsulException(statusCode, body));
		}
		catch (IOException | RuntimeException e) {
			throw new ConsultantException(e);
		}
	}

}
