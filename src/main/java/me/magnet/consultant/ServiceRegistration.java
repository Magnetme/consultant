package me.magnet.consultant;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ServiceRegistration {

	@JsonProperty("ID")
	private final String id;

	@JsonProperty("Name")
	private final String name;

	@JsonProperty("Tags")
	private final String[] tags;

	@JsonProperty("Address")
	private final String address;

	@JsonProperty("Port")
	private final int port;

	@JsonProperty("Check")
	private final Check check;

	ServiceRegistration(String id, String name, String address, int port, Check check, Set<String> tags) {
		this.id = id;
		this.name = name;
		this.address = address;
		this.port = port;
		this.tags = tags.toArray(new String[tags.size()]);
		this.check = check;
	}

}
