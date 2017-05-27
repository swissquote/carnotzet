package com.swissquote.github.carnotzet.e2e.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.swissquote.carnotzet.core.runtime.CommandRunner;

public class ExtensionExamplesTest {

	private static File logFile;
	private static Verifier mavenVerifier;

	@BeforeClass
	public static void setUp() throws IOException, VerificationException {
		Path projectPom = FileSystems.getDefault().getPath("../examples/redis").toRealPath();
		logFile = File.createTempFile("log", "txt", projectPom.toFile());
		mavenVerifier = new Verifier(projectPom.toAbsolutePath().toString());
		mavenVerifier.setLogFileName(logFile.getName());
		mavenVerifier.setAutoclean(true);
		mavenVerifier.setForkJvm(true);
	}

	@AfterClass
	public static void tearDown() throws VerificationException {
		mavenVerifier.executeGoal("zet:stop");
		mavenVerifier.executeGoal("zet:clean");
		mavenVerifier.resetStreams();
		logFile.delete();
	}

	@Test
	public void testClassPathExtension() throws VerificationException, IOException {

		mavenVerifier.verify(false);
		mavenVerifier.executeGoal("zet:start");

		String output = CommandRunner
				.runCommandAndCaptureOutput("docker", "ps", "-q", "--filter", "label=carnotzet.hello.message=Hello Carnotzet");
		Assert.assertFalse("The output should contain single entry for redis zet module", output.isEmpty());
	}
}
