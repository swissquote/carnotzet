package com.github.swissquote.carnotzet.core.runtime.log;

import java.util.function.Function;

public interface LogListener {

	void accept(LogEvent event);

	/**
	 * null -> receive all previous log entries
	 * 0 -> receive none of the log entries
	 * N -> receive only N lines from each service
	 */
	default Integer getTail() {
		return null;
	}

	/**
	 * Indicates if the listener is interested only in past events (follow = false) or both past and future (follow = true)
	 */
	default boolean getFollow() {
		return false; // same default as docker
	}

	/**
	 * Filter the events that this listener will process
	 */
	void setEventFilter(Function<LogEvent, Boolean> filter);
}
