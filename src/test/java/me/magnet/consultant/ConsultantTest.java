package me.magnet.consultant;

import static me.magnet.consultant.HttpUtils.createStatus;
import static me.magnet.consultant.HttpUtils.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsultantTest {

	private Consultant consultant;
	private CloseableHttpClient http;

	@Before
	public void setUp() {
		this.http = mock(CloseableHttpClient.class);
	}

	@After
	public void tearDown() {
		consultant.shutdown();
	}

	@Test(timeout = 5_000)
	public void verifyInitialConfigLoad() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig(future::set)
				.build();

		Properties properties = future.get();
		assertEquals("some-value", properties.getProperty("some.key"));
	}

	@Test(timeout = 5_000, expected = TimeoutException.class)
	public void verifyThatInvalidConfigIsNotPublished() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig(future::set)
				.validateConfigWith((config) -> {
					throw new IllegalArgumentException("Config is invalid");
				})
				.build();

		future.get(2_000, TimeUnit.MILLISECONDS);
	}

	@Test(timeout = 5_000)
	public void verifyThatValidConfigIsPublished() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig(future::set)
				.build();

		Properties expected = new Properties();
		expected.setProperty("some.key", "some-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyThatNewSettingsArePublishedThroughSettingsListener() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Triple<String, String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> {
					future.set(Triple.of(key, oldValue, newValue));
				})
				.build();

		Triple<String, String, String> expected = Triple.<String, String, String>of("some.key", null, "some-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyThatModifiedSettingsArePublishedThroughSettingsListener() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response1.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		CloseableHttpResponse response2 = mock(CloseableHttpResponse.class);
		when(response2.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
		when(response2.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response2.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-other-value")));

		when(http.execute(any())).thenReturn(response1, response2);
		SettableFuture<Triple<String, String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> {
					if (oldValue != null) {
						future.set(Triple.of(key, oldValue, newValue));
					}
				})
				.build();

		Triple<String, String, String> expected = Triple.of("some.key", "some-value", "some-other-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyThatDeletedSettingsArePublishedThroughSettingsListener() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response1.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		CloseableHttpResponse response2 = mock(CloseableHttpResponse.class);
		when(response2.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
		when(response2.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response2.getEntity()).thenReturn(toJson(ImmutableMap.of()));

		when(http.execute(any())).thenReturn(response1, response2);
		SettableFuture<Pair<String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> {
					if (newValue == null) {
						future.set(Pair.of(key, oldValue));
					}
				})
				.build();

		Pair<String, String> expected = Pair.of("some.key", "some-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyPropertiesObjectIsUpdatedOnNewConfig() throws Exception {
		CloseableHttpResponse response1 = mock(CloseableHttpResponse.class);
		when(response1.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response1.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response1.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));

		CloseableHttpResponse response2 = mock(CloseableHttpResponse.class);
		when(response2.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
		when(response2.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response2.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-other-value")));

		when(http.execute(any())).thenReturn(response1, response2);
		CountDownLatch latch = new CountDownLatch(2);

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig((config) -> latch.countDown())
				.build();

		Properties properties = consultant.getProperties();

		latch.await();
		assertEquals("some-other-value", properties.getProperty("some.key"));
	}

	@Test
	public void verifyPropertiesCanBeSetAsEnvironment() {
		System.setProperty("CONSUL_HOST", "http://localhost");
		System.setProperty("SERVICE_NAME", "oauth");
		System.setProperty("SERVICE_DC", "eu-central");
		System.setProperty("SERVICE_HOST", "web-1");
		System.setProperty("SERVICE_INSTANCE", "master");

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.build();

		ServiceIdentifier id = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");
		assertEquals(id, consultant.getServiceIdentifier());
	}

	@Test
	public void verifyConfigListenerCanBeRemoved() {
		ConfigListener listener = System.out::println;
		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth")
				.onValidConfig(listener)
				.build();

		assertTrue(consultant.removeConfigListener(listener));
		assertFalse(consultant.removeConfigListener(listener));
	}

	@Test
	public void verifySettingListenerCanBeRemoved() {
		SettingListener listener = (name, oldValue, newValue) -> {};
		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth")
				.onSettingUpdate("some-key", listener)
				.build();

		assertTrue(consultant.removeSettingListener("some-key", listener));
		assertFalse(consultant.removeSettingListener("some-key", listener));
	}

}
