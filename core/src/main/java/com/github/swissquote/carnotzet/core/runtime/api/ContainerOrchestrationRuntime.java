package com.github.swissquote.carnotzet.core.runtime.api;

import java.util.List;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;

public interface ContainerOrchestrationRuntime {

	/**
	 * Start all applications
	 */
	void start();

	/**
	 * Start one application
	 */
	void start(String service);

	/**
	 * Indicates if any of the containers managed by this orchestrator is currently running
	 */
	boolean isRunning();

	/**
	 * Stop all running applications
	 */
	void stop();

	/**
	 * Stop one running application
	 */
	void stop(String service);

	/**
	 * Clean all resources of this environment in the container orchestrator
	 * Should call "onClean" an all log listeners
	 */
	void clean();

	/**
	 * Clean one service
	 */
	void clean(String service);

	/**
	 * outputs container status
	 */
	void status();

	/**
	 * Spawn an interactive shell inside the given service's container
	 */
	void shell(Container container);

	/**
	 * Pull all docker images used in this environment
	 */
	void pull();

	/**
	 * Pull a docker image used in this environment
	 */
	void pull(String service);

	/**
	 * List containers managed by this
	 */
	List<Container> getContainers();

	/**
	 * get details about a specific container
	 */
	Container getContainer(String serviceName);

	/**
	 * Register a listener for log events
	 * log event will be sent asynchronously to the listener
	 */
	void registerLogListener(LogListener listener);

}
