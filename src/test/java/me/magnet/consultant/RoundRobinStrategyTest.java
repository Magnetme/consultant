package me.magnet.consultant;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RoundRobinStrategyTest extends RoutingStrategyTest {

	public RoundRobinStrategyTest() {
		super(RoutingStrategies.ROUND_ROBIN);
	}

	@Test
	public void testFirstEntryIsInstanceOnFirstNode() {
		ServiceLocator locator = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);
		ServiceInstance first = locator.next().get();
		assertEquals(dc1node1service1, first);
	}

	@Test
	public void testSecondEntryIsInstanceOnSecondNode() {
		ServiceLocator locator1 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);
		ServiceLocator locator2 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);

		locator1.next().get();
		ServiceInstance second = locator2.next().get();

		assertEquals(dc1node2service1, second);
	}

	@Test
	public void testSecondEntryIsInstanceOnThirdNode() {
		ServiceLocator locator1 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);
		ServiceLocator locator2 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);

		locator1.next().get();
		locator1.next().get();
		ServiceInstance third = locator2.next().get();

		assertEquals(dc1node3service1, third);
	}

	@Test
	public void testForthEntryIsInstanceOnFirstNode() {
		ServiceLocator locator1 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);
		ServiceLocator locator2 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);

		locator1.next().get();
		locator2.next().get();
		locator1.next().get();
		ServiceInstance fourth = locator2.next().get();

		assertEquals(dc1node1service1, fourth);
	}

	@Test
	public void testFirstEntryOfOtherServiceIsInstanceOnFirstNode() {
		ServiceLocator locator1 = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);
		ServiceLocator locator2 = strategy.locateInstances(serviceInstanceBackend, SERVICE_2);

		locator1.next().get();
		ServiceInstance first = locator2.next().get();

		assertEquals(dc1node1service2, first);
	}

}
