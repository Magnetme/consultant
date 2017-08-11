package me.magnet.consultant;

import static me.magnet.consultant.HttpUtils.createStatus;
import static me.magnet.consultant.HttpUtils.toJson;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ConfigUpdaterTest {

	private ScheduledExecutorService executor;
	private CloseableHttpClient http;
	private ObjectMapper objectMapper;
	private ServiceIdentifier id;

	@Before
	public void setUp() {
		this.executor = new ScheduledThreadPoolExecutor(1);
		this.http = mock(CloseableHttpClient.class);
		this.id = new ServiceIdentifier("oauth", null, null, null);
		this.objectMapper = new ObjectMapper();
	}

	@After
	public void tearDown() {
		executor.shutdownNow();
	}

	@Test(timeout = 5_000)
	public void verifyInitialConfigLoad() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("some-prefix/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Properties> future = SettableFuture.create();
		ConfigUpdater updater = new ConfigUpdater(executor, http, null, null, id, objectMapper, null, future::set,
				"some-prefix");

		updater.run();

		Properties properties = future.get();
		assertEquals("some-value", properties.getProperty("some.key"));
	}

	@Test(timeout = 5_000)
	public void verifyConsecutiveConfigLoad() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response1.getEntity()).thenReturn(toJson(ImmutableMap.of("some-prefix/oauth/some.key", "some-value")));

		CloseableHttpResponse response2 = mock(CloseableHttpResponse.class);
		when(response2.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
		when(response2.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response2.getEntity()).thenReturn(
				toJson(ImmutableMap.of("some-prefix/oauth/some.key", "some-other-value")));

		when(http.execute(any())).thenReturn(response1, response2);

		CountDownLatch latch = new CountDownLatch(2);
		AtomicReference<Properties> properties = new AtomicReference<>();

		ConfigUpdater updater = new ConfigUpdater(executor, http, null, null, id, objectMapper, null, (config) -> {
			latch.countDown();
			properties.set(config);
		}, "some-prefix");
		updater.run();

		latch.await();
		assertEquals("some-other-value", properties.get().getProperty("some.key"));
	}

	@Test(timeout = 10_000)
	public void verifyFolderIsIgnored() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("some-prefix/oauth/", "some-value",
				"some-prefix/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Properties> future = SettableFuture.create();
		ConfigUpdater updater = new ConfigUpdater(executor, http, null, null, id, objectMapper, null, future::set,
				"some-prefix");

		updater.run();

		Properties properties = future.get();
		assertEquals(properties.keySet(), Sets.newHashSet("some.key"));
	}

	@Test(timeout = 5_000, expected = TimeoutException.class)
	public void verifyNoUpdateOnChangingDifferentKey() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response1.getEntity()).thenReturn(toJson(ImmutableMap.of("some-prefix/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response1);

		SettableFuture<Properties> future = SettableFuture.create();
		id = new ServiceIdentifier("database", null, null, null);
		ConfigUpdater updater = new ConfigUpdater(executor, http, null, null, id, objectMapper, null, future::set,
				"some-prefix");

		updater.run();

		future.get(2000, TimeUnit.MILLISECONDS);
	}

	@Test(timeout = 10_000)
	public void verify404ReschedulesAfter5Seconds() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(404, "Not Found"));
		when(response1.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response1);
		ScheduledExecutorService executorSpy = spy(executor);

		SettableFuture<Properties> future = SettableFuture.create();
		id = new ServiceIdentifier("oauth", null, null, null);
		ConfigUpdater updater = new ConfigUpdater(executor, http, null, null, id, objectMapper, null, future::set,
				"some-prefix");

		updater.run();

		Thread.sleep(5100);
		verify(executorSpy, times(2));
	}

	@Test(timeout = 500_000)
	public void verifyThatUpdaterIsRescheduledAfterException() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response1.getEntity()).thenReturn(new StringEntity("{Some: \"weird object\"]"));

		when(http.execute(any())).thenReturn(response1);
		ScheduledExecutorService executorSpy = spy(executor);

		ConfigUpdater updater = new ConfigUpdater(executor, http, null, null, id, objectMapper, null, null, null);
		updater.run();

		Thread.sleep(1100);
		verify(executorSpy, times(2));
	}

}
