package com.github.swissquote.carnotzet.core.runtime.api;

import java.util.List;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;

public interface ContainerOrchestrationRuntime {

	/**
	 * Start all applications
	 */
	void start();

	/**
	 * Start an application
	 * @param service the application to start
	 */
	void start(String service);

	/**
	 * @return true if any of the services in the environment is currently running
	 */
	boolean isRunning();

	/**
	 * Stop all running applications
	 */
	void stop();

	/**
	 * Stop an application
	 * @param service to stop
	 */
	void stop(String service);

	/**
	 * Clean all resources of this environment in the container orchestrator (container / pod)
	 */
	void clean();

	/**
	 * Delete resources for a given service
	 * @param service to clean
	 */
	void clean(String service);

	/**
	 * outputs container status
	 */
	void status();

	/**
	 * Spawn an interactive shell inside the given service's container
	 * @param container to open the shell into
	 */
	void shell(Container container);

	/**
	 * Pull all docker images used in this environment
	 */
	void pull();

	/**
	 * Pull a docker image used in this environment
	 * @param service to pull
	 */
	void pull(String service);

	/**
	 * List containers managed by this
	 * @return the list of all containers for this environment
	 */
	List<Container> getContainers();

	/**
	 * get details about a specific container
	 * @param serviceName in the environment
	 * @return details about the container
	 */
	Container getContainer(String serviceName);

	/**
	 * Register a listener for log events.
	 * log event will be sent asynchronously to the listener.
	 * Can be called before or after start()
	 * @param listener to register
	 */
	void registerLogListener(LogListener listener);

}
