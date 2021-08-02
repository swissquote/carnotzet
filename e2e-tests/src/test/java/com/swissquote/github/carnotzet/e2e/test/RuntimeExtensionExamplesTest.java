package com.swissquote.github.carnotzet.e2e.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;

public class RuntimeExtensionExamplesTest {

	private final static Pattern IP_PATTERN = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

	@Test
	public void testClassPathExtension() throws IOException {
		try {
			runGoal("zet:start");
			String output = DefaultCommandRunner.INSTANCE
					.runCommandAndCaptureOutput("docker", "ps", "-q", "--filter",
							"label=carnotzet.runtime.hello.message=Hello Carnotzet Runtime");
			assertFalse("The output should contain single entry for redis zet module", output.isEmpty());
			List<String> lines = Files.readAllLines(Paths.get("/tmp/carnotzet_ip_dump/redis"));
			assertTrue(IP_PATTERN.matcher(lines.get(0)).find());
		}
		finally {
			runGoal("zet:stop");
			runGoal("zet:clean");
		}

		assertFalse(new File("/tmp/carnotzet_ip_dump").exists());

	}

	private void runGoal(String goal) throws IOException {
		DefaultCommandRunner.INSTANCE.runCommand(Paths.get("../examples/redis").toRealPath().toFile(), "mvn", goal);
	}

}
