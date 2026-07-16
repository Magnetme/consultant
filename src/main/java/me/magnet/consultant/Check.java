package me.magnet.consultant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Check {

	@JsonProperty("HTTP")
	private final String http;

	@JsonProperty("Interval")
	private final String interval;

	Check(String http, String interval) {
		this.http = http;
		this.interval = interval;
	}

}
