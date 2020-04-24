package com.github.swissquote.carnotzet.core.runtime;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeoutException;

import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

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
		ProcessExecutor pe = new ProcessExecutor()
				.command(command)
				.directory(directoryForRunning);

		// by default, zt-exec pumps the logs to a nullOutputStream, so we don't need to pump or redirect to a file

		if (inheritIo) {
			// we forward output lines to SLF4J by default, writing to stdout can cause issue
			// (for example writing from a forked JVM when running in surefire produces error messages)
			pe = pe.redirectOutput(Slf4jStream.of(log).asInfo());
			pe = pe.redirectError(Slf4jStream.of(log).asInfo());
		}

		try {
			ProcessResult processResult = pe.execute();
			log.debug("Command completed : [{}]", Joiner.on(" ").join(command));
			return processResult.getExitValue();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CarnotzetDefinitionException(e);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (TimeoutException e) {
			throw new CarnotzetDefinitionException(e);
		}
	}

	public String runCommandAndCaptureOutput(String... command) {
		return runCommandAndCaptureOutput(new File("/"), command);
	}

	public String runCommandAndCaptureOutput(File directoryForRunning, String... command) {
		log.debug("Running command [{}]", Joiner.on(" ").join(command));

		ProcessExecutor pe = new ProcessExecutor()
				.command(command)
				.redirectErrorStream(true)
				.directory(directoryForRunning)
				.readOutput(true);
		try {
			ProcessResult processResult = pe.execute();
			String output = processResult.outputUTF8().trim();
			if (processResult.getExitValue() != 0) {
				throw new RuntimeException("External command [" + Joiner.on(" ").join(command) + "] exited with [" + processResult.getExitValue()
						+ "], output: " + output);
			}
			return output;
		}
		catch (InterruptedException | IOException | TimeoutException e) {
			throw new RuntimeException(e);
		}

	}

}
