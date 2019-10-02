package com.github.swissquote.carnotzet.core.runtime.log;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class LogEvent {
	private final String service;
	private final int replicaNumber;
	private final String logEntry;

	// for backwards compatibility
	public LogEvent(String service, String logEntry) {
		this(service, 1, logEntry);
	}
}
