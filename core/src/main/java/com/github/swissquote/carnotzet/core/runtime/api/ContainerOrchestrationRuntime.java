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
	 * Pull all docker images used in this environment. If there is any local image with a tag that matches the pulled images, the local
	 * tag will be overridden and point to the freshly pushed images.
	 * Equivalent to calling pull(true).
	 */
	void pull();

	/**
	 * Pull all docker images used in this environment unless the force parameter is set to false and local images are more recent than
	 * the corresponding images on the remote repository. Calling pull(false) will make sure that the most recent version of all images is
	 * available locally after this method returns.
	 *
	 * @param force true overrides the local image if it exists, false only overrides it if the registry image is more recent
	 */
	void pull(boolean force);

	/**
	 * Pull a docker image used in this environment. If there is any local image with a tag that matches the pulled image, the local
	 * tag will be overridden and point to the freshly pushed image.
	 * Equivalent to calling pull(service, true).
	 *
	 * @param service to pull
	 */
	void pull(String service);

	/**
	 * Pull a docker image used in this environment	unless the force parameter is set to false and the local image is more recent than
	 * the corresponding image on the remote repository. Calling pull(service, false) will make sure that the most recent version of the
	 * image is available locally after this method returns.
	 *
	 * @param service to pull
	 * @param force   true overrides the local image if it exists, false only overrides it if the registry image is more recent
	 */
	void pull(String service, boolean force);

	/**
	 * List containers managed by this
	 *
	 * @return the list of all containers for this environment
	 */
	List<Container> getContainers();

	/**
	 * get details about a specific container
	 *
	 * @param serviceName in the environment
	 * @return details about the container
	 */
	Container getContainer(String serviceName);

	/**
	 * Register a listener for log events.
	 * log event will be sent asynchronously to the listener.
	 * Can be called before or after start()
	 *
	 * @param listener to register
	 */
	void registerLogListener(LogListener listener);

}
