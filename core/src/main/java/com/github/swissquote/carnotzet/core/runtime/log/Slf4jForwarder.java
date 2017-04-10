package com.github.swissquote.carnotzet.core.runtime.log;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * Forwards logs to SLF4J
 */
@Slf4j
public class Slf4jForwarder extends LogListenerBase {

	public Slf4jForwarder() {
		super();
	}

	public Slf4jForwarder(Integer tail, boolean follow) {
		super(tail, follow);
	}

	@Override
	public void acceptInternal(LogEvent event) {
		Logger logger = org.slf4j.LoggerFactory.getLogger(event.getService());
		logger.info(event.getLogEntry());
	}

}
