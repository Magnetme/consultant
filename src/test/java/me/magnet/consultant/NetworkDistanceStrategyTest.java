package me.magnet.consultant;

import static org.junit.Assert.assertEquals;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;

public class NetworkDistanceStrategyTest extends RoutingStrategyTest {

	public NetworkDistanceStrategyTest() {
		super(RoutingStrategies.NETWORK_DISTANCE);
	}

	@Test
	public void testThatInstancesAreReturnedInOrder() {
		ServiceLocator locations = strategy.locateInstances(serviceInstanceBackend, SERVICE_1);
		List<ServiceInstance> instances = iterate(locations);
		assertEquals(Lists.newArrayList(dc1node1service1, dc1node2service1, dc1node3service1, dc2node1service1), instances);
	}

}
