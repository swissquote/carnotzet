package com.github.swissquote.carnotzet.core.runtime.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * This class captures and stores log events for analysis
 */
@Slf4j
public class LogEvents extends LogListenerBase {

	private final List<LogEvent> events;

	public LogEvents(Integer tail, boolean follow) {
		super(tail, follow);
		this.events = Collections.synchronizedList(new ArrayList<>());
	}

	public LogEvents() {
		super();
		this.events = Collections.synchronizedList(new ArrayList<>());
	}

	public List<LogEvent> getEvents() {
		return new ArrayList<>(events);
	}

	/**
	 * Consumes log events received
	 */
	public List<LogEvent> consumeEvents() {
		synchronized (events) {
			List<LogEvent> result = getEvents();
			clear();
			return result;
		}
	}

	/**
	 * Discards captured events
	 */
	public void clear() {
		this.events.clear();
	}

	public boolean hasEntry(String service, String content) {
		return getEvents().stream()
				.filter(event -> event.getService().equals(service))
				.map(LogEvent::getLogEntry)
				.anyMatch(entry -> entry.contains(content));
	}

	public void waitForEntry(String service, String content, long timeoutMillis, int checkIntervalMillis) {
		System.out.println("Waiting at most [" + timeoutMillis + "ms] until [" + content + "] appears in the logs of [" + service + "]");
		long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() < timeoutMillis + startTime) {
			if (hasEntry(service, content)) {
				System.out.println("Log entry [" + content + "] seen in logs of service [" + service + "] after [" + (System.currentTimeMillis()
						- startTime) + "ms]");
				return;
			}
			try {
				Thread.sleep(checkIntervalMillis);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		throw new RuntimeException("Did not to read [" + content + "] in logs of service [" + service + "] after [" + timeoutMillis + "ms].");
	}

	@Override
	public void acceptInternal(LogEvent event) {
		events.add(event);
	}
}
