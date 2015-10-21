package me.magnet.consultant;

import java.util.Properties;

/**
 * This interface allows you to handle updates to the Properties object containing your service's configuration.
 */
@FunctionalInterface
public interface ConfigListener {

	/**
	 * This method is fired when the Properties object containing your service's configuration is modified.
	 *
	 * @param properties The updated Properties object.
	 */
	void onConfigUpdate(Properties properties);

}
