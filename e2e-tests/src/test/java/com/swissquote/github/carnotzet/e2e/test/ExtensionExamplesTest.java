package com.swissquote.github.carnotzet.e2e.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.hamcrest.CoreMatchers;
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
		Path projectPom = FileSystems.getDefault().getPath("../examples/voting-worker").toRealPath();
		logFile = File.createTempFile("log", "txt", projectPom.toFile());
		mavenVerifier = new Verifier(projectPom.toAbsolutePath().toString());
		mavenVerifier.setLogFileName(logFile.getName());
		mavenVerifier.setAutoclean(true);
		mavenVerifier.setForkJvm(true);
	}

	@Test
	public void testClassPathExtension() throws VerificationException, IOException {

		mavenVerifier.verify(false);
		mavenVerifier.executeGoal("zet:start");

		String output = CommandRunner
				.runCommandAndCaptureOutput("docker", "ps", "--filter", "label=carnotzet.hello.message=Hello Carnotzet");
		Assert.assertThat(output.split("\n").length, CoreMatchers.equalTo(4));
	}

	@AfterClass
	public static void tearDown() throws VerificationException {
		mavenVerifier.executeGoal("zet:stop");
		mavenVerifier.executeGoal("zet:clean");
		mavenVerifier.resetStreams();
		logFile.delete();
	}
}
