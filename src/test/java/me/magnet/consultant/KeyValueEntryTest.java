package me.magnet.consultant;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.Test;

public class KeyValueEntryTest {

	@Test
	public void verifyThatPublicNoArgsConstructorExists() throws ReflectiveOperationException {
		Constructor<?>[] constructors = KeyValueEntry.class.getDeclaredConstructors();

		Stream.of(constructors)
				.filter(constructor -> Modifier.isPublic(constructor.getModifiers()))
				.filter(constructor -> constructor.getParameterCount() == 0)
				.findFirst()
				.orElseThrow(() -> new ReflectiveOperationException("The class '" + KeyValueEntry.class
						+ "' must contain one public no-args constructor!"));
	}

}
