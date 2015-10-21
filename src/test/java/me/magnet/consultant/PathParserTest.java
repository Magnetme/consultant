package me.magnet.consultant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PathParserTest {

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatNullInputForPathThrowsException() {
		PathParser.parse(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void verifyThatEmptyInputForPathThrowsException() {
		PathParser.parse(null, "");
	}

	@Test
	public void verifyThatOnlyServiceNameIsParsedCorrectly() {
		Path actual = PathParser.parse(null, "oauth");
		Path expected = new Path(null, new ServiceIdentifier("oauth", null, null, null), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatPrefixParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", null, null, null), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatDifferentPrefixReturnsNull() {
		Path actual = PathParser.parse("some-other-prefix", "some-prefix/oauth");
		assertNull(actual);
	}

	@Test
	public void verifyOnlyServiceNameWithDCIsParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/[dc=eu-central]");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", "eu-central", null, null), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatServiceNameWithHostIsParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/[host=web-1]");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", null, "web-1", null), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatServiceNameWithInstanceIsParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/[instance=master]");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", null, null, "master"), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatServiceNameWithMultipleFieldsIsParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/[dc=eu-central,host=web-1,instance=master]");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", "eu-central", "web-1", "master"), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatServiceNameWithMultipleFieldsInDifferentOrderIsParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/[instance=master,host=web-1,dc=eu-central]");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", "eu-central", "web-1", "master"), null);
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatConfigKeyIsParsedCorrectly() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/[instance=master,host=web-1,dc=eu-central].some-key");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", "eu-central", "web-1", "master"), "some-key");
		assertEquals(expected, actual);
	}

	@Test
	public void verifyThatConfigKeyIsParsedCorrectlyWithDescriptors() {
		Path actual = PathParser.parse("some-prefix", "some-prefix/oauth/some-key");
		Path expected = new Path("some-prefix", new ServiceIdentifier("oauth", null, null, null), "some-key");
		assertEquals(expected, actual);
	}

}
