package me.magnet.consultant;

import static org.junit.Assert.assertEquals;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class PathTest {

	@Test(expected = NullPointerException.class)
	public void verifyThatConstructorThrowsExceptionOnNullAsServiceIdentifier() {
		new Path(null, null, null);
	}

	@Test
	public void verifyThatConstructorDoesNotThrowExceptionWhenServiceIdentifierIsNotNull() {
		new Path(null, new ServiceIdentifier("oauth", null, null, null), null);
	}

	@Test
	public void verifyToStringOnFullPath() {
		ServiceIdentifier id = new ServiceIdentifier("oauth", "eu-central", "web-1", "master");
		Path path = new Path("some-prefix/sub-fix", id, "some.key");
		assertEquals("some-prefix/sub-fix/oauth/[dc=eu-central,host=web-1,instance=master]/some.key", path.toString());
	}

	@Test
	public void verifyEqualsMethod() {
		EqualsVerifier.forClass(Path.class)
				.suppress(Warning.STRICT_INHERITANCE)
				.verify();
	}

}
