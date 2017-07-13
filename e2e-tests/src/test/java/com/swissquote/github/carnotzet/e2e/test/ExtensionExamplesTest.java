package com.swissquote.github.carnotzet.e2e.test;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;

public class ExtensionExamplesTest {

	@Test
	public void testClassPathExtension() throws IOException {
		try {
			runGoal("zet:start");
			String output = DefaultCommandRunner.INSTANCE
					.runCommandAndCaptureOutput("docker", "ps", "-q", "--filter", "label=carnotzet.hello.message=Hello Carnotzet");
			Assert.assertFalse("The output should contain single entry for redis zet module", output.isEmpty());
		}
		finally {
			runGoal("zet:stop");
			runGoal("zet:clean");

		}
	}

	private void runGoal(String goal) throws IOException {
		DefaultCommandRunner.INSTANCE.runCommand(Paths.get("../examples/redis").toRealPath().toFile(), "mvn", goal);
	}
}
