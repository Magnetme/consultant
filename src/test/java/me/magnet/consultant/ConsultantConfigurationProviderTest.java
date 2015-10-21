package me.magnet.consultant;

import static me.magnet.consultant.HttpUtils.createStatus;
import static me.magnet.consultant.HttpUtils.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;
import com.netflix.governator.configuration.ConfigurationKey;
import com.netflix.governator.configuration.Property;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConsultantConfigurationProviderTest {

	private static final ConfigurationKey CONFIGURATION_KEY = new ConfigurationKey("some.key", Lists.newArrayList());

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
	public void verifyLoadingStringProperty() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", "some-value"));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<String> result = provider.getStringProperty(CONFIGURATION_KEY, null);
		assertEquals("some-value", result.get());
	}

	@Test(timeout = 5_000)
	public void verifyFailingToLoadStringPropertyReturnsDefault() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.other.key", "true"));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<String> result = provider.getStringProperty(CONFIGURATION_KEY, "some-value");
		assertEquals("some-value", result.get());
	}

	@Test(timeout = 5_000)
	public void verifyLoadingBooleanProperty() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", "true"));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Boolean> result = provider.getBooleanProperty(CONFIGURATION_KEY, null);
		assertTrue(result.get());
	}

	@Test(timeout = 5333_000)
	public void verifyFailedToLoadBooleanPropertyReturnsDefault() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", ""));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Boolean> result = provider.getBooleanProperty(CONFIGURATION_KEY, true);
		assertTrue(result.get());
	}

	@Test(timeout = 5_000)
	public void verifyLoadingDateProperty() throws Exception {
		Date now = new Date();

		createConsultant(ImmutableMap.of("config/oauth/some.key", DateFormat.getInstance().format(now)));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		DateFormat dateFormat = provider.getDateFormat();
		Date reParsed = dateFormat.parse(dateFormat.format(now));

		Property<Date> result = provider.getDateProperty(CONFIGURATION_KEY, null);
		assertEquals(reParsed, result.get());
	}

	@Test(timeout = 5_000)
	public void verifyFailedToLoadDatePropertyReturnsDefault() throws Exception {
		Date now = new Date();
		createConsultant(ImmutableMap.of("config/oauth/some.key", ""));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Date> result = provider.getDateProperty(CONFIGURATION_KEY, now);
		assertEquals(now, result.get());
	}

	@Test(timeout = 5_000)
	public void verifyLoadingDoubleProperty() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", "5.4"));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Double> result = provider.getDoubleProperty(CONFIGURATION_KEY, null);
		assertTrue(5.4 == result.get());
	}

	@Test(timeout = 5_000)
	public void verifyFailedToLoadDoublePropertyReturnsDefault() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", ""));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Double> result = provider.getDoubleProperty(CONFIGURATION_KEY, 5.4);
		assertTrue(5.4 == result.get());
	}

	@Test(timeout = 5_000)
	public void verifyLoadingLongProperty() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", "1"));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Long> result = provider.getLongProperty(CONFIGURATION_KEY, null);
		assertTrue(1L == result.get());
	}

	@Test(timeout = 5_000)
	public void verifyFailedToLoadLongPropertyReturnsDefault() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", ""));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Long> result = provider.getLongProperty(CONFIGURATION_KEY, 1L);
		assertTrue(1L == result.get());
	}

	@Test(timeout = 5_000)
	public void verifyLoadingIntegerProperty() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", "1"));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Integer> result = provider.getIntegerProperty(CONFIGURATION_KEY, null);
		assertTrue(1 == result.get());
	}

	@Test(timeout = 5_000)
	public void verifyFailedToLoadIntegerPropertyReturnsDefault() throws Exception {
		createConsultant(ImmutableMap.of("config/oauth/some.key", ""));
		ConsultantConfigurationProvider provider = new ConsultantConfigurationProvider(consultant);
		Property<Integer> result = provider.getIntegerProperty(CONFIGURATION_KEY, 1);
		assertTrue(1 == result.get());
	}

	private void createConsultant(Map<String, String> entries) throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		when(response.getFirstHeader(eq("X-Consul-Index"))).thenReturn(new BasicHeader("X-Consul-Index", "1000"));
		when(response.getStatusLine()).thenReturn(createStatus(200, "OK"));
		when(response.getEntity()).thenReturn(toJson(entries));

		when(http.execute(any())).thenReturn(response);

		SettableFuture<Properties> future = SettableFuture.create();

		consultant = Consultant.builder()
				.usingHttpClient(http)
				.withConsulHost("http://localhost")
				.identifyAs("oauth", "eu-central", "web-1", "master")
				.onValidConfig(future::set)
				.build();

		future.get();
	}

}
