package me.magnet.consultant;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class which can be used to retrieve lists of instances or datacenters from Consul over its HTTP API.
 */
public class ServiceInstanceBackend {

	private static class ServiceIdentifierCacheKey {

		private final String datacenter;
		private final String serviceName;

		private ServiceIdentifierCacheKey(String datacenter, String serviceName) {
			this.datacenter = datacenter;
			this.serviceName = serviceName;
		}

		public String getDatacenter() {
			return datacenter;
		}

		public String getServiceName() {
			return serviceName;
		}

		public boolean equals(Object other) {
			if (other instanceof ServiceIdentifierCacheKey) {
				ServiceIdentifierCacheKey otherKey = (ServiceIdentifierCacheKey) other;
				return new EqualsBuilder()
						.append(datacenter, otherKey.datacenter)
						.append(serviceName, otherKey.serviceName)
						.isEquals();
			}
			return false;
		}

		public int hashCode() {
			return new HashCodeBuilder()
					.append(datacenter)
					.append(serviceName)
					.toHashCode();
		}

		public String toString() {
			if (datacenter != null) {
				return datacenter + "-" + serviceName;
			}
			return serviceName;
		}

	}

	private static final Logger log = LoggerFactory.getLogger(ConfigUpdater.class);

	private static final TypeReference<List<ServiceInstance>> TYPES = new TypeReference<List<ServiceInstance>>() {};

	private final Optional<String> datacenter;
	private final LoadingCache<ServiceIdentifierCacheKey, List<ServiceInstance>> serviceInstances;
	private final Cache<ServiceIdentifierCacheKey, List<ServiceInstance>> lastSuccessfulInstances;
	private final Supplier<List<String>> datacenters;


	/**
	 * Constructs a new ServiceInstanceBackend object.
	 *
	 * @param datacenter   The datacenter as it is defined in the ServiceIdentifier.
	 * @param consulUri    The URI where Consul's API can be found.
	 * @param token        An optional token to be used to authenticate requests directed at Consul's API.
	 * @param objectMapper The ObjectMapper which can be used to deserialize JSON.
	 * @param http         The HTTP client to use.
	 * @param cacheLocateCallsForMillis How long the results of locate calls should be cached for.
	 */
	ServiceInstanceBackend(Optional<String> datacenter, URI consulUri, String token, ObjectMapper objectMapper,
			CloseableHttpClient http, long cacheLocateCallsForMillis) {
		this(datacenter, consulUri, token, objectMapper, http, cacheLocateCallsForMillis, Ticker.systemTicker());
	}

	ServiceInstanceBackend(Optional<String> datacenter, URI consulUri, String token, ObjectMapper objectMapper,
			CloseableHttpClient http, long cacheLocateCallsForMillis, Ticker ticker) {

		this.datacenter = datacenter;

		this.lastSuccessfulInstances = CacheBuilder.newBuilder()
				.ticker(ticker)
				.expireAfterWrite(15, TimeUnit.SECONDS)
				.build();
		this.serviceInstances = CacheBuilder.newBuilder()
				.ticker(ticker)
				.expireAfterWrite(cacheLocateCallsForMillis, TimeUnit.MILLISECONDS)
				.build(CacheLoader.from(key -> {
					List<ServiceInstance> instances = fetchInstances(key, consulUri, token, objectMapper, http);
					lastSuccessfulInstances.put(key, instances);
					return instances;
				}));

		this.datacenters = Suppliers.memoizeWithExpiration(
				() -> fetchDatacenters(consulUri, token, objectMapper, http),
				cacheLocateCallsForMillis, TimeUnit.MILLISECONDS);
	}

	private List<String> fetchDatacenters(URI consulUri, String token, ObjectMapper objectMapper,
			CloseableHttpClient http) {
		String url = consulUri + "/v1/catalog/datacenters";

		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", "Consultant");
		if (!Strings.isNullOrEmpty(token)) {
			request.setHeader("X-Consul-Token", token);
		}

		try (CloseableHttpResponse response = http.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 400) {
				InputStream content = response.getEntity().getContent();
				return objectMapper.readValue(content, new TypeReference<List<String>>() {
				});
			}
			String body = EntityUtils.toString(response.getEntity());
			throw new ConsultantException("Could not locate datacenters",
					new ConsulException(statusCode, body));
		}
		catch (IOException | RuntimeException e) {
			throw new ConsultantException(e);
		}
	}

	private List<ServiceInstance> fetchInstances(ServiceIdentifierCacheKey key, URI consulUri, String token,
			ObjectMapper objectMapper, CloseableHttpClient http) {
		String url = consulUri + "/v1/health/service/" + key.getServiceName() + "?near=_agent";
		if (!Strings.isNullOrEmpty(key.getDatacenter())) {
			url += "&dc=" + key.getDatacenter();
		}

		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", "Consultant");
		if (!Strings.isNullOrEmpty(token)) {
			request.setHeader("X-Consul-Token", token);
		}

		try (CloseableHttpResponse response = http.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 400) {
				InputStream content = response.getEntity().getContent();
				List<ServiceInstance> allInstances = objectMapper.readValue(content, TYPES);

				List<ServiceInstance> passingInstances = allInstances.stream()
						.filter(instance -> instance.getChecks().stream()
								.allMatch(checkStatus -> "passing".equals(checkStatus.getStatus())))
						.collect(Collectors.toList());

				/*
				 * If there are known instances matching the specified service name, but they all have at
				 * least one failing health check (making them unavailable), log this so it's obvious to
				 * whoever is debugging such issues.
				 */
				if (passingInstances.isEmpty() && !allInstances.isEmpty()) {
					StringBuilder builder = new StringBuilder();
					builder.append("None of the known instances are passing all of their checks: \n");

					for (ServiceInstance instance : allInstances) {
						String name = instance.getService().getService();
						String nodeName = instance.getNode().getNode();

						builder.append("\tService \"")
								.append(name)
								.append("\" on node \"")
								.append(nodeName)
								.append("\":\n");

						for (CheckStatus checkStatus : instance.getChecks()) {
							builder.append("\t\t- Check \"")
									.append(checkStatus.getName())
									.append("\" has status \"")
									.append(checkStatus.getStatus())
									.append("\" with output: ")
									.append(checkStatus.getOutput())
									.append("\n");
						}
					}

					log.warn(builder.toString());
				}

				return passingInstances;
			}

			String body = EntityUtils.toString(response.getEntity());
			throw new ConsultantException("Could not locate service: " + key.getServiceName(),
					new ConsulException(statusCode, body));
		}
		catch (IOException | RuntimeException e) {
			throw new ConsultantException(e);
		}
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
	 * specified datacenter. If a refresh fails, the last successful result is reused for up to 15 seconds
	 * after it was fetched. Failed refreshes do not extend this window.
	 *
	 * @param serviceName The name of the service to list.
	 * @param datacenter  The name of the datacenter to search.
	 * @return A List of service instances located in the specified datacenter.
	 */
	public List<ServiceInstance> listInstances(String serviceName, String datacenter) {
		ServiceIdentifierCacheKey key = new ServiceIdentifierCacheKey(datacenter, serviceName);
		try {
			return serviceInstances.get(key);
		}
		catch (ExecutionException | UncheckedExecutionException e) {
			// Failed refreshes must neither renew the fallback nor enter the normal cache.
			List<ServiceInstance> lastSuccessful = lastSuccessfulInstances.getIfPresent(key);
			if (lastSuccessful != null) {
				return lastSuccessful;
			}
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			throw new RuntimeException(e.getCause());
		}
	}

	/**
	 * @return A list of datacenters as registered in Consul.
	 */
	public List<String> listDatacenters() {
		return datacenters.get();
	}

}
