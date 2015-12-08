package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {

	@JsonProperty("Node")
	private String node;

	@JsonProperty("Address")
	private String address;

	@JsonProperty("ServiceID")
	private String serviceId;

	@JsonProperty("ServiceName")
	private String serviceName;

	@JsonProperty("ServiceTags")
	private String[] serviceTags;

	@JsonProperty("ServiceAddress")
	private String serviceAddress;

	@JsonProperty("ServicePort")
	private Integer servicePort;

	public String getNode() {
		return node;
	}

	public String getAddress() {
		return address;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public String[] getServiceTags() {
		return serviceTags;
	}

	public String getServiceAddress() {
		return serviceAddress;
	}

	public Integer getServicePort() {
		return servicePort;
	}

}
