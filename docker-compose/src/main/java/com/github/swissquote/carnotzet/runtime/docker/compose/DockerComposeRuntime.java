package com.github.swissquote.carnotzet.runtime.docker.compose;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.docker.registry.DockerRegistry;
import com.github.swissquote.carnotzet.core.docker.registry.ImageMetaData;
import com.github.swissquote.carnotzet.core.docker.registry.ImageRef;
import com.github.swissquote.carnotzet.core.runtime.CommandRunner;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.api.PullPolicy;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerComposeRuntime implements ContainerOrchestrationRuntime {

	private final Carnotzet carnotzet;

	private final String instanceId;

	private final DockerLogManager logManager;

	private final CommandRunner commandRunner;

	public DockerComposeRuntime(Carnotzet carnotzet) {
		this(carnotzet, carnotzet.getTopLevelModuleName());
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId, CommandRunner commandRunner) {
		this.carnotzet = carnotzet;
		if (instanceId != null) {
			this.instanceId = instanceId;
		} else {
			this.instanceId = carnotzet.getTopLevelModuleName();
		}
		this.logManager = new DockerLogManager();
		this.commandRunner = commandRunner;
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId) {
		this(carnotzet, instanceId, DefaultCommandRunner.INSTANCE);
	}

	private void computeDockerComposeFile() {
		log.debug(String.format("Building docker-compose.yml for [%s]", carnotzet.getConfig().getTopLevelModuleId()));

		Map<String, Service> services = new HashMap<>();
		List<CarnotzetModule> modules = carnotzet.getModules();
		for (CarnotzetModule module : modules) {
			if (module.getImageName() == null) {
				log.debug("Module [{}] has no docker image", module.getName());
				continue;
			}
			Service.ServiceBuilder serviceBuilder = Service.builder();
			String moduleName = module.getName();

			serviceBuilder.image(module.getImageName());
			serviceBuilder.volumes(module.getDockerVolumes());
			serviceBuilder.entrypoint(module.getDockerEntrypoint());
			serviceBuilder.command(module.getDockerCmd());
			serviceBuilder.env_file(module.getDockerEnvFiles());

			Map<String, ContainerNetwork> networks = new HashMap<>();
			Set<String> networkAliases = new HashSet<>();
			networkAliases.addAll(lookUpCustomAliases(module));

			// Carnotzet semantics name
			networkAliases.add(module.getName() + ".docker");
			networkAliases.add(instanceId + "." + module.getName() + ".docker");

			// Legacy compat (default dnsdock pattern)
			networkAliases.add(module.getShortImageName() + ".docker");
			networkAliases.add(instanceId + "_" + module.getName() + "." + module.getShortImageName() + ".docker");

			ContainerNetwork network = ContainerNetwork.builder().aliases(networkAliases).build();
			networks.put("carnotzet", network);
			serviceBuilder.networks(networks);

			Map<String, String> labels = new HashMap<>();
			if (module.getLabels() != null) {
				labels.putAll(module.getLabels());
			}
			labels.put("com.dnsdock.alias", networkAliases.stream().collect(Collectors.joining(",")));
			labels.put("carnotzet.instance.id", instanceId);
			labels.put("carnotzet.module.name", module.getName());
			labels.put("carnotzet.top.level.module.name", carnotzet.getTopLevelModuleName());

			serviceBuilder.labels(labels);
			serviceBuilder.extra_hosts(lookUpExtraHosts(module));

			services.put(moduleName, serviceBuilder.build());
		}

		Network network = Network.builder().driver("bridge").build();
		Map<String, Network> networks = new HashMap<>();
		networks.put("carnotzet", network);

		DockerCompose compose = DockerCompose.builder().version("2").services(services).networks(networks).build();
		DockerComposeGenerator generator = new DockerComposeGenerator(compose);
		try {
			Files.write(generator.generateDockerComposeFile(),
					carnotzet.getResourcesFolder().resolve("docker-compose.yml").toFile(),
					StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to write docker-compose.yml", e);
		}
		log.debug(String.format("End build compose file for module %s", carnotzet.getConfig().getTopLevelModuleId()));
	}

	private Collection<String> lookUpCustomAliases(CarnotzetModule m) {
		Set<String> result = new HashSet<>();
		if (m.getProperties() != null && m.getProperties().containsKey("network.aliases")) {
			result.addAll(parseNetworkAliases(m.getProperties().get("network.aliases")));
		}
		return result;
	}

	private List<String> parseNetworkAliases(String s) {
		return Arrays.stream(s.split(","))
				.map(String::trim)
				.collect(Collectors.toList());
	}

	private Set<String> lookUpExtraHosts(CarnotzetModule m) {
		Set<String> result = new HashSet<>();
		if (m.getProperties() != null && m.getProperties().containsKey("extra.hosts")) {
			result.addAll(Arrays.stream(m.getProperties().get("extra.hosts").split(","))
					.map(String::trim)
					.collect(Collectors.toList()));
		}
		return result;
	}

	@Override
	public void start() {
		log.debug("Forcing update of docker-compose.yml before start");
		computeDockerComposeFile();
		Instant start = Instant.now();
		carnotzet.getModules().stream().filter(this::shouldStartByDefault).forEach(m ->
				runCommand("docker-compose", "-p", getDockerComposeProjectName(), "up", "-d", m.getName())
		);
		ensureNetworkCommunicationIsPossible();
		logManager.ensureCapturingLogs(start, getContainers());
	}

	private boolean shouldStartByDefault(CarnotzetModule m) {
		if (m.getImageName() == null) {
			return false;
		}
		if (m.getProperties() == null){
			return true;
		}
		String str = m.getProperties().get("start.by.default");
		if (str == null){
			return true;
		}
		if (str.trim().toLowerCase().equals("false")){
			return false;
		}
		return true;
	}

	@Override
	public void start(String service) {
		log.debug("Forcing update of docker-compose.yml before start");
		computeDockerComposeFile();
		Instant start = Instant.now();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "up", "-d", service);
		ensureNetworkCommunicationIsPossible();
		logManager.ensureCapturingLogs(start, Collections.singletonList(getContainer(service)));
	}

	private void ensureNetworkCommunicationIsPossible() {
		String buildContainerId =
				runCommandAndCaptureOutput("/bin/sh", "-c", "docker ps | grep $(hostname) | grep -v k8s_POD | cut -d ' ' -f 1");

		if (Strings.isNullOrEmpty(buildContainerId)) {
			// we are probably not running inside a container, networking should be fine
			return;
		}

		log.debug("Execution from inside a container detected! Attempting to configure container networking to allow communication.");

		String networkMode =
				runCommandAndCaptureOutput("/bin/sh", "-c", "docker inspect -f '{{.HostConfig.NetworkMode}}' " + buildContainerId);

		String containerToConnect = buildContainerId;

		// shared network stack
		if (networkMode.startsWith("container:")) {
			containerToConnect = networkMode.replace("container:", "");
			log.debug("Detected a shared container network stack.");
		}
		log.debug("attaching container [" + containerToConnect + "] to network [" + getDockerNetworkName() + "]");
		runCommand("/bin/sh", "-c", "docker network connect " + getDockerNetworkName() + " " + containerToConnect);
	}

	private String getDockerComposeProjectName() {
		return normalizeDockerComposeProjectName(instanceId);
	}

	private String getDockerNetworkName() {
		return getDockerComposeProjectName() + "_carnotzet";
	}

	// normalize docker compose project name the same way docker-compose does (see https://github.com/docker/compose/tree/master/compose)
	private String normalizeDockerComposeProjectName(String moduleName) {
		return moduleName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
	}

	@Override
	public void stop() {
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "stop");
		// networks are created on demand and it's very fast, deleting the network upon stop helps avoiding sub-network starvation
		// when using a lot of docker networks
		runCommandAndCaptureOutput("docker", "network", "rm", getDockerNetworkName());
	}

	@Override
	public void stop(String service) {
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "stop", service);
	}

	@Override
	public void status() {
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "ps");
	}

	@Override
	public void clean() {
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "rm", "-f");
	}

	@Override
	public void shell(Container container) {
		ensureDockerComposeFileIsPresent();
		try {
			Process process = new ProcessBuilder("docker", "exec", "-it", container.getId(), "/bin/bash").inheritIO().start();
			process.waitFor();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot execute docker exec", ex);
		}
		catch (InterruptedException e) {
			//exit
		}
	}

	@Override
	public void pull() {
		pull(PullPolicy.ALWAYS);
	}

	@Override
	public void pull(PullPolicy policy) {
		// We need to check service by service if a newer version exists or not
		carnotzet.getModules().forEach(module -> pull(module.getName(), policy));
	}

	@Override
	public void pull(@NonNull String service) {
		pull(service, PullPolicy.ALWAYS);
	}

	@Override
	public void pull(@NonNull String service, PullPolicy policy) {
		// Find out the name and tag of the image we are trying to pull
		CarnotzetModule serviceModule = carnotzet.getModule(service)
				.orElseThrow(() -> new IllegalArgumentException("No such service: " + service));

		String imageName = serviceModule.getImageName();
		if (imageName == null) {
			// This module has no image. There is nothing to pull in any case
			return;
		}

		// fetch metadata if the policy needs it to take its decision
		Instant localTimestamp = null;
		if (policy.requiresLocalMetadata()) {
			localTimestamp = getLocalImageTimestamp(imageName);
		}
		ImageMetaData registryImageMetadata = null;
		if (policy.requiresRegistryMetadata()) {
			registryImageMetadata = getRegistryImageMetadata(imageName);
		}

		// pull if needed
		if (policy.shouldPullImage(serviceModule, localTimestamp, registryImageMetadata)) {
			ensureDockerComposeFileIsPresent();
			runCommand("docker-compose", "-p", getDockerComposeProjectName(), "pull", service);
		}
	}

	private Instant getLocalImageTimestamp(String imageName) {
		// Use docker inspect
		String isoDatetime = runCommandAndCaptureOutput("docker", "inspect", "-f", "{{.Created}}", imageName);
		try {
			return Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(isoDatetime));
		}
		catch (DateTimeException e) {
			log.debug("Could not determine timestamp of local image [" + imageName + "], it probably doesn't exist", e);
			return null;
		}
	}

	private ImageMetaData getRegistryImageMetadata(String imageName) {
		// Call docker registry to ask for image details.
		try {
			return DockerRegistry.INSTANCE.getImageMetaData(new ImageRef(imageName));
		}
		catch (CarnotzetDefinitionException cde) {
			log.debug("Could not determine metadata of registry image [" + imageName + "]", cde);
			return null;
		}
	}

	@Override
	public List<Container> getContainers() {
		String commandOutput = runCommandAndCaptureOutput("docker-compose", "-p", getDockerComposeProjectName(),
				"ps", "-q").replaceAll("\n", " ");
		log.debug("docker-compose ps output : " + commandOutput);
		if (commandOutput.trim().isEmpty()) {
			return Collections.emptyList();
		}
		List<String> args = new ArrayList<>(Arrays.asList("docker", "inspect", "-f", "{{ index .Id}}:"
				+ "{{ index .Config.Labels \"com.docker.compose.service\" }}:"
				+ "{{ index .State.Running}}:"
				+ "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}:"
		));
		args.addAll(Arrays.asList(commandOutput.split(" ")));
		commandOutput = runCommandAndCaptureOutput(args.toArray(new String[args.size()]));
		log.debug("docker inspect output : " + commandOutput);
		log.debug("split docker inspect output : " + Arrays.asList(commandOutput.split("\n")));
		return Arrays.stream(commandOutput.split("\n"))
				.filter(desc -> !desc.trim().isEmpty())
				.map(desc -> desc.split(":"))
				.map(parts -> new Container(parts[0], parts[1], parts[2].equals("true"), parts.length > 3 ? parts[3] : null))
				.sorted(Comparator.comparing(Container::getServiceName))
				.collect(Collectors.toList());

	}

	@Override
	public Container getContainer(String serviceName) {
		return getContainers().stream().filter(c -> c.getServiceName().equals(serviceName)).findFirst().orElse(null);
	}

	@Override
	public void registerLogListener(LogListener listener) {
		ensureDockerComposeFileIsPresent();
		logManager.registerLogListener(listener, getContainers());
	}

	@Override
	public boolean isRunning() {
		ensureDockerComposeFileIsPresent();
		return getContainers().stream().anyMatch(Container::isRunning);
	}

	private int runCommand(String... command) {
		return commandRunner.runCommand(carnotzet.getResourcesFolder().toFile(), command);
	}

	private String runCommandAndCaptureOutput(String... command) {
		return commandRunner.runCommandAndCaptureOutput(carnotzet.getResourcesFolder().toFile(), command);
	}

	private boolean dockerComposeFileExists() {
		return java.nio.file.Files.exists(carnotzet.getResourcesFolder().resolve("docker-compose.yml"));
	}

	private void ensureDockerComposeFileIsPresent() {
		if (dockerComposeFileExists()) {
			log.debug("Using existing docker-compose.yml");
			return;
		}
		log.debug("docker-compose.yml not found");
		computeDockerComposeFile();
	}

	public void clean(String service) {
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "rm", "-f", service);
	}
}
