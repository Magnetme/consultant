package me.magnet.consultant;

/**
 * Specialized interface which can be used to locate a bunch of service instances. Implementations of this interface can
 * be used to do client-side load balancing, and determine the order in which the service instances can be tried.
 */
public interface RoutingStrategy {

	/**
	 * Creates a new ServiceLocator object which can be used to locate one or more instances according to
	 * implementation of this interface. The service instances matching the service name may be returned
	 * in any particular order.
	 *
	 * @param serviceInstanceBackend The ServiceInstanceBackend to use to fetch data from Consul.
	 * @param serviceName The name of the service instance to locate.
	 * @return A ServiceLocator object to locate instances.
	 */
	ServiceLocator locateInstances(ServiceInstanceBackend serviceInstanceBackend, String serviceName);

	/**
	 * Resets any internal state of this RoutingStrategy implementation.
	 */
	default void reset() {
		// Do nothing
	}

}
