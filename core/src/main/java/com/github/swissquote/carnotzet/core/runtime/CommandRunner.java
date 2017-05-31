package com.github.swissquote.carnotzet.core.runtime;

import java.io.File;

public interface CommandRunner {
	int runCommand(String... command);

	int runCommand(Boolean inheritIo, String... command);

	int runCommand(File directoryForRunning, String... command);

	int runCommand(Boolean inheritIo, File directoryForRunning, String... command);

	String runCommandAndCaptureOutput(String... command);

	String runCommandAndCaptureOutput(File directoryForRunning, String... command);

}
