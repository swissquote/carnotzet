package com.github.swissquote.carnotzet.core.runtime.api;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * describes a docker container managed by an orchestrator runtime
 */
@Value
@AllArgsConstructor
public class Container {
	private final String id;
	private final String serviceName;
	private final boolean running;
	private final int replicaNumber; // Starts from 1
	private final String ip; // only present when container is running

	// backwards compatibility
	public Container(String id, String serviceName, boolean running, String ip) {
		this(id, serviceName, running, 1, ip);
	}

}
