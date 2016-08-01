package me.magnet.consultant;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class RoutingStrategies {

	public static final RoutingStrategy RANDOMIZED_WEIGHTED_DISTANCE = new RoutingStrategy() {

		private final Random random = new Random();

		@Override
		public List<ServiceInstance> listInstances(ServiceLocator serviceLocator, String serviceName) {
			List<ServiceInstance> instances = serviceLocator.listInstances(serviceName);
			if (instances.size() <= 1) {
				// No routing to do.
				return instances;
			}

			// Pick a random number between 0-1.
			double chance = random.nextDouble();

			/*
			 * Pick the index of the first instance to try based on this chance.
			 * If the chance was < 0.5 then use the instance on index 0.
			 * If the chance was >= 0.5 and < 0.75 then use the instance on index 1.
			 * If the chance was >= 0.75 and < 0.875 then use the instance on index 2.
			 * If the chance was >= 0.875 and < 0.9375 then use the instance on index 3.
			 * Etc...
			 */
			int index = 0;
			double threshold = 0.5;
			while (threshold < chance && index < instances.size() - 1) {
				index++;
				threshold = Math.pow(0.5, index + 1);
			}

			return splice(instances, index);
		}
	};

	public static final RoutingStrategy ROUND_ROBIN = new RoutingStrategy() {

		private final Multimap<String, ServiceInstance> lastRequested = HashMultimap.create();

		@Override
		public List<ServiceInstance> listInstances(ServiceLocator serviceLocator, String serviceName) {
			List<ServiceInstance> nextAvailable = serviceLocator.listInstances(serviceName);
			Set<ServiceInstance> lastRequests = Sets.newHashSet(lastRequested.get(serviceName));

			// If all instances have been hit at least once, reset the counter for this service.
			if (lastRequests.containsAll(nextAvailable)) {
				lastRequested.removeAll(serviceName);
			}

			// List the instances to hit in order of not hit to already hit (conserving network distance).
			List<ServiceInstance> nextUp = nextAvailable.stream()
					.filter(next -> !lastRequests.contains(next))
					.collect(Collectors.toList());
			nextAvailable.stream()
					.filter(lastRequests::contains)
					.forEachOrdered(nextUp::add);

			// Register that nextUp[0] instance is going to get hit first.
			if (!nextUp.isEmpty()) {
				lastRequested.put(serviceName, nextUp.get(0));
			}

			return nextUp;
		}
	};

	public static final RoutingStrategy RANDOMIZED = new RoutingStrategy() {

		private final Random random = new Random();

		@Override
		public List<ServiceInstance> listInstances(ServiceLocator serviceLocator, String serviceName) {
			List<ServiceInstance> instances = serviceLocator.listInstances(serviceName);
			int index = random.nextInt(instances.size());
			return splice(instances, index);
		}
	};

	public static final RoutingStrategy NETWORK_DISTANCE = ServiceLocator::listInstances;

	private static List<ServiceInstance> splice(List<ServiceInstance> instances, int index) {
		// If we picked the first instance return the instances in order.
		if (index == 0) {
			return instances;
		}

		// Else create a new list of instances starting with the index, and looping round the end of the list.
		List<ServiceInstance> result = Lists.newArrayList(instances.subList(index, instances.size() - 1));
		result.addAll(instances.subList(0, index - 1));
		return result;
	}

	private RoutingStrategies() {
		// Prevent instantiation.
	}

}
