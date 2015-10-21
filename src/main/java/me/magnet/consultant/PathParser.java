package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PathParser {

	private static final Pattern DC_FIELD = Pattern.compile("^dc\\s*=\\s*(?<dc>.*)$");
	private static final Pattern HOST_FIELD = Pattern.compile("^host\\s*=\\s*(?<host>.*)$");
	private static final Pattern INSTANCE_FIELD = Pattern.compile("^instance\\s*=\\s*(?<instance>.*)$");

	static Path parse(String prefix, String path) {
		checkArgument(!isNullOrEmpty(path), "You must specify an 'path'!");

		String tail = path;
		if (prefix != null) {
			if (!path.startsWith(prefix + "/")) {
				return null;
			}
			tail = path.substring(prefix.length() + 1);
		}

		String serviceName;
		String datacenter = null;
		String hostName = null;
		String serviceInstance = null;

		if (tail.contains("/[")) {
			int index = tail.indexOf("/[");
			serviceName = tail.substring(0, index);

			String part = tail.substring(index + 2);
			part = part.substring(0, part.indexOf(']'));
			String[] splits = part.split(",");
			for (String split : splits) {
				if (datacenter == null) {
					Matcher matcher = DC_FIELD.matcher(split);
					if (matcher.find()) {
						datacenter = matcher.group("dc");
						continue;
					}
				}
				if (hostName == null) {
					Matcher matcher = HOST_FIELD.matcher(split);
					if (matcher.find()) {
						hostName = matcher.group("host");
						continue;
					}
				}
				if (serviceInstance == null) {
					Matcher matcher = INSTANCE_FIELD.matcher(split);
					if (matcher.find()) {
						serviceInstance = matcher.group("instance");
					}
				}
			}
			tail = tail.substring(tail.indexOf("]") + 1);
			if (tail.startsWith(".")) {
				tail = tail.substring(1);
			}
		}
		else if (tail.contains("/")) {
			serviceName = tail.substring(0, tail.indexOf("/"));
			tail = tail.substring(tail.indexOf("/") + 1);
		}
		else {
			serviceName = tail;
			tail = "";
		}

		ServiceIdentifier id = new ServiceIdentifier(serviceName, datacenter, hostName, serviceInstance);
		return new Path(prefix, id, emptyToNull(tail));
	}

	private PathParser() {
		// Prevent instantiation.
	}

}
