package com.github.swissquote.carnotzet.core.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Know how to merge java .properties files
 * Env files are a subset of java .properties files so it can merge them too
 */
public class PropertiesAndEnvFileMerger implements FileMerger {

	@Override
	public boolean knowsHowToMerge(Path path) {
		return path.toString().endsWith(".properties")
				|| path.toAbsolutePath().toString().contains("/env/");
	}

	/**
	 * Properties of file2 have precedence over the ones in file1
	 */
	@Override
	public void merge(Path file1Path, Path file2Path, Path output) {
		Properties file1 = new Properties();
		Properties file2 = new Properties();
		Properties merged = new Properties();
		try {
			file1.load(Files.newInputStream(file1Path));
			file2.load(Files.newInputStream(file2Path));
			merged.putAll(file1);
			merged.putAll(file2);
			// We don't use merged.store because the output is not consistent.
			PropertyUtils.outputCleanPropFile(merged, output);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
