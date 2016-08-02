package me.magnet.consultant;

public interface RoutingStrategy {

	ServiceLocations locateInstances(ServiceLocator serviceLocator, String serviceName);

	default void reset() {
		// Do nothing
	}

}
