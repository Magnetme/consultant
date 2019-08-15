package me.magnet.consultant;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceInstance {

	@JsonProperty("Node")
	private final Node node;

	@JsonProperty("Service")
	private final Service service;

	@JsonProperty("Checks")
	private final List<CheckStatus> checks;

	private ServiceInstance() {
		this.node = null;
		this.service = null;
		this.checks = null;
	}

	ServiceInstance(Node node, Service service, List<CheckStatus> checks) {
		this.node = node;
		this.service = service;
		this.checks = checks;
	}

	public Node getNode() {
		return node;
	}

	public Service getService() {
		return service;
	}

	public List<CheckStatus> getChecks() {
		return ImmutableList.copyOf(checks);
	}

	@Override
	public String toString() {
		return "[" + service.getService() + " - " + service.getId() + " @ " + node.getNode()  + "]";
	}

}
