package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckStatus {

	@JsonProperty("Name")
	private final String name;

	@JsonProperty("Output")
	private final String output;

	@JsonProperty("Status")
	private final String status;

	CheckStatus() {
		this.name = null;
		this.output = null;
		this.status = null;
	}

	CheckStatus(String name, String output, String status) {
		this.name = name;
		this.output = output;
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public String getOutput() {
		return output;
	}

	public String getStatus() {
		return status;
	}

}
