package me.magnet.consultant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

public abstract class RoutingStrategyTest {

	protected Node createNode(String node, String address) {
		Node result = mock(Node.class);
		when(result.getNode()).thenReturn(node);
		when(result.getAddress()).thenReturn(address);
		return result;
	}

	protected Service createService(String serviceName) {
		int port = serviceName.hashCode();

		Service result = mock(Service.class);
		when(result.getId()).thenReturn(serviceName);
		when(result.getPort()).thenReturn(port);
		when(result.getService()).thenReturn(serviceName);
		when(result.getTags()).thenReturn(new String[0]);
		when(result.getAddress()).thenReturn("localhost:" + port);
		return result;
	}

	protected List<ServiceInstance> iterate(ServiceLocations locations) {
		Optional<ServiceInstance> instance;
		List<ServiceInstance> instances = Lists.newArrayList();
		while ((instance = locations.next()).isPresent()) {
			instances.add(instance.get());
		}
		return instances;
	}

}
