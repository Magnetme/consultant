package me.magnet.consultant;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ServiceLocations {

	private final Supplier<Iterator<ServiceInstance>> instanceSupplier;
	private final Supplier<ServiceLocations> fallbackSupplier;

	private Iterator<ServiceInstance> instances;
	private ServiceLocations fallback;

	private Consumer<ServiceInstance> listener;

	public ServiceLocations(Supplier<Iterator<ServiceInstance>> instanceSupplier) {
		this(instanceSupplier, (ServiceLocations) null);
	}

	public ServiceLocations(Supplier<Iterator<ServiceInstance>> instanceSupplier, ServiceLocations fallback) {
		this(instanceSupplier, () -> fallback);
	}

	public ServiceLocations(Supplier<Iterator<ServiceInstance>> instanceSupplier, Supplier<ServiceLocations> fallbackSupplier) {
		this.instanceSupplier = instanceSupplier;
		this.fallbackSupplier = fallbackSupplier;
	}

	public ServiceLocations setListener(Consumer<ServiceInstance> listener) {
		this.listener = listener;
		if (fallback != null) {
			fallback.setListener(listener);
		}
		return this;
	}

	public ServiceLocations map(Function<Iterator<ServiceInstance>, Iterator<ServiceInstance>> mapper) {
		if (fallback != null) {
			return new ServiceLocations(() -> mapper.apply(instanceSupplier.get()), fallback.map(mapper));
		}
		return new ServiceLocations(() -> mapper.apply(instanceSupplier.get()));
	}

	public Optional<ServiceInstance> next() {
		if (instances == null) {
			instances = instanceSupplier.get();
		}
		if (instances.hasNext()) {
			ServiceInstance instance = instances.next();
			if (listener != null) {
				listener.accept(instance);
			}
			return Optional.of(instance);
		}

		if (fallback == null) {
			fallback = fallbackSupplier.get();
		}
		if (fallback != null) {
			return fallback.next();
		}

		return Optional.empty();
	}

}
