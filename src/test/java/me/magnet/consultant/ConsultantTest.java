package me.magnet.consultant;

import static me.magnet.consultant.HttpUtils.createStatus;
import static me.magnet.consultant.HttpUtils.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import me.magnet.consultant.Consultant.Builder.Agent;
import me.magnet.consultant.Consultant.Builder.Config;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsultantTest {

	private Consultant consultant;
	private MockedHttpClientBuilder httpBuilder;

	@Before
	public void setUp() throws IOException {
		httpBuilder = prepareHttpClient();
	}

	@After
	public void tearDown() throws InterruptedException {
		if (consultant != null) {
			consultant.shutdown();
		}
	}

	@Test(timeout = 5_000)
	public void verifyInitialConfigLoad() throws Exception {
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig(future::set)
				.build();

		Properties properties = future.get();
		assertEquals("some-value", properties.getProperty("some.key"));
	}

	@Test(timeout = 5_000, expected = TimeoutException.class)
	public void verifyThatInvalidConfigIsNotPublished() throws Exception {
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
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
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
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
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		SettableFuture<Triple<String, String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> {
					future.set(Triple.of(key, oldValue, newValue));
				})
				.build();

		Triple<String, String, String> expected = Triple.of("some.key", null, "some-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyThatModifiedSettingsArePublishedThroughSettingsListener() throws Exception {
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true&index=1000", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-other-value")));
			return response;
		});

		SettableFuture<Triple<String, String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
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
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true&index=1000", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of()));
			return response;
		});

		SettableFuture<Pair<String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
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
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true&index=1000", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-other-value")));
			return response;
		});

		CountDownLatch latch = new CountDownLatch(2);

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig((config) -> latch.countDown())
				.build();

		Properties properties = consultant.getProperties();

		latch.await();
		assertEquals("some-other-value", properties.getProperty("some.key"));
	}

	@Test
	public void verifyPropertiesCanBeSetAsEnvironment() throws Exception {
		System.setProperty("CONSUL_HOST", "http://localhost");
		System.setProperty("SERVICE_NAME", "oauth");
		System.setProperty("SERVICE_DC", "eu-central");
		System.setProperty("SERVICE_HOST", "web-1");
		System.setProperty("SERVICE_INSTANCE", "master");

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.build();

		ServiceIdentifier id = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");
		assertEquals(id, consultant.getServiceIdentifier());
	}

	@Test
	public void verifyConsulHostDefaultsToPort8500() throws Exception {
		consultant = Consultant.builder()
				.identifyAs("oauth")
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("localhost")
				.build();

		assertEquals("http://localhost:8500", consultant.getConsulHost());
	}

	@Test
	public void verifyConsulHostDefaultsToHttp() throws Exception {
		consultant = Consultant.builder()
				.identifyAs("oauth")
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("localhost:8501")
				.build();

		assertEquals("http://localhost:8501", consultant.getConsulHost());
	}

	@Test
	public void verifyConsulHostWithSchemeDoesNotDefaultToDifferentPort() throws Exception {
		consultant = Consultant.builder()
				.identifyAs("oauth")
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.build();

		assertEquals("http://localhost", consultant.getConsulHost());
	}

	@Test
	public void verifyConfigListenerCanBeRemoved() throws Exception {
		ConfigListener listener = System.out::println;
		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth")
				.onValidConfig(listener)
				.build();

		assertTrue(consultant.removeConfigListener(listener));
		assertFalse(consultant.removeConfigListener(listener));
	}

	@Test
	public void verifySettingListenerCanBeRemoved() throws Exception {
		SettingListener listener = (name, oldValue, newValue) -> {};
		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth")
				.onSettingUpdate("some-key", listener)
				.build();

		assertTrue(consultant.removeSettingListener("some-key", listener));
		assertFalse(consultant.removeSettingListener("some-key", listener));
	}

	@Test(timeout = 5_000)
	public void verifyThatIrrelevantOverridesAreIgnored() throws Exception {
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/[dc=test].some.key", "some-value")));
			return response;
		});

		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true&index=1000", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-other-value")));
			return response;
		});

		SettableFuture<Pair<String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> future.set(Pair.of(key, newValue)))
				.build();

		Pair<String, String> expected = Pair.of("some.key", "some-other-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyThatRelevantOverridesAreProcessed() throws Exception {
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true&index=1000", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1001"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/[dc=eu-central].some.key", "some-other-value")));
			return response;
		});

		SettableFuture<Pair<String, String>> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> {
					if (oldValue != null) {
						future.set(Pair.of(key, newValue));
					}
				})
				.build();

		Pair<String, String> expected = Pair.of("some.key", "some-other-value");
		assertEquals(expected, future.get(2_000, TimeUnit.MILLISECONDS));
	}

	@Test(timeout = 5_000)
	public void verifyThatSpecifyingNoConfigPullDoesNotUpdateConfig() throws Exception {
		httpBuilder.onGet("/v1/kv/config/oauth/?recurse=true", request -> {
			CloseableHttpResponse response = mock(CloseableHttpResponse.class);
			when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
			when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
			when(response.getEntity()).thenReturn(toJson(ImmutableMap.of("config/oauth/some.key", "some-value")));
			return response;
		});

		CountDownLatch latch = new CountDownLatch(1);

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.pullConfigFromConsul(false)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central")
				.onSettingUpdate("some.key", (key, oldValue, newValue) -> {
					latch.countDown();
				})
				.build();

		assertFalse(latch.await(4, TimeUnit.SECONDS));
	}

	@Test(timeout = 5_000)
	public void testSpecifyingCustomProperties() throws Exception {
		Properties properties = new Properties();
		properties.setProperty("some.key", "some-value");

		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.pullConfigFromConsul(false)
				.startWith(properties)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central")
				.build();

		Properties expected = new Properties();
		expected.setProperty("some.key", "some-value");

		assertEquals(expected, consultant.getProperties());
	}

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatSpecifyingNullForCustomPropertiesThrowsException() throws Exception {
		consultant = Consultant.builder()
				.usingHttpClient(httpBuilder.create())
				.pullConfigFromConsul(false)
				.startWith(null)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central")
				.build();
	}

	private MockedHttpClientBuilder prepareHttpClient() throws IOException {
		return new MockedHttpClientBuilder()
				.onGet("/v1/agent/self", request -> {
					CloseableHttpResponse response = mock(CloseableHttpResponse.class);
					when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));

					Config config = new Config();
					config.setDatacenter("eu-central");
					config.setNodeName("app1");

					Agent agent = new Agent();
					agent.setConfig(config);

					when(response.getEntity()).thenReturn(toJson(agent));
					return response;
				});
	}

}
