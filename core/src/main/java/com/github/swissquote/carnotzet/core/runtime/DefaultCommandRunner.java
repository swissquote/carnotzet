package com.github.swissquote.carnotzet.core.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.apache.commons.lang.SystemUtils;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.google.common.base.Joiner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
public final class DefaultCommandRunner implements CommandRunner {

	public static final DefaultCommandRunner INSTANCE = new DefaultCommandRunner();

	private DefaultCommandRunner() {
	}

	public int runCommand(String... command) {
		return runCommand(new File("/"), command);
	}

	public int runCommand(Boolean inheritIo, String... command) {
		return runCommand(inheritIo, new File("/"), command);
	}

	public int runCommand(File directoryForRunning, String... command) {
		return runCommand(true, directoryForRunning, command);
	}

	public int runCommand(Boolean inheritIo, File directoryForRunning, String... command) {
		log.debug("Running command [{}]", Joiner.on(" ").join(command));
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(directoryForRunning);
		if (inheritIo) {
			pb.inheritIO();
		} else {
			File sink = getOsSpecificSink();
			pb.redirectError(sink);
			pb.redirectOutput(sink);
		}
		try {
			Process p = pb.start();
			p.waitFor();
			log.debug("Command completed : [{}]", Joiner.on(" ").join(command));
			return p.exitValue();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CarnotzetDefinitionException(e);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private File getOsSpecificSink() {
		if (SystemUtils.IS_OS_WINDOWS) {
			return new File("NUL");
		}
		return new File("/dev/null");
	}

	public String runCommandAndCaptureOutput(String... command) {
		return runCommandAndCaptureOutput(new File("/"), command);
	}

	public String runCommandAndCaptureOutput(File directoryForRunning, String... command) {

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(directoryForRunning);
		try {
			Process p = pb.start();
			p.waitFor();
			String stdOut = getInputAsString(p.getInputStream());
			String stdErr = getInputAsString(p.getErrorStream());
			return stdOut.trim() + stdErr.trim();
		}
		catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}

	}

	private static String getInputAsString(InputStream is) {
		try (java.util.Scanner s = new java.util.Scanner(is, "UTF-8")) {
			return s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
	}

}
