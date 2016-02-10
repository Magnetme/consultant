package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Service {

	@JsonProperty("Id")
	private String id;

	@JsonProperty("Service")
	private String service;

	@JsonProperty("Tags")
	private String[] tags;

	@JsonProperty("Address")
	private String address;

	@JsonProperty("Port")
	private Integer port;

	public String getId() {
		return id;
	}

	public String getService() {
		return service;
	}

	public String[] getTags() {
		return tags;
	}

	public String getAddress() {
		return address;
	}

	public Integer getPort() {
		return port;
	}

}
