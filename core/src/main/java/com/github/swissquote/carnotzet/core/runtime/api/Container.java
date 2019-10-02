package com.github.swissquote.carnotzet.core.runtime.api;

import lombok.Value;

/**
 * describes a docker container managed by an orchestrator runtime
 */
@Value
public class Container {
	private final String id;
	private final String serviceName;
	private final boolean running;
	private final int number; // Starts from 1, aka replica number
	private final String ip; // only present when container is running
}
