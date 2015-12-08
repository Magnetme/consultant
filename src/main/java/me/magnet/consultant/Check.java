package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Check {

	@JsonProperty("HTTP")
	private final String http;

	@JsonProperty("Interval")
	private final Integer interval;

	Check(String http, Integer interval) {
		this.http = http;
		this.interval = interval;
	}

}
