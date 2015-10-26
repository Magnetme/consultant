package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;

public class PropertiesUtil {

	/**
	 * Copies all the properties from <code>source</code> to <code>target</code>. If a property exists in both
	 * Properties objects, then it is overwritten. If a property exists in <code>target</code>, but not in
	 * <code>source</code>, it is removed from the <code>target</code> Properties object.
	 *
	 * @param source The source Properties object.
	 * @param target The target Properties object.
	 *
	 * @return A Map of changes. The key of the Map is the setting name, whereas the value is a Pair object with the
	 * old and new value of the setting.
	 */
	public static Map<String, Pair<String, String>> sync(Properties source, Properties target) {
		checkNotNull(source, "You must specify a 'source' Properties object!");
		checkNotNull(target, "You must specify a 'source' Properties object!");

		Set<String> sourceKeys = source.stringPropertyNames();
		Set<String> targetKeys = target.stringPropertyNames();

		Set<String> added = Sets.newHashSet(Sets.difference(sourceKeys, targetKeys));
		Set<String> modified = Sets.newHashSet(Sets.intersection(sourceKeys, targetKeys));
		Set<String> removed = Sets.newHashSet(Sets.difference(targetKeys, sourceKeys));

		Map<String, Pair<String, String>> changes = Maps.newHashMap();

		added.forEach(key -> {
			String newValue = source.getProperty(key);
			changes.put(key, Pair.of(null, newValue));
			target.setProperty(key, newValue);
		});

		modified.forEach(key -> {
			String oldValue = target.getProperty(key);
			String newValue = source.getProperty(key);
			changes.put(key, Pair.of(oldValue, newValue));
			target.setProperty(key, newValue);
		});

		removed.forEach(key -> {
			String oldValue = target.getProperty(key);
			changes.put(key, Pair.of(oldValue, null));
			target.remove(key);
		});

		return changes;
	}

	private PropertiesUtil() {
		// Prevent instantiation.
	}

}
