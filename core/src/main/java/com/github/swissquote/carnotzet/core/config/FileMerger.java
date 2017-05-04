package com.github.swissquote.carnotzet.core.config;

import java.nio.file.Path;

/**
 * SPI to merge two configuration files in the hierarchical config system.
 */
public interface FileMerger {

	/**
	 * Merges two files, The content of file2 should have precedence over the one in file1
	 * Implementations of this method must support the output path to be the same as either input file path (input file should be overwritten).
	 * @param file1 first file (lower priority)
	 * @param file2 second file (higher priority)
	 * @param output where the result will be written
	 */
	void merge(Path file1, Path file2, Path output);

	/**
	 * Indicates if this file merger is able to merge a given file.
	 * @param file to merge
	 * @return true if the file is supported
	 */
	boolean knowsHowToMerge(Path file);
	
}
