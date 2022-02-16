package me.magnet.consultant;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.mockito.Mockito;

public class MockedHttpClientBuilder {

	private final Table<String, String, Function<HttpRequestBase, CloseableHttpResponse>> handlers;

	MockedHttpClientBuilder() {
		this.handlers = HashBasedTable.create();
	}

	public MockedHttpClientBuilder onGet(String path, Function<HttpGet, CloseableHttpResponse> consumer) {
		handlers.put(path, "GET", request -> consumer.apply((HttpGet) request));
		return this;
	}

	public MockedHttpClientBuilder onPost(String path, Function<HttpPost, CloseableHttpResponse> consumer) {
		handlers.put(path, "POST", request -> consumer.apply((HttpPost) request));
		return this;
	}

	public CloseableHttpClient create() throws IOException {
		CloseableHttpClient mock = Mockito.mock(CloseableHttpClient.class);
		when(mock.execute(any())).then(invocationOnMock -> {
			Object[] arguments = invocationOnMock.getArguments();
			HttpRequestBase argument = (HttpRequestBase) arguments[0];

			URI uri = argument.getURI();
			String path = uri.getPath();
			if (uri.getRawQuery() != null) {
				path += "?" + uri.getRawQuery();
			}

			Function<HttpRequestBase, CloseableHttpResponse> handler = handlers.get(path, argument.getMethod());
			if (handler == null) {
				throw new UnsupportedOperationException("No support for type: " + argument.getClass().getSimpleName()
						+ " and path: " + path);
			}

			return handler.apply(argument);
		});

		return mock;
	}

}
