package com.github.swissquote.carnotzet.runtime.docker.compose;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.docker.DockerUtils;
import com.github.swissquote.carnotzet.core.docker.registry.DockerRegistry;
import com.github.swissquote.carnotzet.core.runtime.CommandRunner;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.api.Container;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntime;
import com.github.swissquote.carnotzet.core.runtime.api.ContainerOrchestrationRuntimeExtension;
import com.github.swissquote.carnotzet.core.runtime.api.ExecResult;
import com.github.swissquote.carnotzet.core.runtime.api.PullPolicy;
import com.github.swissquote.carnotzet.core.runtime.log.LogListener;
import com.github.swissquote.carnotzet.core.runtime.spi.ContainerOrchestrationRuntimeDefaultExtensionsProvider;
import com.github.swissquote.carnotzet.core.util.Sha256;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DockerComposeRuntime implements ContainerOrchestrationRuntime {

	private Carnotzet carnotzet;

	private final String instanceId;

	private final DockerLogManager logManager;

	private final CommandRunner commandRunner;

	private final Boolean shouldExposePorts;

	private final List<ContainerOrchestrationRuntimeExtension> extensions;

	private static final boolean IS_OS_WINDOWS = isWindows();

	private static final boolean IS_OS_MAC = isMac();

	private static final boolean IS_OS_LINUX = isLinux();

	private static boolean isWindows() {
		try {
			String osName = System.getProperty("os.name");
			return osName.startsWith("Windows");
		}
		catch (SecurityException e) {
			return false;
		}
	}

	private static boolean isMac() {
		try {
			String osName = System.getProperty("os.name");
			return osName.startsWith("Mac");
		}
		catch (SecurityException e) {
			return false;
		}
	}

	private static boolean isLinux() {
		try {
			String osName = System.getProperty("os.name");
			return osName.toLowerCase().startsWith("linux");
		}
		catch (SecurityException e) {
			return false;
		}
	}

	public DockerComposeRuntime(Carnotzet carnotzet) {
		this(carnotzet, carnotzet.getTopLevelModuleName());
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId, CommandRunner commandRunner) {
		// Due to limitations in docker for mac and windows, mapping local ports to container ports is the preferred technique for those users.
		// https://docs.docker.com/docker-for-mac/networking/#i-cannot-ping-my-containers
		this(carnotzet, instanceId, commandRunner, IS_OS_MAC || IS_OS_WINDOWS);
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId, CommandRunner commandRunner, Boolean shouldExposePorts) {
		this(carnotzet, instanceId, commandRunner, shouldExposePorts, getDefaultRuntimeExtensions());
	}

	private static List<ContainerOrchestrationRuntimeExtension> getDefaultRuntimeExtensions() {
		ServiceLoader<ContainerOrchestrationRuntimeDefaultExtensionsProvider> loader =
				ServiceLoader.load(ContainerOrchestrationRuntimeDefaultExtensionsProvider.class);
		return StreamSupport.stream(loader.spliterator(), false)
				.map(ContainerOrchestrationRuntimeDefaultExtensionsProvider::getDefaultExtension)
				.collect(Collectors.toList());
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId, CommandRunner commandRunner, Boolean shouldExposePorts,
			List<ContainerOrchestrationRuntimeExtension> extensions) {
		this.carnotzet = carnotzet;
		if (instanceId != null) {
			this.instanceId = instanceId;
		} else {
			this.instanceId = carnotzet.getTopLevelModuleName();
		}
		this.logManager = new DockerLogManager();
		this.commandRunner = commandRunner;
		this.shouldExposePorts = shouldExposePorts;
		this.extensions = extensions;

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
			String serviceId = module.getServiceId();

			serviceBuilder.image(module.getImageName());
			serviceBuilder.volumes(module.getDockerVolumes());
			serviceBuilder.entrypoint(escapeEnvVars(DockerUtils.parseEntrypointOrCmd(module.getDockerEntrypoint())));
			serviceBuilder.command(escapeEnvVars(DockerUtils.parseEntrypointOrCmd(module.getDockerCmd())));
			serviceBuilder.shm_size(module.getDockerShmSize());
			serviceBuilder.environment(module.getEnv());
			serviceBuilder.env_file(module.getDockerEnvFiles());
			if (shouldExposePorts) {
				serviceBuilder.ports(getExposedPorts(module.getImageName(), module.getProperties()));
			}

			Map<String, ContainerNetwork> networks = new HashMap<>();
			Set<String> networkAliases = new HashSet<>(lookUpCustomAliases(module));

			// Carnotzet semantics name
			networkAliases.add(module.getServiceId() + ".docker");
			networkAliases.add(instanceId + "." + module.getServiceId() + ".docker");

			// Legacy compat (default dnsdock pattern)
			if (carnotzet.getSupportLegacyDnsNames()) {
				networkAliases.add(module.getShortImageName() + ".docker");
				networkAliases.add(instanceId + "_" + module.getServiceId() + "." + module.getShortImageName() + ".docker");
			}

			ContainerNetwork network = ContainerNetwork.builder().aliases(networkAliases).build();
			networks.put(genNetworkName(), network);
			serviceBuilder.networks(networks);

			Map<String, String> labels = new HashMap<>();
			if (module.getLabels() != null) {
				labels.putAll(module.getLabels());
			}

			try {
				String hostname = InetAddress.getLocalHost().getHostName();
				labels.put("carnotzet.instance.source", hostname);
			}
			catch (UnknownHostException e) {
				log.error("Cannot determine hostname", e);
			}

			labels.put("com.dnsdock.alias", String.join(",", networkAliases));
			labels.put("carnotzet.instance.id", instanceId);
			labels.put("carnotzet.module.name", module.getName());
			labels.put("carnotzet.module.service.id", module.getServiceId());
			labels.put("carnotzet.top.level.module.name", carnotzet.getTopLevelModuleName());

			serviceBuilder.labels(labels);
			serviceBuilder.extra_hosts(lookUpExtraHosts(module));

			services.put(serviceId, serviceBuilder.build());
		}

		Network network = Network.builder().driver("bridge").build();
		Map<String, Network> networks = new HashMap<>();
		networks.put(genNetworkName(), network);

		DockerCompose compose = DockerCompose.builder().version("2").services(services).networks(networks).build();
		DockerComposeGenerator generator = new DockerComposeGenerator(compose);
		try {
			Files.write(
					carnotzet.getResourcesFolder().resolve("docker-compose.yml"),
					generator.generateDockerComposeFile().getBytes(StandardCharsets.UTF_8)
			);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Failed to write docker-compose.yml", e);
		}
		log.debug(String.format("End build compose file for module %s", carnotzet.getConfig().getTopLevelModuleId()));
	}

	/**
	 * If the entrypoint or command uses environment variables that are present inside the container, we don't want docker-compose to try to
	 * interpolate them when docker-compose is run (the docker-compose process is run outside of the container and those variables don't exist).
	 * <b>https://docs.docker.com/compose/compose-file/compose-file-v3/#variable-substitution</b>
	 */
	private List<String> escapeEnvVars(List<String> cmdOrEntryPoint) {
		if (cmdOrEntryPoint == null) {
			return null;
		}
		return cmdOrEntryPoint.stream().map(s -> s.replaceAll("\\$", "\\$\\$")).collect(Collectors.toList());
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
				.collect(toList());
	}

	private Set<String> lookUpExtraHosts(CarnotzetModule m) {
		Set<String> result = new HashSet<>();
		if (m.getProperties() != null && m.getProperties().containsKey("extra.hosts")) {
			result.addAll(Arrays.stream(m.getProperties().get("extra.hosts").split(","))
					.map(String::trim)
					.collect(toList()));
		}
		return result;
	}

	private void invokeAllExtensions(BiFunction<ContainerOrchestrationRuntimeExtension, CarnotzetModule, CarnotzetModule> consumer) {
		List<CarnotzetModule> modules = new ArrayList<>();
		for (CarnotzetModule m : carnotzet.getModules()) {
			CarnotzetModule modified = m;
			for (ContainerOrchestrationRuntimeExtension extension : extensions) {
				modified = consumer.apply(extension, modified);
			}
			modules.add(modified);
		}

		carnotzet.setModules(modules);
	}

	private void invokeAllExtensions(BiFunction<ContainerOrchestrationRuntimeExtension, CarnotzetModule, CarnotzetModule> consumer,
			CarnotzetModule module) {
		CarnotzetModule modified = module;
		for (ContainerOrchestrationRuntimeExtension extension : extensions) {
			modified = consumer.apply(extension, modified);
		}
		List<CarnotzetModule> modules = new ArrayList<>();
		for (CarnotzetModule m : carnotzet.getModules()) {
			if (m.getId().equals(modified.getId())) {
				modules.add(modified);
			} else {
				modules.add(m);
			}
		}
		carnotzet.setModules(modules);
	}

	@Override
	public void start() {
		Instant start = Instant.now();
		invokeAllExtensions((e, m) -> e.beforeStart(m, this, this.carnotzet));
		log.debug("Forcing update of docker-compose.yml before start");
		computeDockerComposeFile();
		carnotzet.getModules().stream().filter(this::shouldStartByDefault).forEach(m ->
				runCommand("docker-compose", "-p", getDockerComposeProjectName(), "up", "-d",
						"--scale", m.getServiceId() + "=" + m.getReplicas(), m.getServiceId())
		);
		ensureNetworkCommunicationIsPossible();
		logManager.ensureCapturingLogs(start, getContainers());
		invokeAllExtensions((e, m) -> e.afterStart(m, this, this.carnotzet));
	}

	private boolean shouldStartByDefault(CarnotzetModule m) {
		if (m.getImageName() == null) {
			return false;
		}
		if (m.getProperties() == null) {
			return true;
		}
		String str = m.getProperties().get("start.by.default");
		if (str == null) {
			return true;
		}
		if (str.trim().equalsIgnoreCase("false")) {
			return false;
		}
		return true;
	}

	@Override
	public void start(String services) {
		log.debug("Forcing update of docker-compose.yml before start");
		Set<CarnotzetModule> resolvedModules = resolveModules(services);

		for (CarnotzetModule carnotzetModule : resolvedModules) {
			invokeAllExtensions((e, m) -> e.beforeStart(m, this, this.carnotzet), carnotzetModule);
		}

		computeDockerComposeFile();

		for (CarnotzetModule carnotzetModule : resolvedModules) {
			String service = carnotzetModule.getServiceId();
			Instant start = Instant.now();
			runCommand("docker-compose", "-p", getDockerComposeProjectName(), "up", "-d", service);
			ensureNetworkCommunicationIsPossible();
			logManager.ensureCapturingLogs(start, Collections.singletonList(getContainer(service)));
			invokeAllExtensions((e, m) -> e.afterStart(m, this, this.carnotzet), carnotzetModule);
		}
	}

	private void ensureNetworkCommunicationIsPossible() {

		if (!IS_OS_LINUX) {
			return;
		}

		if (carnotzet.getAttachToCarnotzetNetwork()) {
			String buildContainerId =
					runCommandAndCaptureOutput("/bin/bash", "-c", "docker ps | grep $(hostname) | grep -v k8s_POD | cut -d ' ' -f 1");

			if (buildContainerId == null || buildContainerId.trim().isEmpty()) {
				// we are probably not running inside a container, networking should be fine
				return;
			}

			log.debug("Execution from inside a container detected! Attempting to configure container networking to allow communication.");
			log.debug("attaching container [" + buildContainerId + "] to network [" + getDockerNetworkName() + "]");
			runCommand("/bin/bash", "-c", "docker network connect " + getDockerNetworkName() + " " + buildContainerId);
		}
	}

	private String getDockerComposeProjectName() {
		return normalizeDockerComposeProjectName(instanceId);
	}

	private String getDockerNetworkName() {
		return getDockerComposeProjectName() + "_" + genNetworkName();
	}

	private String genNetworkName() {
		// We use only first 12 characters of sha256 (48-bit) which has collision chance 1 of out of 10M
		return "carnotzet_" + Sha256.getSHA(carnotzet.getResourcesFolder().toString()).substring(0, 12);
	}

	// normalize docker compose project name the same way docker-compose does (see https://github.com/docker/compose/tree/master/compose)
	private String normalizeDockerComposeProjectName(String moduleName) {
		return moduleName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
	}

	@Override
	public void stop() {
		invokeAllExtensions((e, m) -> e.beforeStop(m, this, this.carnotzet));
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "stop");
		invokeAllExtensions((e, m) -> e.afterStop(m, this, this.carnotzet));
	}

	@Override
	public void stop(String services) {
		for (CarnotzetModule carnotzetModule : resolveModules(services)) {
			String service = carnotzetModule.getServiceId();
			invokeAllExtensions((e, m) -> e.beforeStop(m, this, this.carnotzet), carnotzetModule);
			ensureDockerComposeFileIsPresent();
			runCommand("docker-compose", "-p", getDockerComposeProjectName(), "stop", service);
			invokeAllExtensions((e, m) -> e.afterStop(m, this, this.carnotzet), carnotzetModule);
		}
	}

	@Override
	public void status() {
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "ps");
	}

	@Override
	public void clean() {
		invokeAllExtensions((e, m) -> e.beforeClean(m, this, this.carnotzet));
		ensureDockerComposeFileIsPresent();
		runCommand("docker-compose", "-p", getDockerComposeProjectName(), "rm", "-f");
		// The resources folder cannot be deleted while the sandbox is running on windows.
		// So we do it here instead
		if (IS_OS_WINDOWS) {
			try {
				FileUtils.deleteDirectory(carnotzet.getResourcesFolder().toFile());
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		invokeAllExtensions((e, m) -> e.afterClean(m, this, this.carnotzet));
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
		invokeAllExtensions((e, m) -> e.beforePull(m, this, this.carnotzet));
		// We need to check service by service if a newer version exists or not
		carnotzet.getModules().forEach(module -> pull(module.getServiceId(), policy));
		invokeAllExtensions((e, m) -> e.afterPull(m, this, this.carnotzet));
	}

	@Override
	public void pull(@NonNull String services) {
		pull(services, PullPolicy.ALWAYS);
	}

	@Override
	public void pull(@NonNull String services, PullPolicy policy) {
		for (CarnotzetModule serviceModule : resolveModules(services)) {
			invokeAllExtensions((e, m) -> e.beforePull(m, this, this.carnotzet), serviceModule);
			DockerRegistry.pullImage(serviceModule, policy);
			invokeAllExtensions((e, m) -> e.afterPull(m, this, this.carnotzet), serviceModule);
		}
	}

	@Override
	public List<Container> getContainers() {
		String commandOutput = runCommandAndCaptureOutput("docker-compose", "-p", getDockerComposeProjectName(),
				"ps", "-q").replaceAll(System.lineSeparator(), " ");
		log.debug("docker-compose ps output : " + commandOutput);
		if (commandOutput.trim().isEmpty()) {
			return Collections.emptyList();
		}
		StringBuilder template = new StringBuilder("{{ index .Id}}:");
		if (IS_OS_WINDOWS) {
			template.append("{{ index .Config.Labels \\\"com.docker.compose.service\\\" }}:");
		} else {
			template.append("{{ index .Config.Labels \"com.docker.compose.service\" }}:");
		}
		template.append("{{ index .State.Running}}:");
		if (IS_OS_WINDOWS) {
			template.append("{{ index .Config.Labels \\\"com.docker.compose.container-number\\\" }}:");
		} else {
			template.append("{{ index .Config.Labels \"com.docker.compose.container-number\" }}:");
		}
		template.append("{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}:");
		List<String> args = new ArrayList<>(Arrays.asList("docker", "inspect", "-f", template.toString()));

		args.addAll(Arrays.asList(commandOutput.split(" ")));
		commandOutput = runCommandAndCaptureOutput(args.toArray(new String[0]));
		log.debug("docker inspect output : " + commandOutput);
		log.debug("split docker inspect output : " + Arrays.asList(commandOutput.split("\n")));
		return Arrays.stream(commandOutput.split("\n"))
				.filter(desc -> !desc.trim().isEmpty())
				.map(desc -> desc.split(":"))
				.map(parts -> new Container(parts[0], parts[1], parts[2].equals("true"), Integer.parseInt(parts[3]),
						parts.length > 4 ? parts[4] : null))
				.sorted(Comparator.comparing(Container::getServiceName).thenComparing(Container::getReplicaNumber))
				.collect(toList());

	}

	@Override
	public Container getContainer(String serviceName) {
		return getContainers().stream().filter(c -> c.getServiceName().equals(serviceName)).findFirst().orElse(null);
	}

	@Override
	public List<Container> getContainers(String serviceName) {
		return getContainers().stream().filter(c -> c.getServiceName().equals(serviceName)).collect(toList());
	}

	@Override
	public Container getContainer(String serviceName, int number) {
		return getContainers().stream().filter(c -> c.getServiceName().equals(serviceName) && c.getReplicaNumber() == number).findFirst()
				.orElse(null);
	}

	@Override
	public void registerLogListener(LogListener listener) {
		ensureDockerComposeFileIsPresent();
		logManager.registerLogListener(listener, getContainers());
	}

	@Override
	public ExecResult exec(String serviceName, int timeout, TimeUnit timeoutUnit, String... command) {
		Container container = getContainer(serviceName);
		List<String> fullCommand = new ArrayList<>(Arrays.asList("docker", "exec", container.getId()));
		fullCommand.addAll(Arrays.asList(command));
		try {
			ProcessResult pr = new ProcessExecutor()
					.command(fullCommand)
					.readOutput(true)
					.timeout(timeout, timeoutUnit)
					.execute();
			return new ExecResult(pr.getExitValue(), pr.getOutput().getUTF8());
		}
		catch (IOException | InterruptedException | TimeoutException e) {
			throw new RuntimeException("Failed to execute " + Arrays.toString(command) + " in container [" + serviceName + "]", e);
		}
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
		return Files.exists(carnotzet.getResourcesFolder().resolve("docker-compose.yml"));
	}

	private void ensureDockerComposeFileIsPresent() {
		if (dockerComposeFileExists()) {
			log.debug("Using existing docker-compose.yml");
			return;
		}
		log.debug("docker-compose.yml not found");
		computeDockerComposeFile();
	}

	public void clean(String services) {
		for (CarnotzetModule carnotzetModule : resolveModules(services)) {
			invokeAllExtensions((e, m) -> e.beforeClean(m, this, this.carnotzet), carnotzetModule);
			String service = carnotzetModule.getServiceId();
			runCommand("docker-compose", "-p", getDockerComposeProjectName(), "rm", "-f", service);
			invokeAllExtensions((e, m) -> e.afterClean(m, this, this.carnotzet), carnotzetModule);
		}
	}

	private final static Pattern PORT_PATTERN = Pattern.compile(".*?(\\d*\\/\\w*)");

	private Set<String> getExposedPorts(String dockerImage, Map<String, String> props) {

		Map<String, String> mapping = new HashMap<>();

		// lower priority
		pull(PullPolicy.IF_LOCAL_IMAGE_ABSENT);
		String output = commandRunner.runCommandAndCaptureOutput("docker", "inspect", "--format={{ .Config.ExposedPorts }}", dockerImage);
		Matcher m = PORT_PATTERN.matcher(output);

		while (m.find()) {
			mapping.put(m.group(1), null);
		}

		// static mappings defined in .properties file
		String customPorts = props.get("exposed.ports");
		if (customPorts != null && !customPorts.isEmpty()) {
			String[] ports = customPorts.split(",");
			for (String port : ports) {
				String[] parts = port.split(":");
				String containerPort = parts[1].trim();
				// Default
				if (!containerPort.contains("/")) {
					containerPort = containerPort + "/tcp";
				}
				if (!mapping.containsKey(containerPort)) {
					log.warn("Manually exposed.port [{}] in .properties file but image doesn't expose this port.", containerPort);
				}
				mapping.put(containerPort, parts[0].trim());
			}
		}

		Set<String> result = new HashSet<>();

		for (Map.Entry<String, String> entry : mapping.entrySet()) {
			if (entry.getValue() == null) {
				result.add(entry.getKey());
			} else {
				result.add(entry.getValue() + ":" + entry.getKey());
			}
		}

		return result;
	}

	private Set<CarnotzetModule> resolveModules(String services) {
		Set<CarnotzetModule> myModules = new HashSet<>();
		for (String service : services.split(",")) {
			for (CarnotzetModule module : carnotzet.getModules()) {
				if (module.getServiceId().matches(service)) {
					myModules.add(module);
				}
			}
		}
		if (myModules.isEmpty()) {
			throw new RuntimeException("service [" + services + "] not found");
		}
		return myModules;
	}

	@Override
	public String getInstanceId() {
		return instanceId;
	}

	@Override
	public List<ContainerOrchestrationRuntimeExtension> getRuntimeExtension() {
		return extensions;
	}
}
