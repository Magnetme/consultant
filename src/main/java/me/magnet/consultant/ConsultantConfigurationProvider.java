package me.magnet.consultant;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.google.common.collect.ImmutableMap;
import com.netflix.governator.configuration.AbstractObjectConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.ConfigurationOwnershipPolicy;
import com.netflix.governator.configuration.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ConsultantConfigurationProvider class can be used when using Netflix's Governator library to bootstrap your
 * application on service.
 */
public class ConsultantConfigurationProvider extends AbstractObjectConfigurationProvider {

	private static final Logger log = LoggerFactory.getLogger(ConsultantConfigurationProvider.class);

	private final Consultant consultant;
	private final DateFormat dateFormat;
	private final ConfigurationOwnershipPolicy ownershipPolicy;
	private final ImmutableMap<String, String> variables;


	/**
	 * Creates a new ConsultantConfigurationProvider.
	 *
	 * @param consultant The Consultant to use to retrieve the configuration from Consul, and subscribe to updates.
	 */
	public ConsultantConfigurationProvider(Consultant consultant) {
		this(consultant, DateFormat.getInstance(), defaultOwnership(consultant));
	}

	private static ConfigurationOwnershipPolicy defaultOwnership(Consultant consultant) {
		return (key, variables) -> consultant.getProperties().containsKey(key.getKey(variables));
	}

	/**
	 * Creates a new ConsultantConfigurationProvider.
	 *
	 * @param consultant The Consultant to use to retrieve the configuration from Consul, and subscribe to updates.
	 * @param dateFormat A custom DateFormat to use to parse Date's stored in Consul's config.
	 */
	public ConsultantConfigurationProvider(Consultant consultant, DateFormat dateFormat) {
		this(consultant, dateFormat, defaultOwnership(consultant));
	}

	/**
	 * Creates a new ConsultantConfigurationProvider.
	 *
	 * @param consultant      The Consultant to use to retrieve the configuration from Consul, and subscribe to
	 *                        updates.
	 * @param ownershipPolicy The policy for which this consultant instance holds keys
	 */
	public ConsultantConfigurationProvider(Consultant consultant, ConfigurationOwnershipPolicy ownershipPolicy) {
		this(consultant, DateFormat.getInstance(), ownershipPolicy);
	}

	/**
	 * Creates a new ConsultantConfigurationProvider.
	 *
	 * @param consultant      The Consultant to use to retrieve the configuration from Consul, and subscribe to
	 *                        updates.
	 * @param dateFormat      A custom DateFormat to use to parse Date's stored in Consul's config.
	 * @param ownershipPolicy The policy for which this consultant instance holds keys
	 */
	public ConsultantConfigurationProvider(Consultant consultant, DateFormat dateFormat,
			ConfigurationOwnershipPolicy ownershipPolicy) {
		this.consultant = consultant;
		this.dateFormat = dateFormat;
		this.ownershipPolicy = ownershipPolicy;

		ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
		builder.put("serviceName", consultant.getServiceIdentifier().getServiceName());
		consultant.getServiceIdentifier().getHostName().ifPresent(hostname -> builder.put("hostname", hostname));
		consultant.getServiceIdentifier().getInstance().ifPresent(instance -> builder.put("instance", instance));
		consultant.getServiceIdentifier().getDatacenter().ifPresent(dc -> builder.put("dataCenter", dc));
		variables = builder.build();
	}

	/**
	 * @return The DateFormat used to parse Date's in Consul's config. If no custom DateFormat was specified,
	 * the one returned by DateFormat.getInstance() is used instead.
	 */
	public DateFormat getDateFormat() {
		return dateFormat;
	}

	@Override
	public Property<Boolean> getBooleanProperty(ConfigurationKey configurationKey, Boolean defaultValue) {
		return new Property<Boolean>() {
			@Override
			public Boolean get() {
				String rawKey = configurationKey.getRawKey();
				String value = consultant.getProperties().getProperty(rawKey);
				if (isNullOrEmpty(value)) {
					return defaultValue;
				}
				return Boolean.parseBoolean(value);
			}
		};
	}

	@Override
	public Property<Integer> getIntegerProperty(ConfigurationKey configurationKey, Integer defaultValue) {
		return new Property<Integer>() {
			@Override
			public Integer get() {
				String rawKey = configurationKey.getRawKey();
				try {
					String value = consultant.getProperties().getProperty(rawKey);
					if (isNullOrEmpty(value)) {
						return defaultValue;
					}
					return Integer.parseInt(value);
				}
				catch (RuntimeException e) {
					log.error("Failed to parse integer: " + rawKey + ": {}", e.getMessage(), e);
					return defaultValue;
				}
			}
		};
	}

	@Override
	public Property<Long> getLongProperty(ConfigurationKey configurationKey, Long defaultValue) {
		return new Property<Long>() {
			@Override
			public Long get() {
				String rawKey = configurationKey.getRawKey();
				try {
					String value = consultant.getProperties().getProperty(rawKey);
					if (isNullOrEmpty(value)) {
						return defaultValue;
					}
					return Long.parseLong(value);
				}
				catch (RuntimeException e) {
					log.error("Failed to parse long: " + rawKey + ": {}", e.getMessage(), e);
					return defaultValue;
				}
			}
		};
	}

	@Override
	public Property<Double> getDoubleProperty(ConfigurationKey configurationKey, Double defaultValue) {
		return new Property<Double>() {
			@Override
			public Double get() {
				String rawKey = configurationKey.getRawKey();
				try {
					String value = consultant.getProperties().getProperty(rawKey);
					if (isNullOrEmpty(value)) {
						return defaultValue;
					}
					return Double.parseDouble(value);
				}
				catch (RuntimeException e) {
					log.error("Failed to parse double: " + rawKey + ": {}", e.getMessage(), e);
					return defaultValue;
				}
			}
		};
	}

	@Override
	public Property<String> getStringProperty(ConfigurationKey configurationKey, String defaultValue) {
		return new Property<String>() {
			@Override
			public String get() {
				String rawKey = configurationKey.getRawKey();
				String value = consultant.getProperties().getProperty(rawKey);
				if (value == null) {
					return defaultValue;
				}
				return value;
			}
		};
	}

	@Override
	public Property<Date> getDateProperty(ConfigurationKey configurationKey, Date defaultValue) {
		return new Property<Date>() {
			@Override
			public Date get() {
				String rawKey = configurationKey.getRawKey();
				try {
					String value = consultant.getProperties().getProperty(rawKey);
					if (isNullOrEmpty(value)) {
						return defaultValue;
					}
					return dateFormat.parse(value);
				}
				catch (RuntimeException | ParseException e) {
					log.error("Failed to parse date: " + rawKey + ": {}", e.getMessage(), e);
					return defaultValue;
				}
			}
		};
	}

	@Override
	public boolean has(ConfigurationKey key) {
		return ownershipPolicy.has(key, variables);
	}
}
