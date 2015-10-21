package me.magnet.consultant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.junit.Test;

public class PropertiesUtilTest {

	@Test(expected = NullPointerException.class)
	public void verifyThatSyncMethodWithNullSourcePropertiesThrowsException() {
		PropertiesUtil.sync(null, new Properties());
	}

	@Test(expected = NullPointerException.class)
	public void verifyThatSyncMethodWithNullTargetPropertiesThrowsException() {
		PropertiesUtil.sync(new Properties(), null);
	}

	@Test
	public void verifyThatAllPropertiesInSourceAreTransferredToTarget() {
		Properties source = new Properties();
		Properties target = new Properties();

		source.setProperty("key-1", "some-value");
		PropertiesUtil.sync(source, target);

		assertEquals("some-value", target.getProperty("key-1"));
	}

	@Test
	public void verifyThatAllPropertiesNotInSourceAreRemovedFromTarget() {
		Properties source = new Properties();
		Properties target = new Properties();

		target.setProperty("key-1", "some-value");
		PropertiesUtil.sync(source, target);

		assertFalse(target.containsKey("key-1"));
	}

	@Test
	public void verifyThatAllPropertiesInSourceOverrideTargetProperties() {
		Properties source = new Properties();
		Properties target = new Properties();

		source.setProperty("key-1", "some-value");
		target.setProperty("key-1", "some-other-value");
		PropertiesUtil.sync(source, target);

		assertEquals("some-value", target.getProperty("key-1"));
	}

	@Test
	public void verifyMerge() {
		Properties source = new Properties();
		Properties target = new Properties();

		source.setProperty("key-1", "some-value");
		source.setProperty("key-2", "other-value");

		target.setProperty("key-1", "some-other-value");
		target.setProperty("key-3", "new-value");

		PropertiesUtil.sync(source, target);

		assertEquals("some-value", target.getProperty("key-1"));
		assertEquals("other-value", target.getProperty("key-2"));
		assertNull(target.getProperty("key-3"));
	}

}
