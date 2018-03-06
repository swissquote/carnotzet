package com.github.swissquote.cartnotzet.core.runtime;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.google.common.util.concurrent.SimpleTimeLimiter;

public class DefaultCommandRunnerTest {

	@Test
	public void test_large_output() throws Exception {

		// create temp file of about 274k
		final File tmp = File.createTempFile("carnotzet-test", null);
		tmp.deleteOnExit();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10000; i++){
			sb.append("This line is repeated a lot\n");
		}
		String expected = sb.toString();
		FileUtils.write(tmp, expected);

		// When
		String actual = new SimpleTimeLimiter().callWithTimeout(
				() -> DefaultCommandRunner.INSTANCE.runCommandAndCaptureOutput("cat", tmp.getAbsolutePath()),
				2, TimeUnit.SECONDS, true);

		// Then
		Assert.assertThat(actual, Is.is(expected));
	}

}
