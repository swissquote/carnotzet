package com.github.swissquote.carnotzet.core.runtime.log;

import java.util.function.Predicate;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LogListenerBase implements LogListener {

	public static final int DEFAULT_TAIL = 1000;

	/**
	 * Indicates how many lines of logs to get at max
	 */
	private Integer tail;

	/**
	 * Indicates if this listener should
	 */
	private boolean follow;

	@Setter
	private Predicate<LogEvent> eventFilter;

	public LogListenerBase() {
		this.tail = DEFAULT_TAIL;
		this.follow = true;
	}

	public LogListenerBase(Integer tail, boolean follow) {
		this.tail = tail;
		this.follow = follow;
	}

	@Override
	public Integer getTail() {
		return tail;
	}

	@Override
	public boolean getFollow() {
		return follow;
	}

	@Override
	public final void accept(LogEvent event) {
		if (eventFilter != null && !eventFilter.test(event)) {
			return;
		}
		acceptInternal(event);
	}

	public abstract void acceptInternal(LogEvent event);

}
