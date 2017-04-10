package com.github.swissquote.carnotzet.core.runtime.log;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public abstract class LogListenerBase implements LogListener {

	/**
	 * Indicates how many lines of logs to get at max
	 */
	private Integer tail;

	/**
	 * Indicates if this listener should
	 */
	private boolean follow;

	@Setter
	private Function<LogEvent, Boolean> eventFilter;

	public LogListenerBase() {
		this.tail = 1000;
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
		if (eventFilter != null && !eventFilter.apply(event)) {
			return;
		}
		acceptInternal(event);
	}

	public abstract void acceptInternal(LogEvent event);

}
