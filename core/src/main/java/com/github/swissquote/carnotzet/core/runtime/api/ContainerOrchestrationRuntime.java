package com.github.swissquote.carnotzet.core.runtime.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.swissquote.carnotzet.core.runtime.log.LogListener;

public interface ContainerOrchestrationRuntime {

	/**
	 * Start all applications
	 */
	void start();

	/**
	 * Start an application
	 *
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
	 *
	 * @param service to stop
	 */
	void stop(String service);

	/**
	 * Clean all resources of this environment in the container orchestrator (container / pod)
	 */
	void clean();

	/**
	 * Delete resources for a given service
	 *
	 * @param service to clean
	 */
	void clean(String service);

	/**
	 * outputs container status
	 */
	void status();

	/**
	 * Spawn an interactive shell inside the given service's container
	 *
	 * @param container to open the shell into
	 */
	void shell(Container container);

	/**
	 * Pulls all docker images used in this environment. If there is any local image with a tag that matches the pulled images, the local
	 * tag will be overridden and point to the freshly pushed images.
	 * Equivalent to calling pull(PullPolicy.ALWAYS).
	 */
	void pull();

	/**
	 * Pulls all docker images used in this environment according to the rules set by the specified PullPolicy.
	 *
	 * @param policy decides whether an image must be pulled or not
	 */
	void pull(PullPolicy policy);

	/**
	 * Pulls a single docker image used in this environment. If there is any local image with a tag that matches the pulled image, the local
	 * tag will be overridden and point to the freshly pushed image.
	 * Equivalent to calling pull(service, PullPolicy.ALWAYS).
	 *
	 * @param service the name of the service whose docker image to be pulled
	 */
	void pull(String service);

	/**
	 * Pulls a single docker image used in this environment	according to the rules set by the specified PullPolicy.
	 *
	 * @param service the name of the service whose docker image to be pulled
	 * @param policy  decides whether an image must be pulled or not
	 */
	void pull(String service, PullPolicy policy);

	/**
	 * Executes a command in a service container
	 *
	 * @param service the name of the service in which's container to execute the command
	 * @param timeout maximum execution time allowed
	 * @param timeoutUnit Time unit for the timeout
	 * @param command The command to execute
	 * @return The execution result
	 */
	ExecResult exec(String service, int timeout, TimeUnit timeoutUnit, String... command);

	/**
	 * List containers in the environment
	 * <p>
	 * If a service has multiple replicas, all containers will be returned
	 * @return the list of all containers for this environment
	 */
	List<Container> getContainers();

	/**
	 * get details about a specific container in the environment
	 * <p>
	 * equivalent to getContainer(servicename, 1)
	 *
	 * @param serviceName in the environment
	 * @return details about the container
	 */
	Container getContainer(String serviceName);

	/**
	 * get all replica containers for a given service
	 *
	 * @param serviceName in the environment
	 * @return details about the container
	 */
	List<Container> getContainers(String serviceName);

	/**
	 * get details about a specific container
	 *
	 * @param serviceName in the environment
	 * @param number the replica number
	 * @return details about the container
	 */
	Container getContainer(String serviceName, int number);

	/**
	 * Register a listener for log events.
	 * log event will be sent asynchronously to the listener.
	 * Can be called before or after start()
	 *
	 * @param listener to register
	 */
	void registerLogListener(LogListener listener);

}
