package me.magnet.consultant;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.stream.Collectors;

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

	public static StringEntity toJson(Map<String, String> entries) throws UnsupportedEncodingException {
		Encoder encoder = Base64.getEncoder();
		return new StringEntity("[" + entries.entrySet().stream()
				.map(entry -> "{\"Key\":\"" + entry.getKey() + "\",\"Value\":\"" + encoder.encodeToString(
						entry.getValue().getBytes()) + "\"}")
				.collect(Collectors.joining(",")) + "]");
	}

}
