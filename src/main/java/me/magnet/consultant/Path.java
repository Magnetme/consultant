package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

class Path {

	private final String prefix;
	private final ServiceIdentifier id;
	private final String key;

	Path(String prefix, ServiceIdentifier id, String key) {
		checkNotNull(id, "You must specify an 'id'!");

		this.prefix = prefix;
		this.id = id;
		this.key = key;
	}

	public String getPrefix() {
		return prefix;
	}

	public ServiceIdentifier getId() {
		return id;
	}

	public String getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(prefix)
				.append(id)
				.append(key)
				.toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Path) {
			Path path = (Path) other;
			return new EqualsBuilder()
					.append(prefix, path.prefix)
					.append(id, path.id)
					.append(key, path.key)
					.isEquals();
		}
		return false;
	}

	@Override
	public String toString() {
		List<String> descriptors = Lists.newArrayList();
		id.getDatacenter().ifPresent(dc -> descriptors.add("dc=" + dc));
		id.getHostName().ifPresent(host -> descriptors.add("host=" + host));
		id.getInstance().ifPresent(instance -> descriptors.add("instance=" + instance));

		StringBuilder builder = new StringBuilder();
		if (!isNullOrEmpty(prefix)) {
			builder.append(prefix).append("/");
		}
		builder.append(id.getServiceName());
		if (!descriptors.isEmpty()) {
			builder.append("/[")
					.append(descriptors.stream().collect(Collectors.joining(",")))
					.append("]");
		}
		if (!isNullOrEmpty(key)) {
			builder.append("/").append(key);
		}
		return builder.toString();
	}

}
