package com.github.swissquote.carnotzet.core.runtime.log;

import java.util.function.Predicate;

public interface LogListener {

	void accept(LogEvent event);

	/**
	 * @return one of :<br>
	 * null : receive all previous log entries<br>
	 * 0 : receive none of the log entries<br>
	 * N : receive only N lines from each service
	 */
	default Integer getTail() {
		return null;
	}

	/**
	 * @return true if the listener is interested in both past and future events,
	 * false if it is only interested in past events
	 */
	default boolean getFollow() {
		return false; // same default as docker
	}

	/**
	 * Filter the events that this listener will process
	 * @param predicate to decide if accept() should be invoked or not for an event.
	 */
	void setEventFilter(Predicate<LogEvent> predicate);
}
