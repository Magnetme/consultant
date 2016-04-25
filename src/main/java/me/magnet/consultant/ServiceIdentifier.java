package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ServiceIdentifier {

	private final String serviceName;
	private final Optional<String> datacenter;
	private final Optional<String> hostName;
	private final Optional<String> instance;
	private final Set<String> tags;

	ServiceIdentifier(String serviceName, String datacenter, String hostName, String instance) {
		this(serviceName, datacenter, hostName, instance, Sets.newHashSet());
	}

	ServiceIdentifier(String serviceName, String datacenter, String hostName, String instance, Set<String> tags) {
		checkArgument(!isNullOrEmpty(serviceName), "You must specify a 'serviceName'!");
		checkArgument(datacenter == null || !datacenter.isEmpty(), "You cannot specify 'datacenter' as empty String!");
		checkArgument(hostName == null || !hostName.isEmpty(), "You cannot specify 'hostName' as empty String!");
		checkArgument(instance == null || !instance.isEmpty(), "You cannot specify 'instance' as empty String!");
		checkArgument(tags != null, "You cannot specify 'tags' as a null Set!");

		this.datacenter = Optional.ofNullable(datacenter);
		this.hostName = Optional.ofNullable(hostName);
		this.serviceName = serviceName;
		this.instance = Optional.ofNullable(instance);
		this.tags = tags;
	}

	public String getServiceName() {
		return serviceName;
	}

	public Optional<String> getDatacenter() {
		return datacenter;
	}

	public Optional<String> getHostName() {
		return hostName;
	}

	public Optional<String> getInstance() {
		return instance;
	}

	public Set<String> getTags() {
		return tags;
	}

	public boolean appliesTo(ServiceIdentifier serviceIdentifier) {
		checkNotNull(serviceIdentifier, "You must specify a 'serviceIdentifier'!");

		if (!getServiceName().equals(serviceIdentifier.getServiceName())) {
			return false;
		}
		else if (!matches(getDatacenter(), serviceIdentifier.getDatacenter())) {
			return false;
		}
		else if (!matches(getHostName(), serviceIdentifier.getHostName())) {
			return false;
		}
		else if (!matches(getInstance(), serviceIdentifier.getInstance())) {
			return false;
		}
		return true;
	}

	private <T> boolean matches(Optional<T> left, Optional<T> right) {
		if (left.isPresent() && right.isPresent()) {
			// Both values are set, they must match.
			return left.get().equals(right.get());
		}
		else if (left.isPresent()) {
			// Only the matching value is set, accept nothing.
			return false;
		}
		// Either the matching value is not set so accept everything.
		return true;
	}

	public boolean moreSpecificThan(ServiceIdentifier other) {
		checkNotNull(other, "You must specify a 'other'!");

		if (!other.getInstance().isPresent() && getInstance().isPresent()) {
			return true;
		}
		else if (!other.getHostName().isPresent() && getHostName().isPresent()) {
			return true;
		}
		else if (!other.getDatacenter().isPresent() && getDatacenter().isPresent()) {
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ServiceIdentifier) {
			ServiceIdentifier id = (ServiceIdentifier) other;
			return new EqualsBuilder()
					.append(serviceName, id.serviceName)
					.append(datacenter, id.datacenter)
					.append(hostName, id.hostName)
					.append(instance, id.instance)
					.append(tags, id.tags)
					.isEquals();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append(serviceName)
				.append(datacenter)
				.append(hostName)
				.append(instance)
				.append(tags)
				.toHashCode();
	}

	@Override
	public String toString() {
		List<String> descriptors = Lists.newArrayList();
		getDatacenter().ifPresent(dc -> descriptors.add("dc=" + dc));
		getHostName().ifPresent(host -> descriptors.add("host=" + host));
		getInstance().ifPresent(instance -> descriptors.add("instance=" + instance));

		StringBuilder builder = new StringBuilder(serviceName);
		if (!descriptors.isEmpty()) {
			builder.append("[");
			builder.append(descriptors.stream().collect(Collectors.joining(",")));
			builder.append("]");
		}
		return builder.toString();
	}
}
