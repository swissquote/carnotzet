package com.github.swissquote.carnotzet.runtime.docker.compose;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.log.LogEvent;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.log.LogListenerBase;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Possible improvements to this class include : - reorder previous log events by timestamp when outputting previous log events (may be tricky to
 * implement) - Expose "since" in the api of ListenerBase to allow users to define it.
 */
@Slf4j
		/* package */ class DockerLogManager {

	@Value
	private static class ContainerListener {
		Container container;
		LogListener listener;
	}

	private final Collection<LogListener> logListeners;

	private final Map<ContainerListener, Flowable<LogEvent>> captureStreams;

	/* package */ DockerLogManager() {
		this.logListeners = new CopyOnWriteArrayList<>();
		this.captureStreams = new ConcurrentHashMap<>();
	}

	/**
	 * makes sure all registered listeners are capturing logs of specified containers
	 **/
	/* package */ void ensureCapturingLogs(Instant from, Collection<Container> containers) {
		log.debug("registering log listeners [{}] with containers [{}]", logListeners, containers);
		logListeners.forEach(listener -> containers.forEach(container -> ensureCapturingContainerLogs(container, from, listener)));
	}

	/**
	 * Ensures that a listener is listening to the logs of a container, from a given time
	 **/
	@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")
	private void ensureCapturingContainerLogs(Container container, Instant since, LogListener listener) {

		ContainerListener key = new ContainerListener(container, listener);
		Flowable stream = captureStreams.get(key);
		if (stream != null) {
			return;
		}

		List<String> command = getLogCommand(since, listener, container);
		log.debug("Scheduling new log capture flowable for container [{}] and listener [{}], command is [{}]",
				container, listener, String.join(" ", command));

		try {
			Process dockerCliProcess = new ProcessBuilder(command.toArray(new String[command.size()])).start();

			Flowable<String> stdOutLines = flowableInputStreamScanner(dockerCliProcess.getInputStream()).subscribeOn(Schedulers.newThread());
			Flowable<String> stdErrLines = flowableInputStreamScanner(dockerCliProcess.getErrorStream()).subscribeOn(Schedulers.newThread());
			Flowable<String> allLines = stdOutLines.mergeWith(stdErrLines);
			Flowable<LogEvent> allEvents = allLines.map(s -> new LogEvent(container.getServiceName(), container.getReplicaNumber(), s));
			allEvents.subscribe(listener::accept, Throwable::printStackTrace, () -> captureStreams.remove(key));
			captureStreams.put(key, allEvents);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Flowable<String> flowableInputStreamScanner(InputStream inputStream) {
		return Flowable.create(subscriber -> {
			try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
				while (scanner.hasNext()) {
					subscriber.onNext(scanner.nextLine());
				}
			}
			subscriber.onComplete();
		}, BackpressureStrategy.BUFFER);
	}

	private List<String> getLogCommand(Instant since, LogListener listener, Container container) {
		List<String> command = Arrays.asList("docker", "logs");
		Integer tail = listener.getTail();
		if (since != null) {
			String sinceTimestamp = Long.toString(since.getEpochSecond());
			command.add("--since");
			command.add(sinceTimestamp);
			tail = LogListenerBase.DEFAULT_TAIL;
		}
		if (listener.getFollow()) {
			command.add("--follow");
		}

		if (listener.getTail() != null) {
			command.add("--tail=" + tail);
		}
		command.add(container.getId());
		return command;
	}

	/* package */ void registerLogListener(LogListener listener, Collection<Container> containers) {
		log.debug("registering log listener [{}]", listener);
		logListeners.add(listener);
		containers.forEach(container -> ensureCapturingContainerLogs(container, null, listener));
	}
}
