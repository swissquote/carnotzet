package com.github.swissquote.carnotzet.core.runtime.log;

import lombok.Value;

@Value
public class LogEvent {
	private final String service;
	private final String logEntry;
}
