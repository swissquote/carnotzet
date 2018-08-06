package com.github.swissquote.carnotzet.runtime.docker.compose;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.docker.DockerUtils;
import com.github.swissquote.carnotzet.core.docker.registry.DockerRegistry;
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

	private final Boolean shouldExposePorts;

	public DockerComposeRuntime(Carnotzet carnotzet) {
		this(carnotzet, carnotzet.getTopLevelModuleName());
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId, CommandRunner commandRunner) {
		// Due to limitations in docker for mac and windows, mapping local ports to container ports is the preferred technique for those users.
		// https://docs.docker.com/docker-for-mac/networking/#i-cannot-ping-my-containers
		this(carnotzet, instanceId, commandRunner, SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS);
	}

	public DockerComposeRuntime(Carnotzet carnotzet, String instanceId, CommandRunner commandRunner, Boolean shouldExposePorts) {
		this.carnotzet = carnotzet;
		if (instanceId != null) {
			this.instanceId = instanceId;
		} else {
			this.instanceId = carnotzet.getTopLevelModuleName();
		}
		this.logManager = new DockerLogManager();
		this.commandRunner = commandRunner;
		this.shouldExposePorts = shouldExposePorts;
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
			serviceBuilder.entrypoint(DockerUtils.parseEntrypointOrCmd(module.getDockerEntrypoint()));
			serviceBuilder.command(DockerUtils.parseEntrypointOrCmd(module.getDockerCmd()));
			serviceBuilder.environment(module.getEnv());
			serviceBuilder.env_file(module.getDockerEnvFiles());
			if (shouldExposePorts) {
				serviceBuilder.ports(getExposedPorts(module.getImageName(), module.getProperties()));
			}

			Map<String, ContainerNetwork> networks = new HashMap<>();
			Set<String> networkAliases = new HashSet<>();
			networkAliases.addAll(lookUpCustomAliases(module));

			// Carnotzet semantics name
			networkAliases.add(module.getServiceId() + ".docker");
			networkAliases.add(instanceId + "." + module.getServiceId() + ".docker");

			// Legacy compat (default dnsdock pattern)
			networkAliases.add(module.getShortImageName() + ".docker");
			networkAliases.add(instanceId + "_" + module.getServiceId() + "." + module.getShortImageName() + ".docker");

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
			labels.put("carnotzet.module.service.id", module.getServiceId());
			labels.put("carnotzet.top.level.module.name", carnotzet.getTopLevelModuleName());

			serviceBuilder.labels(labels);
			serviceBuilder.extra_hosts(lookUpExtraHosts(module));

			services.put(serviceId, serviceBuilder.build());
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

	@Override
	public void start() {
		log.debug("Forcing update of docker-compose.yml before start");
		computeDockerComposeFile();
		Instant start = Instant.now();
		carnotzet.getModules().stream().filter(this::shouldStartByDefault).forEach(m ->
				runCommand("docker-compose", "-p", getDockerComposeProjectName(), "up", "-d", m.getServiceId())
		);
		ensureNetworkCommunicationIsPossible();
		logManager.ensureCapturingLogs(start, getContainers());
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
		if (str.trim().toLowerCase().equals("false")) {
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

		if (!SystemUtils.IS_OS_LINUX) {
			return;
		}

		String buildContainerId =
				runCommandAndCaptureOutput("/bin/bash", "-c", "docker ps | grep $(hostname) | grep -v k8s_POD | cut -d ' ' -f 1");

		if (Strings.isNullOrEmpty(buildContainerId)) {
			// we are probably not running inside a container, networking should be fine
			return;
		}

		log.debug("Execution from inside a container detected! Attempting to configure container networking to allow communication.");

		String networkMode =
				runCommandAndCaptureOutput("/bin/bash", "-c", "docker inspect -f '{{.HostConfig.NetworkMode}}' " + buildContainerId);

		String containerToConnect = buildContainerId;

		// shared network stack
		if (networkMode.startsWith("container:")) {
			containerToConnect = networkMode.replace("container:", "");
			log.debug("Detected a shared container network stack.");

			String parentNetworkMode =
					runCommandAndCaptureOutput("/bin/bash", "-c", "docker inspect -f '{{.HostConfig.NetworkMode}}' " + containerToConnect);

			if (parentNetworkMode.equals("none")) {
				Container dnsContainer = getContainer("carnotzet-dns");
				if (dnsContainer == null) {
					log.warn("Infrastructure container has NetworkMode [none] and there is no [carnotzet-dns] service in the environment, "
							+ "name resolution of [*.docker] will not work from this container");
					return;
				}
				log.debug("Adding nameserver [{}] to the container's /etc/resolv.conf", dnsContainer.getIp());
				RandomAccessFile f = null;
				try {
					f = new RandomAccessFile(new File("/etc/resolv.conf"), "rw");
					f.seek(0); // to the beginning
					f.write(("nameserver " + dnsContainer.getIp()).getBytes());
					f.close();
				}
				catch (IOException e) {
					log.warn("Failed to add nameserver to /etc/resolv.conf, name resolution of [*.docker] will not work from this container", e);
				}

				return;
			}

		}

		log.debug("attaching container [" + containerToConnect + "] to network [" + getDockerNetworkName() + "]");
		runCommand("/bin/bash", "-c", "docker network connect " + getDockerNetworkName() + " " + containerToConnect);

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
		// The resources folder cannot be deleted while the sandbox is running on windows.
		// So we do it here instead
		if (SystemUtils.IS_OS_WINDOWS) {
			try {
				FileUtils.deleteDirectory(carnotzet.getResourcesFolder().toFile());
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
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
		carnotzet.getModules().forEach(module -> pull(module.getServiceId(), policy));
	}

	@Override
	public void pull(@NonNull String service) {
		pull(service, PullPolicy.ALWAYS);
	}

	@Override
	public void pull(@NonNull String service, PullPolicy policy) {
		// Find out the name and tag of the image we are trying to pull
		CarnotzetModule serviceModule = carnotzet.getModuleByServiceId(service)
				.orElseThrow(() -> new IllegalArgumentException("No such service: " + service));

		DockerRegistry.pullImage(serviceModule, policy);
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
		if (SystemUtils.IS_OS_WINDOWS) {
			template.append("{{ index .Config.Labels \\\"com.docker.compose.service\\\" }}:");
		} else {
			template.append("{{ index .Config.Labels \"com.docker.compose.service\" }}:");
		}
		template.append("{{ index .State.Running}}:");
		template.append("{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}:");
		List<String> args = new ArrayList<>(Arrays.asList("docker", "inspect", "-f", template.toString()));

		args.addAll(Arrays.asList(commandOutput.split(" ")));
		commandOutput = runCommandAndCaptureOutput(args.toArray(new String[args.size()]));
		log.debug("docker inspect output : " + commandOutput);
		log.debug("split docker inspect output : " + Arrays.asList(commandOutput.split("\n")));
		return Arrays.stream(commandOutput.split("\n"))
				.filter(desc -> !desc.trim().isEmpty())
				.map(desc -> desc.split(":"))
				.map(parts -> new Container(parts[0], parts[1], parts[2].equals("true"), parts.length > 3 ? parts[3] : null))
				.sorted(Comparator.comparing(Container::getServiceName))
				.collect(toList());

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
}
