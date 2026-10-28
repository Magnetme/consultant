package me.magnet.consultant;

import static me.magnet.consultant.HttpUtils.createStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Ticker;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;

public class ServiceInstanceBackendTest {

	private static class ManualTicker extends Ticker {
		private long nanos;

		@Override
		public long read() {
			return nanos;
		}

		void advanceMillis(long millis) {
			nanos += TimeUnit.MILLISECONDS.toNanos(millis);
		}
	}

	private final ManualTicker ticker = new ManualTicker();
	private final List<HttpGet> requests = new ArrayList<>();
	private CloseableHttpClient http;
	private ServiceInstanceBackend backend;
	private int status = 200;
	private String body = instances("first", "passing");
	private IOException failure;
	private long requestDurationMillis;

	@Before
	public void setUp() throws Exception {
		http = mock(CloseableHttpClient.class);
		when(http.execute(any())).thenAnswer(invocation -> {
			requests.add((HttpGet) invocation.getArguments()[0]);
			ticker.advanceMillis(requestDurationMillis);
			if (failure != null) {
				throw failure;
			}
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getStatusLine()).thenReturn(createStatus(status, "Consul response"));
			when(response.getEntity()).thenReturn(new StringEntity(body));
			return response;
		});
		backend = new ServiceInstanceBackend(Optional.empty(), URI.create("http://localhost:8500"), "token",
				new ObjectMapper(), http, 1_000, ticker);
	}

	@Test
	public void successfulResultsUseTheNormalCacheInterval() {
		List<ServiceInstance> first = backend.listInstances("oauth");
		ticker.advanceMillis(999);
		assertSame(first, backend.listInstances("oauth"));
		assertEquals(1, requests.size());

		body = instances("second", "passing");
		ticker.advanceMillis(1);
		assertEquals("second", backend.listInstances("oauth").get(0).getService().getId());
		assertEquals(2, requests.size());
		assertEquals("http://localhost:8500/v1/health/service/oauth?near=_agent",
				requests.get(0).getURI().toString());
		assertEquals("token", requests.get(0).getFirstHeader("X-Consul-Token").getValue());
		assertEquals("Consultant", requests.get(0).getFirstHeader("User-Agent").getValue());
	}

	@Test
	public void initialHttpFailureIsSurfacedWithConsulDetails() {
		status = 503;
		body = "No cluster leader";
		Throwable cause = rootCause(assertThrows(ConsultantException.class, () -> backend.listInstances("oauth")));
		assertTrue(cause instanceof ConsulException);
		assertEquals(503, ((ConsulException) cause).getStatus());
		assertEquals(body, cause.getMessage());
	}

	@Test
	public void initialTransportFailureIsSurfaced() {
		failure = new IOException("Connection refused");
		assertSame(failure, rootCause(assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"))));
	}

	@Test
	public void failedRefreshesReuseRecentResultsAndRetryEveryCall() {
		List<ServiceInstance> first = backend.listInstances("oauth");
		status = 503;
		ticker.advanceMillis(1_000);
		assertSame(first, backend.listInstances("oauth"));
		assertSame(first, backend.listInstances("oauth"));
		assertEquals(3, requests.size());

		ticker.advanceMillis(13_999);
		assertSame(first, backend.listInstances("oauth"));
		assertEquals(4, requests.size());

		ticker.advanceMillis(1);
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"));
		assertEquals(5, requests.size());
	}

	@Test
	public void transportFailureReusesRecentResults() {
		List<ServiceInstance> first = backend.listInstances("oauth");
		ticker.advanceMillis(1_000);
		failure = new IOException("Connection reset");
		assertSame(first, backend.listInstances("oauth"));
		ticker.advanceMillis(14_001);
		assertSame(failure, rootCause(assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"))));
	}

	@Test
	public void slowFailedRefreshCannotReturnResultsThatExpiredDuringTheRequest() {
		backend.listInstances("oauth");
		ticker.advanceMillis(14_000);
		requestDurationMillis = 1_000;
		status = 503;
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"));
	}

	@Test
	public void recoveryReplacesOldInstancesAndStartsANewFallbackWindow() {
		backend.listInstances("oauth");
		ticker.advanceMillis(1_000);
		status = 503;
		backend.listInstances("oauth");

		ticker.advanceMillis(13_000);
		status = 200;
		body = instances("replacement", "passing");
		List<ServiceInstance> replacement = backend.listInstances("oauth");
		assertEquals("replacement", replacement.get(0).getService().getId());

		status = 503;
		ticker.advanceMillis(14_999);
		assertSame(replacement, backend.listInstances("oauth"));
		ticker.advanceMillis(1);
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"));
	}

	@Test
	public void successfulEmptyResponseReplacesOldInstances() {
		backend.listInstances("oauth");
		ticker.advanceMillis(1_000);
		body = "[]";
		assertTrue(backend.listInstances("oauth").isEmpty());

		ticker.advanceMillis(1_000);
		status = 503;
		assertTrue(backend.listInstances("oauth").isEmpty());
		ticker.advanceMillis(14_000);
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"));
	}

	@Test
	public void successfulUnhealthyResponseReplacesOldInstancesWithEmptyResult() {
		backend.listInstances("oauth");
		ticker.advanceMillis(1_000);
		body = instances("first", "critical");
		assertTrue(backend.listInstances("oauth").isEmpty());

		ticker.advanceMillis(1_000);
		status = 503;
		assertTrue(backend.listInstances("oauth").isEmpty());
	}

	@Test
	public void emptyInitialResultCanAlsoBeUsedAsFallback() {
		body = "[]";
		assertTrue(backend.listInstances("oauth").isEmpty());
		ticker.advanceMillis(1_000);
		failure = new IOException("Connection refused");
		assertTrue(backend.listInstances("oauth").isEmpty());
	}

	@Test
	public void fallbackIsIsolatedByServiceAndDatacenter() {
		List<ServiceInstance> local = backend.listInstances("oauth");
		body = instances("remote", "passing");
		List<ServiceInstance> remote = backend.listInstances("oauth", "eu-west");
		assertEquals("http://localhost:8500/v1/health/service/oauth?near=_agent&dc=eu-west",
				requests.get(1).getURI().toString());

		status = 503;
		ticker.advanceMillis(1_000);
		assertSame(local, backend.listInstances("oauth"));
		assertSame(remote, backend.listInstances("oauth", "eu-west"));
		assertThrows(ConsultantException.class, () -> backend.listInstances("messaging"));
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth", "us-east"));
	}

	@Test
	public void normalCacheHitsDoNotExtendTheFallbackWindow() {
		backend.listInstances("oauth");
		ticker.advanceMillis(999);
		backend.listInstances("oauth");
		ticker.advanceMillis(14_001);
		status = 503;
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"));
	}

	@Test
	public void successfulFetchAfterFallbackExpiryStillRecovers() {
		backend.listInstances("oauth");
		ticker.advanceMillis(15_000);
		status = 503;
		assertThrows(ConsultantException.class, () -> backend.listInstances("oauth"));

		status = 200;
		body = instances("recovered", "passing");
		assertEquals("recovered", backend.listInstances("oauth").get(0).getService().getId());
	}

	private static Throwable rootCause(Throwable error) {
		while (error.getCause() != null) {
			error = error.getCause();
		}
		return error;
	}

	private static String instances(String id, String checkStatus) {
		return "[{\"Node\":{\"Node\":\"web-1\",\"Address\":\"127.0.0.1\"},"
				+ "\"Service\":{\"ID\":\"" + id + "\",\"Service\":\"oauth\",\"Port\":8080},"
				+ "\"Checks\":[{\"Name\":\"health\",\"Status\":\"" + checkStatus + "\"}]}]";
	}

}
