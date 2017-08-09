package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Consultant class allows you to retrieve the configuration for your application from Consul, and at the same
 * time subscribe to changes to that configuration.
 */
public class Consultant {

	private static final int HEALTH_CHECK_INTERVAL = 10;
	private static final int TERMINATION_TIMEOUT_SECONDS = 5;
	private ConfigUpdater poller;

	/**
	 * Allows you to build a custom Consultant object.
	 */
	public static class Builder {

		private static final int CONSUL_DEFAULT_PORT = 8500;
		private static final String CONSUL_ADDRESS = "http://localhost" + ":" + CONSUL_DEFAULT_PORT;

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Agent {

			@JsonProperty("Config")
			private Config config;

			public Config getConfig() {
				return config;
			}

			void setConfig(Config config) {
				this.config = config;
			}

		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class Config {

			@JsonProperty("Datacenter")
			private String datacenter;

			@JsonProperty("NodeName")
			private String nodeName;

			public String getDatacenter() {
				return datacenter;
			}

			public String getNodeName() {
				return nodeName;
			}

			void setDatacenter(String datacenter) {
				this.datacenter = datacenter;
			}

			void setNodeName(String nodeName) {
				this.nodeName = nodeName;
			}

		}

		private ScheduledExecutorService executor;
		private ObjectMapper mapper;
		private CloseableHttpClient http;

		private ConfigValidator validator;
		private final SetMultimap<String, SettingListener> settingListeners;
		private final Set<ConfigListener> configListeners;

		private String host;
		private Properties properties;
		private boolean pullConfig;

		private String serviceName;
		private String kvPrefix;
		private String datacenter;
		private String hostname;
		private String instanceName;
		private String healthEndpoint;

		private URI consulURI;


		private Builder() {
			this.settingListeners = HashMultimap.create();
			this.configListeners = Sets.newHashSet();
			this.properties = new Properties();
			this.pullConfig = true;
			this.healthEndpoint = "/_health";
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
		 * States that Consultant should use a specific ObjectMapper for serialization and deserialization of JSON.
		 *
		 * @param mapper The ObjectMapper to use when deserializing JSON and serializing objects.
		 * @return The Builder instance.
		 */
		public Builder usingObjectMapper(ObjectMapper mapper) {
			this.mapper = mapper;
			return this;
		}

		/**
		 * Specifies where the Consul REST API can be reached on. Alternatively you can specify this through the
		 * environment variable <code>CONSUL_HOST=http://localhost</code>. The default port (8500) is used.
		 *
		 * @param host The host where the Consul REST API can be found.
		 * @return The Builder instance.
		 */
		public Builder withConsulHost(String host) {
			this.host = host;
			return this;
		}

        /**
         * Allows the caller to specify the prefix when looking up the key value properties. This will default to
         * {@code config} if not specified.
         *
         * @param kvPrefix the path prefix to be used when looking-up the properties.
         * @return The Builder instance.
         */
		public Builder withKvPrefix(String kvPrefix) {
            this.kvPrefix = kvPrefix;
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
			return identifyAs(serviceName, null, null, null);
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you should define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code> and
		 * <code>SERVICE_HOST</code>. If the datacenter is not defined using environment variables either, this
		 * value will default to the corresponding value of the Consul agent.
		 *
		 * @param serviceName The name of this service.
		 * @param datacenter The name of the datacenter where this service is running in.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName, String datacenter) {
			return identifyAs(serviceName, datacenter, null, null);
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you should define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code> and
		 * <code>SERVICE_HOST</code>. If the datacenter and hostname are not defined using environment variables
		 * either, these values will default to the corresponding values of the Consul agent.
		 *
		 * @param serviceName The name of this service.
		 * @param datacenter The name of the datacenter where this service is running in.
		 * @param hostname The name of the host where this service is running on.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName, String datacenter, String hostname) {
			return identifyAs(serviceName, datacenter, hostname, null);
		}

		/**
		 * States the identify of this application. This is used to figure out what configuration settings apply
		 * to this application. If you don't set the identity using this method, you should define it using
		 * environment variables such as <code>SERVICE_NAME</code>, and optionally <code>SERVICE_DC</code>,
		 * <code>SERVICE_HOST</code>, and <code>SERVICE_INSTANCE</code>. If the datacenter and hostname are not
		 * defined using environment variables either, these values will default to the corresponding values of
		 * the Consul agent.
		 *
		 * @param serviceName The name of this service.
		 * @param datacenter The name of the datacenter where this service is running in.
		 * @param hostname The name of the host where this service is running on.
		 * @param instanceName The name/role of this service instance.
		 * @return The Builder instance.
		 */
		public Builder identifyAs(String serviceName, String datacenter, String hostname, String instanceName) {
			checkArgument(!isNullOrEmpty(serviceName), "You must specify a 'serviceName'!");
			this.serviceName = serviceName;
			this.datacenter = datacenter;
			this.hostname = hostname;
			this.instanceName = instanceName;
			return this;
		}

		/**
		 * States the endpoint used for checking the service's health.
		 *
		 * @param endpoint The endpoint to use for health checking. Defaults to "/_health".
		 * @return The Builder instance.
		 */
		public Builder setHealthEndpoint(String endpoint) {
			this.healthEndpoint = endpoint;
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
		 * Specifies a callback listener which is notified of whenever a new and valid configuration is detected
		 * in Consul.
		 *
		 * @param listener The listener to call when a new valid configuration is detected.
		 * @return The Builder instance.
		 */
		public Builder onValidConfig(ConfigListener listener) {
			this.configListeners.add(listener);
			return this;
		}

		/**
		 * Specifies a callback listener which is notified of whenever the specified setting is updated.
		 *
		 * @param listener The listener to call when the specified setting is updated.
		 * @return The Builder instance.
		 */
		public Builder onSettingUpdate(String key, SettingListener listener) {
			this.settingListeners.put(key, listener);
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
		 * Specifies that Consultant should or should not fetch configuration from Consul. By default this is set to
		 * true, but it can be useful to set this to false for testing.
		 *
		 * @param pullConfig True if configuration should be retrieved from Consul, or false if it should not.
		 * @return The Builder instance.
		 */
		public Builder pullConfigFromConsul(boolean pullConfig) {
			this.pullConfig = pullConfig;
			return this;
		}

		/**
		 * Ensures that Consultant starts out with a default Properties object. This object will be updated if
		 * Consultant pulls configuration from Consul. By default Consultant will start out with an empty Properties
		 * object.
		 *
		 * @param properties The Properties object to start Consultant with.
		 * @return The Builder instance.
		 */
		public Builder startWith(Properties properties) {
			checkArgument(properties != null, "You must specify a non-null Properties object!");
			this.properties = properties;
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
			}

			String consulHost = host;
			if (isNullOrEmpty(consulHost)) {
				consulHost = CONSUL_ADDRESS;
			}

			// Use regex here because URI cannot properly parse "localhost:8500".
			boolean changedConsulSchema = false;
			if (!consulHost.matches("[a-zA-Z0-9\\+\\-\\.]+://.*")) {
				consulHost = "http://" + consulHost;
				changedConsulSchema = true;
			}

			try {
				consulURI = new URI(consulHost);

				// Default consul URI to port 8500 if no port has been set, and no scheme was defined by the user.
				if (consulURI.getPort() == -1 && changedConsulSchema) {
					consulURI = new URI(consulURI.getScheme(), consulURI.getUserInfo(), consulURI.getHost(),
							CONSUL_DEFAULT_PORT, consulURI.getPath(), consulURI.getQuery(), consulURI.getFragment());
				}
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException("The specified CONSUL_HOST is not a valid URI: " + host, e);
			}

			serviceName = Optional.ofNullable(serviceName).orElse(fromEnvironment("SERVICE_NAME"));
			datacenter = Optional.ofNullable(datacenter).orElse(fromEnvironment("SERVICE_DC"));
			hostname = Optional.ofNullable(hostname).orElse(fromEnvironment("SERVICE_HOST"));
			instanceName = Optional.ofNullable(instanceName)
					.orElse(Optional.ofNullable(fromEnvironment("SERVICE_INSTANCE"))
							.orElse(UUID.randomUUID().toString()));

			if (mapper == null) {
				mapper = new ObjectMapper();
			}

			if (executor == null) {
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

			try (CloseableHttpResponse response = http.execute(new HttpGet(consulURI + "/v1/agent/self"))) {
				HttpEntity entity = response.getEntity();
				Agent agent = mapper.readValue(entity.getContent(), Agent.class);
				Config config = agent.getConfig();

				if (isNullOrEmpty(datacenter)) {
					datacenter = config.getDatacenter();
				}
				if (isNullOrEmpty(hostname)) {
					hostname = config.getNodeName();
				}
			}
			catch (IOException e) {
				throw new RuntimeException("Could not fetch agent details from Consul.", e);
			}

			ServiceIdentifier id = new ServiceIdentifier(serviceName, datacenter, hostname, instanceName);
			Consultant consultant = new Consultant(executor, mapper, consulURI, id, settingListeners, configListeners,
                    validator, http, pullConfig, healthEndpoint, kvPrefix);

			consultant.init(properties);
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

	private final AtomicBoolean registered;
	private final CloseableHttpClient http;
	private final ScheduledExecutorService executor;
	private final URI consulUri;
	private final ServiceIdentifier id;
	private final ObjectMapper mapper;
	private final ConfigValidator validator;
	private final Properties validated;
	private final boolean pullConfig;
	private final String healthEndpoint;
	private final String kvPrefix;

	private final ServiceInstanceBackend serviceInstanceBackend;
	private final Multimap<String, SettingListener> settingListeners;
	private final Set<ConfigListener> configListeners;
	private final AtomicBoolean shutdownBegun = new AtomicBoolean(false);

	private Consultant(ScheduledExecutorService executor, ObjectMapper mapper, URI consulUri,
			ServiceIdentifier identifier, SetMultimap<String, SettingListener> settingListeners,
			Set<ConfigListener> configListeners, ConfigValidator validator, CloseableHttpClient http,
			boolean pullConfig, String healthEndpoint, String kvPrefix) {

		this.registered = new AtomicBoolean();
		this.settingListeners = Multimaps.synchronizedSetMultimap(settingListeners);
		this.configListeners = Sets.newConcurrentHashSet(configListeners);
		this.serviceInstanceBackend = new ServiceInstanceBackend(identifier.getDatacenter(), consulUri, mapper, http);
		this.mapper = mapper;
		this.validator = validator;
		this.executor = executor;
		this.consulUri = consulUri;
		this.id = identifier;
		this.pullConfig = pullConfig;
		this.validated = new Properties();
		this.healthEndpoint = healthEndpoint;
		this.http = http;
        this.kvPrefix = kvPrefix;
	}

	private void init(Properties initProperties) {
		updateValidatedConfig(initProperties);
		if (!pullConfig) {
			return;
		}

		log.info("Fetching initial configuration from Consul for serviceID: {}", id);
		poller = new ConfigUpdater(executor, http, consulUri, null, id, mapper, null, (properties) -> {
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
		}, kvPrefix);

		try {
			executor.submit(poller).get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			throw new ConsultantException(e.getCause());
		}
	}

	public void registerService(int port) {
		if (!registered.compareAndSet(false, true)) {
			log.warn("Cannot register the service, as service was already registered!");
			return;
		}

		String url = consulUri + "/v1/agent/service/register";
		log.info("Registering service with Consul: {}", id);

		try {
			String serviceId = id.getInstance().get();
			String serviceName = id.getServiceName();
			String serviceHost = id.getHostName().get();
			Check check = new Check("http://" + serviceHost + ":" + port + healthEndpoint, HEALTH_CHECK_INTERVAL);
			ServiceRegistration registration = new ServiceRegistration(serviceId, serviceName, serviceHost, port, check);
			String serialized = mapper.writeValueAsString(registration);

			HttpPut request = new HttpPut(url);
			request.setEntity(new StringEntity(serialized));
			request.setHeader("User-Agent", "Consultant");
			try (CloseableHttpResponse response = http.execute(request)) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode >= 200 && statusCode < 400) {
					return;
				}
				log.error("Could not register service, status: " + statusCode);
				String body = EntityUtils.toString(response.getEntity());
				throw new ConsultantException("Could not register service", new ConsulException(statusCode, body));
			}
		}
		catch (IOException | RuntimeException e) {
			registered.set(false);
			log.error("Could not register service!", e);
			throw new ConsultantException(e);
		}
	}

	public void deregisterService() {
		if (!registered.compareAndSet(true, false)) {
			log.warn("Cannot deregister the service, as service wasn't registered or was already deregistered!");
			return;
		}

		String serviceId = id.getInstance().get();
		String url = consulUri + "/v1/agent/service/deregister/" + serviceId;
		log.info("Deregistering service from Consul: {}", id);

		HttpDelete request = new HttpDelete(url);
		request.setHeader("User-Agent", "Consultant");
		try (CloseableHttpResponse response = http.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode >= 200 && statusCode < 400) {
				return;
			}
			log.error("Could not deregister service, status: " + statusCode);
			String body = EntityUtils.toString(response.getEntity());
			throw new ConsultantException("Could not deregister service", new ConsulException(statusCode, body));
		}
		catch (IOException | RuntimeException e) {
			registered.set(true);
			log.error("Could not deregister service!", e);
			throw new ConsultantException(e);
		}
	}

	/**
	 * Lists all service instances of a particular service known to Consul ordered by network distance. This method is
	 * scheduled for removal in the next major release. Avoid this method if possible due to possible performance
	 * issues
	 * when having a multi datacenter Consul cluster. Use the locateAll() method instead.
	 */
	@Deprecated
	public List<ServiceInstance> list(String serviceName) {
		Optional<ServiceInstance> instance;
		List<ServiceInstance> instances = Lists.newArrayList();
		ServiceLocator locator = locateAll(serviceName, RoutingStrategies.NETWORK_DISTANCE);
		while ((instance = locator.next()).isPresent()) {
			instances.add(instance.get());
		}
		return instances;
	}

	/**
	 * Returns a ServiceLocator with which services matching the specified service name can be found. By default uses
	 * the "Randomized Weighted Distance" RoutingStrategy.
	 *
	 * @param serviceName The name of the service to locate instances of.
	 *
	 * @return The constructed ServiceLocator.
	 */
	public ServiceLocator locateAll(String serviceName) {
		return locateAll(serviceName, RoutingStrategies.RANDOMIZED_WEIGHTED_DISTANCE);
	}

	/**
	 * Returns a ServiceLocator with which services matching the specified service name can be found.
	 *
	 * @param serviceName     The name of the service to locate instances of.
	 * @param routingStrategy The RoutingStrategy to use to locate instances.
	 *
	 * @return The constructed ServiceLocator.
	 */
	public ServiceLocator locateAll(String serviceName, RoutingStrategy routingStrategy) {
		return routingStrategy.locateInstances(serviceInstanceBackend, serviceName);
	}

	/**
	 * Returns the closest service instance's InetSocketAddress.
	 *
	 * @param serviceName The name of the service to locate.
	 *
	 * @return An Optional containing the closest service instance's InetSocketAddress, or an empty Optional otherwise.
	 */
	@Deprecated
	public Optional<InetSocketAddress> locate(String serviceName) {
		return locateAll(serviceName, RoutingStrategies.NETWORK_DISTANCE).next()
				.map(instance -> {
					Node node = instance.getNode();
					Service service = instance.getService();
					String address = Optional.ofNullable(node.getAddress())
							.orElse(service.getAddress());

					return new InetSocketAddress(address, service.getPort());
				});
	}

	public void addConfigListener(ConfigListener listener) {
		configListeners.add(listener);
	}

	public boolean removeConfigListener(ConfigListener listener) {
		return configListeners.remove(listener);
	}

	public void addSettingListener(String key, SettingListener listener) {
		settingListeners.put(key, listener);
	}

	public boolean removeSettingListener(String key, SettingListener listener) {
		return settingListeners.remove(key, listener);
	}

	private void updateValidatedConfig(Properties newConfig) {
		Map<String, Pair<String, String>> changes = PropertiesUtil.sync(newConfig, validated);
		if (changes.isEmpty()) {
			return;
		}

		for (ConfigListener listener : configListeners) {
			listener.onConfigUpdate(validated);
		}

		for (Entry<String, Pair<String, String>> entry : changes.entrySet()) {
			String key = entry.getKey();
			Collection<SettingListener> listeners = settingListeners.get(key);
			if (listeners == null || listeners.isEmpty()) {
				continue;
			}

			for (SettingListener listener : listeners) {
				Pair<String, String> change = entry.getValue();
				listener.onSettingUpdate(key, change.getLeft(), change.getRight());
			}
		}
	}

	/**
	 * Tears any outstanding resources down.
	 * @throws InterruptedException If it got interrupted while waiting for any open tasks
	 */
	public void shutdown() throws InterruptedException {
		if (registered.get()) {
			try {
				deregisterService();
			}
			catch (ConsultantException e) {
				log.error("Error occurred while deregistering", e);
			}
		}

		executor.shutdownNow();
		shutdownBegun.set(true);
		if (poller != null) {
			poller.shutdown();
		}
		try {
			/*
			HTTP client does not have a way to interrupt long-running HTTP calls, so we have to shutdown the whole
			thing.
			 */
			http.close();
		}
		catch (IOException | RuntimeException e) {
			log.error("Error occurred on shutdown: " + e.getMessage(), e);
		}

		if (pullConfig && !executor.isShutdown()) {
			boolean allTasksTerminated = executor.awaitTermination(TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!allTasksTerminated) {
				log.warn("Could not shut down all executor tasks!");
			}
		}
	}

	/**
	 * @return The host on which the Consul agent can be found. Will typically return "http://localhost:8500" unless
	 * otherwise specified in the Builder or environment variables.
	 */
	public String getConsulHost() {
		return consulUri.toString();
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
