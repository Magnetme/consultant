package me.magnet.consultant;

import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;

public class HttpUtils {

	private HttpUtils() {
		// Prevent instantiation...
	}

	public static StatusLine createStatus(int status, String phrase) {
		return new BasicStatusLine(new ProtocolVersion("http", 1, 1), status, phrase);
	}

	public static StringEntity toJson(Map<String, String> entries) {
		Encoder encoder = Base64.getEncoder();
		try {
			return new StringEntity("[" + entries.entrySet().stream()
					.map(entry -> {
						String value = Optional.ofNullable(entry.getValue())
								.map(entryValue -> "\"" + encoder.encodeToString(entryValue.getBytes()) + "\"")
								.orElse("null");

						return "{\"Key\":\"" + entry.getKey() + "\",\"Value\":" + value + "}";
					})
					.collect(Collectors.joining(",")) + "]");
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static StringEntity toJson(Object value) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return new StringEntity(objectMapper.writeValueAsString(value));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
