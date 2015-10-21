package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KeyValueEntry {

	@JsonProperty("Key")
	private String key;

	@JsonProperty("Value")
	private String value;

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}

}
