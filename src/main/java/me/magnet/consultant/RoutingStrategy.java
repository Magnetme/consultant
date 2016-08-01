package me.magnet.consultant;

import java.util.List;

public interface RoutingStrategy {

	List<ServiceInstance> listInstances(ServiceLocator serviceLocator, String serviceName);

}
