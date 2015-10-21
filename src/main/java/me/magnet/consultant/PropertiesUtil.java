package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Properties;
import java.util.Set;

import com.google.common.collect.Sets;

public class PropertiesUtil {

	/**
	 * Copies all the properties from <code>source</code> to <code>target</code>. If a property exists in both
	 * Properties objects, then it is overwritten. If a property exists in <code>target</code>, but not in
	 * <code>source</code>, it is removed from the <code>target</code> Properties object.
	 *
	 * @param source The source Properties object.
	 * @param target The target Properties object.
	 */
	public static void sync(Properties source, Properties target) {
		checkNotNull(source, "You must specify a 'source' Properties object!");
		checkNotNull(target, "You must specify a 'source' Properties object!");

		Set<String> sourceKeys = source.stringPropertyNames();
		Set<String> targetKeys = target.stringPropertyNames();

		Set<String> added = Sets.newHashSet(Sets.difference(sourceKeys, targetKeys));
		Set<String> modified = Sets.newHashSet(Sets.intersection(sourceKeys, targetKeys));
		Set<String> removed = Sets.newHashSet(Sets.difference(targetKeys, sourceKeys));

		added.forEach(key -> target.setProperty(key, source.getProperty(key)));
		modified.forEach(key -> target.setProperty(key, source.getProperty(key)));
		removed.forEach(target::remove);
	}

	private PropertiesUtil() {
		// Prevent instantiation.
	}

}
