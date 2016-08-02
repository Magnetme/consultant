package me.magnet.consultant;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RoundRobinStrategyTest extends RoutingStrategyTest {

	private static final String SERVICE_1 = "hello";
	private static final String SERVICE_2 = "world";
	private static final String SERVICE_3 = "foobar";

	private final RoutingStrategy strategy = RoutingStrategies.ROUND_ROBIN;

	private ServiceLocator serviceLocator;

	private ServiceInstance node1service1;
	private ServiceInstance node2service1;
	private ServiceInstance node3service1;

	private ServiceInstance node1service2;
	private ServiceInstance node2service2;
	private ServiceInstance node3service2;

	private ServiceInstance node1service3;
	private ServiceInstance node2service3;
	private ServiceInstance node3service3;

	@Before
	public void setUp() {
		Node node1 = createNode("app1", "10.0.0.1");
		Node node2 = createNode("app2", "10.0.0.2");
		Node node3 = createNode("app3", "10.0.0.3");

		Service service1 = createService(SERVICE_1);
		Service service2 = createService(SERVICE_2);
		Service service3 = createService(SERVICE_3);

		this.node1service1 = new ServiceInstance(node1, service1);
		this.node2service1 = new ServiceInstance(node2, service1);
		this.node3service1 = new ServiceInstance(node3, service1);

		this.node1service2 = new ServiceInstance(node1, service2);
		this.node2service2 = new ServiceInstance(node2, service2);
		this.node3service2 = new ServiceInstance(node3, service2);

		this.node1service3 = new ServiceInstance(node1, service3);
		this.node2service3 = new ServiceInstance(node2, service3);
		this.node3service3 = new ServiceInstance(node3, service3);

		AtomicInteger counter = new AtomicInteger();

		this.serviceLocator = mock(ServiceLocator.class);
		when(serviceLocator.listInstances(anyString())).thenAnswer(invocation -> {
			int count = counter.getAndIncrement();
			String serviceName = invocation.getArguments()[0].toString();
			switch (serviceName) {
				case SERVICE_1:
					return Lists.newArrayList(node1service1, node2service1, node3service1);
				case SERVICE_2:
					return Lists.newArrayList(node1service2, node2service2, node3service2);
				case SERVICE_3:
					List<ServiceInstance> instances = Lists.newArrayList(node1service3, node2service3, node3service3);
					switch (count) {
						case 0:
							break;
						case 1:
							instances.remove(1);
						case 2:
							break;
					}
					return instances;
				default:
					return Lists.newArrayList();
			}
		});
	}

	@After
	public void tearDown() {
		strategy.reset();
	}

	@Test
	public void testFirstEntryIsInstanceOnFirstNode() {
		ServiceLocations locator = strategy.locateInstances(serviceLocator, SERVICE_1);
		ServiceInstance first = locator.next().get();
		assertEquals(node1service1, first);
	}

	@Test
	public void testSecondEntryIsInstanceOnSecondNode() {
		ServiceLocations locator1 = strategy.locateInstances(serviceLocator, SERVICE_1);
		ServiceLocations locator2 = strategy.locateInstances(serviceLocator, SERVICE_1);

		locator1.next().get();
		ServiceInstance second = locator2.next().get();

		assertEquals(node2service1, second);
	}

	@Test
	public void testSecondEntryIsInstanceOnThirdNode() {
		ServiceLocations locator1 = strategy.locateInstances(serviceLocator, SERVICE_1);
		ServiceLocations locator2 = strategy.locateInstances(serviceLocator, SERVICE_1);

		locator1.next().get();
		locator1.next().get();
		ServiceInstance third = locator2.next().get();

		assertEquals(node3service1, third);
	}

	@Test
	public void testForthEntryIsInstanceOnFirstNode() {
		ServiceLocations locator1 = strategy.locateInstances(serviceLocator, SERVICE_1);
		ServiceLocations locator2 = strategy.locateInstances(serviceLocator, SERVICE_1);

		locator1.next().get();
		locator2.next().get();
		locator1.next().get();
		ServiceInstance fourth = locator2.next().get();

		assertEquals(node1service1, fourth);
	}

	@Test
	public void testFirstEntryOfOtherServiceIsInstanceOnFirstNode() {
		ServiceLocations locator1 = strategy.locateInstances(serviceLocator, SERVICE_1);
		ServiceLocations locator2 = strategy.locateInstances(serviceLocator, SERVICE_2);

		locator1.next().get();
		ServiceInstance first = locator2.next().get();

		assertEquals(node1service2, first);
	}

}
