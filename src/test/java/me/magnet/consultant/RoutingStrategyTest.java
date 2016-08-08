package me.magnet.consultant;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;

public abstract class RoutingStrategyTest {

	protected static final String SERVICE_1 = "hello";
	protected static final String SERVICE_2 = "world";
	protected static final String SERVICE_3 = "foobar";

	protected final RoutingStrategy strategy;

	protected ServiceInstanceBackend serviceInstanceBackend;

	protected ServiceInstance dc1node1service1;
	protected ServiceInstance dc1node2service1;
	protected ServiceInstance dc1node3service1;
	protected ServiceInstance dc2node1service1;

	protected ServiceInstance dc1node1service2;
	protected ServiceInstance dc1node2service2;
	protected ServiceInstance dc1node3service2;

	protected ServiceInstance dc1node1service3;
	protected ServiceInstance dc1node2service3;
	protected ServiceInstance dc1node3service3;

	public RoutingStrategyTest(RoutingStrategy routingStrategy) {
		this.strategy = routingStrategy;
	}

	@Before
	public void setUp() {
		Node dc1node1 = createNode("app1", "10.0.0.1");
		Node dc1node2 = createNode("app2", "10.0.0.2");
		Node dc1node3 = createNode("app3", "10.0.0.3");
		Node dc2node1 = createNode("app1", "10.0.1.1");

		Service service1 = createService(SERVICE_1);
		Service service2 = createService(SERVICE_2);
		Service service3 = createService(SERVICE_3);

		this.dc1node1service1 = new ServiceInstance(dc1node1, service1);
		this.dc1node2service1 = new ServiceInstance(dc1node2, service1);
		this.dc1node3service1 = new ServiceInstance(dc1node3, service1);
		this.dc2node1service1 = new ServiceInstance(dc2node1, service1);

		this.dc1node1service2 = new ServiceInstance(dc1node1, service2);
		this.dc1node2service2 = new ServiceInstance(dc1node2, service2);
		this.dc1node3service2 = new ServiceInstance(dc1node3, service2);

		this.dc1node1service3 = new ServiceInstance(dc1node1, service3);
		this.dc1node2service3 = new ServiceInstance(dc1node2, service3);
		this.dc1node3service3 = new ServiceInstance(dc1node3, service3);

		this.serviceInstanceBackend = mock(ServiceInstanceBackend.class);
		when(serviceInstanceBackend.listInstances(anyString())).thenAnswer(invocation -> {
			String serviceName = invocation.getArguments()[0].toString();
			return getInstances(serviceName, "dc1");
		});

		when(serviceInstanceBackend.listInstances(anyString(), anyString())).thenAnswer(invocation -> {
			String serviceName = invocation.getArguments()[0].toString();
			String dataCenter = invocation.getArguments()[1].toString();
			return getInstances(serviceName, dataCenter);
		});

		when(serviceInstanceBackend.getDatacenter()).thenReturn(Optional.of("dc1"));
		when(serviceInstanceBackend.listDatacenters()).thenReturn(Lists.newArrayList("dc1", "dc2"));
	}

	private List<ServiceInstance> getInstances(String serviceName, String datacenter) {
		switch (serviceName) {
			case SERVICE_1:
				switch (datacenter) {
					case "dc1":
						return Lists.newArrayList(dc1node1service1, dc1node2service1, dc1node3service1);
					case "dc2":
						return Lists.newArrayList(dc2node1service1);
				}
				break;
			case SERVICE_2:
				switch (datacenter) {
					case "dc1":
						return Lists.newArrayList(dc1node1service2, dc1node2service2, dc1node3service2);
					case "dc2":
						return Lists.newArrayList();
				}
				break;
			case SERVICE_3:
				switch (datacenter) {
					case "dc1":
						return Lists.newArrayList(dc1node1service3, dc1node2service3, dc1node3service3);
					case "dc2":
						return Lists.newArrayList();
				}
				break;
		}
		throw new IllegalArgumentException("Invalid datacenter: " + datacenter);
	}

	@After
	public void tearDown() {
		strategy.reset();
	}

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

	protected List<ServiceInstance> iterate(ServiceLocator locations) {
		Optional<ServiceInstance> instance;
		List<ServiceInstance> instances = Lists.newArrayList();
		while ((instance = locations.next()).isPresent()) {
			instances.add(instance.get());
		}
		return instances;
	}

}
