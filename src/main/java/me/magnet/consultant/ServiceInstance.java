package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceInstance {

	@JsonProperty("Node")
	private final Node node;

	@JsonProperty("Service")
	private final Service service;

	private ServiceInstance() {
		this.node = null;
		this.service = null;
	}

	ServiceInstance(Node node, Service service) {
		this.node = node;
		this.service = service;
	}

	public Node getNode() {
		return node;
	}

	public Service getService() {
		return service;
	}

}
