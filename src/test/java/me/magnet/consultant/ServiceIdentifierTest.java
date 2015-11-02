package me.magnet.consultant;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class ServiceIdentifierTest {

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatConstructorThrowsExceptionWhenServiceNameIsNull() {
		new ServiceIdentifier(null, null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatConstructorThrowsExceptionWhenServiceNameIsEmpty() {
		new ServiceIdentifier("", null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatConstructorThrowsExceptionWhenDatacenterIsEmpty() {
		new ServiceIdentifier("oauth", "", null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatConstructorThrowsExceptionWhenHostIsEmpty() {
		new ServiceIdentifier("oauth", null, "", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatConstructorThrowsExceptionWhenInstanceIsEmpty() {
		new ServiceIdentifier("oauth", null, null, "");
	}

	@Test(expected = NullPointerException.class)
	public void verifyThatCallingMoreSpecificThanMethodThrowsExceptionOnNull() {
		new ServiceIdentifier("oauth", null, null, null).moreSpecificThan(null);
	}

	@Test
	public void verifyThatServiceNameOnlyIsLessSpecificThanServiceNameAndDC() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", null, null, null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", null, null);

		assertTrue(id2.moreSpecificThan(id1));
		assertFalse(id1.moreSpecificThan(id2));
	}

	@Test
	public void verifyThatServiceNameAndDCIsLessSpecificThanServiceNameDCAndHost() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", "eu-central", null, null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", null);

		assertTrue(id2.moreSpecificThan(id1));
		assertFalse(id1.moreSpecificThan(id2));
	}

	@Test
	public void verifyThatServiceNameDCAndHostIsLessSpecificThanAllFieldsSet() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", "eu-central", "web-1", null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertTrue(id2.moreSpecificThan(id1));
		assertFalse(id1.moreSpecificThan(id2));
	}

	@Test
	public void verifyThatSameServiceNameApplies() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", null, null, null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertTrue(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatDifferentServiceNameDoesNotApply() {
		ServiceIdentifier id1 = new ServiceIdentifier("logstash", null, null, null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertFalse(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatSameDCApplies() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", "eu-central", null, null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertTrue(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatDifferentDCDoesNotApply() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", "us-east", null, null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertFalse(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatSameHostApplies() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", null, "web-1", null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertTrue(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatDifferentHostDoesNotApply() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", null, "web-2", null);
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertFalse(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatSameInstanceApplies() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", null, null, "master");
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertTrue(id1.appliesTo(id2));
	}

	@Test
	public void verifyThatDifferentInstanceDoesNotApply() {
		ServiceIdentifier id1 = new ServiceIdentifier("oauth", null, null, "slave");
		ServiceIdentifier id2 = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");

		assertFalse(id2.appliesTo(id1));
		assertFalse(id1.appliesTo(id2));
	}

	@Test
	public void verifyEqualsMethod() {
		EqualsVerifier.forClass(ServiceIdentifier.class)
				.allFieldsShouldBeUsed()
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

}
