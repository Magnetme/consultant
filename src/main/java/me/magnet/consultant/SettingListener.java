package me.magnet.consultant;

/**
 * This interface allows you to handle updates to a particular setting of your service's configuration.
 */
@FunctionalInterface
public interface SettingListener {

	/**
	 * This method is fired when the a particular setting of your service's configuration is modified.
	 *
	 * @param key The key of the modified setting.
	 * @param oldValue The old value of the setting.
	 * @param newValue The new value of the setting.
	 */
	void onSettingUpdate(String key, String oldValue, String newValue);

}
