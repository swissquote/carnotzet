package com.github.swissquote.carnotzet.core.runtime;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

public class DefaultCommandRunnerTest {

	@Test
	public void test_large_output() throws Exception {

		// create temp file of about 274k
		final File tmp = File.createTempFile("carnotzet-test", null);
		tmp.deleteOnExit();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			sb.append("This line is repeated a lot\n");
		}
		String expected = sb.toString();
		FileUtils.write(tmp, expected);

		// When
		String actual = callWithTimeout(
				() -> DefaultCommandRunner.INSTANCE.runCommandAndCaptureOutput("cat", tmp.getAbsolutePath()),
				2, TimeUnit.SECONDS);

		// Then
		Assert.assertThat(actual, Is.is(expected.trim()));
	}

	private String callWithTimeout(Callable<String> command, Integer amount, TimeUnit unit)
			throws ExecutionException, InterruptedException, TimeoutException {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(command);
		try {
			return future.get(amount, unit);
		} finally {
			future.cancel(true);
			executor.shutdownNow();
		}
	}

}
