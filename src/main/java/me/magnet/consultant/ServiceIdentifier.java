package me.magnet.consultant;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ServiceIdentifier {

	private final String serviceName;
	private final Optional<String> datacenter;
	private final Optional<String> hostName;
	private final Optional<String> instance;

	ServiceIdentifier(String serviceName, String datacenter, String hostName, String instance) {
		checkArgument(!isNullOrEmpty(serviceName), "You must specify a 'serviceName'!");
		checkArgument(datacenter == null || !datacenter.isEmpty(), "You cannot specify 'datacenter' as empty String!");
		checkArgument(hostName == null || !hostName.isEmpty(), "You cannot specify 'hostName' as empty String!");
		checkArgument(instance == null || !instance.isEmpty(), "You cannot specify 'instance' as empty String!");

		this.datacenter = Optional.ofNullable(datacenter);
		this.hostName = Optional.ofNullable(hostName);
		this.serviceName = serviceName;
		this.instance = Optional.ofNullable(instance);
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

	public boolean appliesTo(ServiceIdentifier mask) {
		checkNotNull(mask, "You must specify a 'mask'!");

		if (!mask.getServiceName().equals(getServiceName())) {
			return false;
		}
		else if (mask.getDatacenter().isPresent() && (getDatacenter().isPresent() && !mask.getDatacenter().equals(getDatacenter()))) {
			return false;
		}
		else if (mask.getHostName().isPresent() && (getHostName().isPresent() && !mask.getHostName().equals(getHostName()))) {
			return false;
		}
		else if (mask.getInstance().isPresent() && (getInstance().isPresent() && !mask.getInstance().equals(getInstance()))) {
			return false;
		}
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
