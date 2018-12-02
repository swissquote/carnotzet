package com.github.swissquote.carnotzet.core.runtime.api;

import lombok.Value;

@Value
public class ExecResult {
	private final int exitCode;
	private final String output;
}
