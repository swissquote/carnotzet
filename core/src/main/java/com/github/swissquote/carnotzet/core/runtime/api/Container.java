package com.github.swissquote.carnotzet.core.runtime.api;

import lombok.Value;

/**
 * describes a docker container managed by an orchestrator runtime
 */
@Value
public class Container {
	String id;
	String serviceName;
	boolean running;
	String ip;
}
