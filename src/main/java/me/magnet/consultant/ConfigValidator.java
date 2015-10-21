package me.magnet.consultant;

import java.util.Properties;

/**
 * This interface allows you to validate the new config for your service, before it's actually exposed to your
 * service. If the updated Properties object is deemed invalid it will not be exposed to your service.
 */
@FunctionalInterface
public interface ConfigValidator {

	/**
	 * This method verifies if a new config as specified by the Properties object is valid or not. Throwing a
	 * RuntimeException (or a subclass) indicates that the configuration is invalid. Not throwing a RuntimeException
	 * means that the configuration is deemed valid.
	 *
	 * @param properties The configuration to validate.
	 */
	void validateConfig(Properties properties);

}
