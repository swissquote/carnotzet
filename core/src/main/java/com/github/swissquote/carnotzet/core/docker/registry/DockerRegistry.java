package com.github.swissquote.carnotzet.core.docker.registry;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.cfg.Annotations;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.api.PullPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DockerRegistry {

	public static final DockerRegistry INSTANCE = new DockerRegistry();

	private final DockerConfig config = DockerConfig.fromEnv();
	private final Map<String, WebTarget> webTargets = new HashMap<>();

	public static void pullImage(CarnotzetModule module, PullPolicy policy) {

		String imageName = module.getImageName();
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
		if (policy.shouldPullImage(module, localTimestamp, registryImageMetadata)) {
			DefaultCommandRunner.INSTANCE.runCommand("docker", "pull", imageName);
		}
	}

	// Return null if the image is not found on the remote registry
	private static ImageMetaData getRegistryImageMetadata(String imageName) {
		// Call docker registry to ask for image details.
		try {
			return DockerRegistry.INSTANCE.getImageMetaData(new ImageRef(imageName));
		}
		catch (CarnotzetDefinitionException cde) {
			log.debug("Could not determine metadata of registry image [" + imageName + "]", cde);
			return null;
		}
	}

	// returns null if the image doesn't exist on the docker host
	private static Instant getLocalImageTimestamp(String imageName) {
		// Use docker inspect
		try {
			String isoDatetime = DefaultCommandRunner.INSTANCE.runCommandAndCaptureOutput("docker", "inspect", "-f", "{{.Created}}", imageName);
			return Instant.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(isoDatetime));
		}
		catch (RuntimeException e) {
			log.debug("Could not determine timestamp of local image [" + imageName + "], assuming it doesn't exist on the local docker host", e);
			return null;
		}
	}

	public ImageMetaData getImageMetaData(ImageRef imageRef) {
		DistributionManifestV2 di = getDistributionManifest(imageRef);
		ContainerImageV1 im = getImageManifest(imageRef, di);
		return new ImageMetaData(di, im);
	}

	private DistributionManifestV2 getDistributionManifest(ImageRef imageRef) {
		try {
			WebTarget registry = getRegistryWebTarget(imageRef);

			return registry.path("v2/{name}/manifests/{reference}")
					.resolveTemplate("name", imageRef.getImageName(), false)
					.resolveTemplate("reference", imageRef.getTag(), false)
					.request("application/vnd.docker.distribution.manifest.v2+json")
					.get(DistributionManifestV2.class);
		}
		catch (Exception e) {
			throw new CarnotzetDefinitionException("Could not fetch distribution manifest of [" + imageRef + "]", e);
		}
	}

	private ContainerImageV1 getImageManifest(ImageRef imageRef, DistributionManifestV2 distributionManifest) {
		if (distributionManifest.getConfig() == null || distributionManifest.getConfig().getDigest() == null) {
			throw new CarnotzetDefinitionException("Distribution manifest of images [" + imageRef + " does not contain digest of image");
		}

		try {

			Properties imageManifestCache = new Properties();
			Path imageManifestCachePath = Paths.get(System.getProperty("user.home"), ".image_manifests.cache");
			if (Files.notExists(imageManifestCachePath)) {
				Files.createFile(imageManifestCachePath);
			}
			imageManifestCache.load(new FileInputStream(imageManifestCachePath.toFile()));

			String imageManifestJSON = (String) imageManifestCache.get(distributionManifest.getConfig().getDigest());
			if (imageManifestJSON == null) {
				WebTarget registry = getRegistryWebTarget(imageRef);
				imageManifestJSON = registry.path("v2/{name}/manifests/{reference}")
						.resolveTemplate("name", imageRef.getImageName(), false)
						.resolveTemplate("reference", distributionManifest.getConfig().getDigest(), false)
						.request("application/vnd.docker.container.image.v1+json")
						.get(String.class);
				imageManifestCache.put(distributionManifest.getConfig().getDigest(), imageManifestJSON);
				imageManifestCache.store(new FileWriter(imageManifestCachePath.toFile()), "image manifest cache");
			}

			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			ContainerImageV1 imageManifest = mapper.readValue(imageManifestJSON, ContainerImageV1.class);
			return imageManifest;

		}
		catch (Exception e) {
			throw new CarnotzetDefinitionException("Could not fetch config manifest of image [" + imageRef + "]", e);
		}
	}

	private WebTarget getRegistryWebTarget(ImageRef imageRef) {
		if (!webTargets.containsKey(imageRef.getRegistryUrl())) {

			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());

			// TODO : This client doesn't handle mandatory Oauth2 Bearer token imposed by some registries implementations (ie : docker hub)
			Client client = ClientBuilder.newClient()
					.register(new JacksonJaxbJsonProvider(mapper, new Annotations[] {Annotations.JACKSON}))
					.register(JacksonFeature.class);
			String auth = config.getAuthFor(imageRef.getRegistryName());
			if (auth != null) {
				String[] credentials = new String(Base64.getDecoder().decode(auth), StandardCharsets.UTF_8).split(":");
				client.register(HttpAuthenticationFeature.basicBuilder().credentials(credentials[0], credentials[1]));
			}
			WebTarget webTarget = client.target(imageRef.getRegistryUrl());
			webTargets.put(imageRef.getRegistryUrl(), webTarget);
		}
		return webTargets.get(imageRef.getRegistryUrl());
	}

}
