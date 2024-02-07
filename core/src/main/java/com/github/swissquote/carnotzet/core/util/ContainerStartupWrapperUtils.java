package com.github.swissquote.carnotzet.core.util;

import static com.github.swissquote.carnotzet.core.util.ContainerStartupWrapperUtils.DockerExecutionItem.CMD;
import static com.github.swissquote.carnotzet.core.util.ContainerStartupWrapperUtils.DockerExecutionItem.ENTRYPOINT;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.docker.DockerUtils;
import com.github.swissquote.carnotzet.core.docker.registry.DockerRegistry;
import com.github.swissquote.carnotzet.core.docker.registry.ImageMetaData;
import com.github.swissquote.carnotzet.core.docker.registry.ImageRef;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * Creates a wrapper script to allow special initialization of applications.<br>
 * It re-uses the ENTRYPOINT and CMD of the module (defined either in carnotzet.properties, local image or registry image) <br>
 * The wrapper script is written to disk and mounted in the containers as a volume. <br>
 * The ENTRYPOINT and CMD of the module are overridden to invoke the mounter wrapper script. <br>
 */
public final class ContainerStartupWrapperUtils {

	private ContainerStartupWrapperUtils() {
	}

	public static StartupWrapper wrap(CarnotzetModule module) {
		return new StartupWrapper(module);
	}

	public static String templateWrapperScript(String before) {
		return "#!/bin/sh\n"
				+ before + "\n"
				+ "exec \"$@\"\n"; // wrapper will be prepended as the first entrypoint item
	}

	// Returns the entrypoint/cmd in Exec format (json array)
	// Returns null if there is no entrypoint/cmd for the image
	// throws a CarnotzetDefinitionException if the docker daemon returned an error (ie: the image could not be found)
	public static String getFromLocalImage(String image, DockerExecutionItem type) {
		try {
			String inspectOutput = DefaultCommandRunner.INSTANCE.runCommandAndCaptureOutput(
					"docker", "inspect", "-f", "{{range .Config." + type.getJsonField() + "}}{{.}}||{{end}}", image).trim();
			if (inspectOutput.isEmpty()) {
				return null;
			}
			return DockerUtils.formatExecEntrypointOrCmd(asList(inspectOutput.split("\\|\\|")));
		}
		catch (RuntimeException e) {
			throw new CarnotzetDefinitionException("Could not get Entrypoint for [" + image + "] on the docker host", e);
		}
	}

	// Returns the entrypoint/cmd in Exec format (json array)
	// Returns null if there is no entrypoint/cmd for the image
	// throws a CarnotzetDefinitionException if the image could not be found
	public static String getFromRegistry(String image, DockerExecutionItem type) {
		try {
			ImageMetaData metadata = DockerRegistry.INSTANCE.getImageMetaData(new ImageRef(image));
			switch (type) {
				case CMD:
					return DockerUtils.formatExecEntrypointOrCmd(metadata.getContainerImage().getConfig().getCmd());
				case ENTRYPOINT:
					return DockerUtils.formatExecEntrypointOrCmd(metadata.getContainerImage().getConfig().getEntrypoint());
				default:
					throw new CarnotzetDefinitionException("Unknown docker execution item type [" + type + "]");
			}
		}
		catch (Exception e) {
			throw new CarnotzetDefinitionException("Could not get Entrypoint for [" + image + "] on the remote registry", e);
		}
	}

	// Returns the entrypoint/cmd in Exec format (json array)
	// Returns null if there is no entrypoint/cmd for the image
	// throws a CarnotzetDefinitionException if the image could not be found
	public static String getImageExecutionItem(CarnotzetModule moduleToWrap, Boolean ignoreLocalImages, DockerExecutionItem type) {

		String fromProperties = getFromModuleProperties(moduleToWrap, type);

		// Highest priority : overrides in carnotzet.properties
		if (fromProperties != null) {
			return DockerUtils.formatExecEntrypointOrCmd(DockerUtils.parseEntrypointOrCmd(fromProperties));
		}

		// Try local image registry
		if (!ignoreLocalImages) {
			try {
				return getFromLocalImage(moduleToWrap.getImageName(), type);
			}
			catch (CarnotzetDefinitionException e) {
				log.debug("Failed to get local image [" + moduleToWrap.getImageName() + "] entrypoint", e);
			}
		}

		// last resort, try the remote registry
		try {
			return getFromRegistry(moduleToWrap.getImageName(), type);
		}
		catch (RuntimeException e) {
			throw new CarnotzetDefinitionException("Could not find image [" + moduleToWrap.getImageName() + "] anywhere", e);
		}
	}

	public static String getFromModuleProperties(CarnotzetModule moduleToWrap, DockerExecutionItem type) {
		switch (type) {
			case ENTRYPOINT:
				return moduleToWrap.getDockerEntrypoint();
			case CMD:
				return moduleToWrap.getDockerCmd();
			default:
				throw new CarnotzetDefinitionException("Unknown Docker execution item type [" + type + "]");
		}
	}

	public static String wrapEntrypoint(CarnotzetModule moduleToWrap, String wrapperCommand, Boolean ignoreLocalImages) {
		List<String> res = new ArrayList<>();
		String existingEntrypoint = getImageExecutionItem(moduleToWrap, ignoreLocalImages, ENTRYPOINT);
		if (existingEntrypoint != null) {
			res.addAll(DockerUtils.parseEntrypointOrCmd(existingEntrypoint));
		}
		res.add(0, wrapperCommand);
		return DockerUtils.formatExecEntrypointOrCmd(res);
	}

	public static class StartupWrapper {

		private final CarnotzetModule moduleToWrap;
		private String startingScript = "";
		private Path resourceFolder;
		private String wrapperName = "wrapper";

		// Local images may not be available for target runtime environment (ie : cloud)
		private Boolean ignoreLocalImages = false;

		@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
		public StartupWrapper(@NonNull CarnotzetModule moduleToWrap) {
			this.moduleToWrap = moduleToWrap;
		}

		public StartupWrapper withStartingScriptContent(@NonNull String startingScript) {
			this.startingScript = startingScript;
			return this;
		}

		public StartupWrapper inResourceFolder(@NonNull Path resourceFolder) {
			this.resourceFolder = resourceFolder;
			return this;
		}

		public StartupWrapper usingWrapperName(@NonNull String wrapperName) {
			this.wrapperName = wrapperName;
			return this;
		}

		public CarnotzetModule build() {
			if (resourceFolder == null) {
				throw new IllegalArgumentException("resourceFolder cannot be null");
			}

			Set<String> allVolumes = new HashSet<>(moduleToWrap.getDockerVolumes());
			String scriptName = wrapperName + "_" + moduleToWrap.getServiceId() + ".sh";
			try {
				Path wrappersDir = resourceFolder.resolve("startup-wrappers");
				Files.createDirectories(wrappersDir);
				Path wrapperScript = wrappersDir.resolve(scriptName).toAbsolutePath();
				Files.write(wrapperScript, templateWrapperScript(startingScript).getBytes(StandardCharsets.UTF_8));
				if (!wrapperScript.toFile().setExecutable(true, false)) {
					throw new IOException("Could not make file executable : " + wrapperScript.toAbsolutePath());
				}
				allVolumes.add(wrapperScript.toString() + ":/" + scriptName);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			log.debug("[{}] Wrapper script generated: {}", wrapperName, scriptName);

			String entryPoint = wrapEntrypoint(moduleToWrap, "/" + scriptName, ignoreLocalImages);

			// wrapping the entrypoint deletes the CMD : https://github.com/docker/compose/issues/3140
			String cmd = getImageExecutionItem(moduleToWrap, ignoreLocalImages, CMD);

			return moduleToWrap.toBuilder()
					.dockerVolumes(allVolumes)
					.dockerEntrypoint(entryPoint)
					.dockerCmd(cmd)
					.build();
		}

	}

	public enum DockerExecutionItem {

		ENTRYPOINT("Entrypoint"), CMD("Cmd");

		@Getter
		private String jsonField;

		DockerExecutionItem(String fieldName) {
			this.jsonField = fieldName;
		}

	}

}