package me.magnet.consultant;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NetworkDistanceStrategyTest extends RoutingStrategyTest {

	private static final String SERVICE = "hello";

	private final RoutingStrategy strategy = RoutingStrategies.NETWORK_DISTANCE;

	private ServiceLocator serviceLocator;

	private ServiceInstance node1service1;
	private ServiceInstance node2service1;
	private ServiceInstance node3service1;

	@Before
	public void setUp() {
		Node node1 = createNode("app1", "10.0.0.1");
		Node node2 = createNode("app2", "10.0.0.2");
		Node node3 = createNode("app3", "10.0.0.3");

		Service service = createService(SERVICE);

		this.node1service1 = new ServiceInstance(node1, service);
		this.node2service1 = new ServiceInstance(node2, service);
		this.node3service1 = new ServiceInstance(node3, service);

		this.serviceLocator = mock(ServiceLocator.class);
		when(serviceLocator.listInstances(anyString())).thenAnswer(invocation ->
			Lists.newArrayList(node1service1, node2service1, node3service1));
	}

	@After
	public void tearDown() {
		strategy.reset();
	}

	@Test
	public void testThatInstancesAreReturnedInOrder() {
		ServiceLocations locations = strategy.locateInstances(serviceLocator, SERVICE);
		List<ServiceInstance> instances = iterate(locations);
		assertEquals(Lists.newArrayList(node1service1, node2service1, node3service1), instances);
	}

}
