package com.github.swissquote.carnotzet.core.docker.registry;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.api.PullPolicy;
import com.github.swissquote.carnotzet.core.util.FileSystemCache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

//import javax.ws.rs.NotFoundException;

@RequiredArgsConstructor
@Slf4j
public class DockerRegistry {

	public static final DockerRegistry INSTANCE = new DockerRegistry();
	public static final String CARNOTZET_IMAGE_MANIFESTS_CACHE_FILENAME = ".carnotzet_image_manifests.cache";
	public static final String CARNOTZET_MANIFEST_DOWNLOAD_RETRIES = "manifest.download.retries.number.max";
	public static final String CARNOTZET_MANIFEST_RETRY_DELAY_SECONDS = "manifest.download.retries.delay.secs";

	private final DockerConfig config = DockerConfig.fromEnv();
	private final Map<String, WebTarget> webTargets = new HashMap<>();
	private final FileSystemCache<ContainerImageV1> imageManifestCache =
			new FileSystemCache<ContainerImageV1>(Paths.get(System.getProperty("user.home"), CARNOTZET_IMAGE_MANIFESTS_CACHE_FILENAME),
					ContainerImageV1.class);

	static {
		ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
		RegisterBuiltin.register(instance);
		instance.registerProvider(ResteasyJackson2Provider.class);
	}

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
			return imageManifestCache.computeIfAbsent(distributionManifest.getConfig().getDigest(), digest ->
					downloadImageManifestAsString(digest, imageRef));
		}
		catch (Exception e) {
			throw new CarnotzetDefinitionException("Could not fetch config manifest of image [" + imageRef + "]", e);
		}
	}

	private String downloadImageManifestAsString(String digest, ImageRef imageRef) {
		WebTarget registry = getRegistryWebTarget(imageRef);
		WebTarget url = registry.path("v2/{name}/blobs/{reference}")
				.resolveTemplate("name", imageRef.getImageName(), false)
				.resolveTemplate("reference", digest, false);
		log.info("Downloading image manifest from {} ...", url.getUri().toString());

		RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
				.handle(WebApplicationException.class)
				.withDelay(Duration.ofSeconds(Integer.parseInt(System.getProperty(CARNOTZET_MANIFEST_RETRY_DELAY_SECONDS, "1"))))
				.withMaxRetries(Integer.parseInt(System.getProperty(CARNOTZET_MANIFEST_DOWNLOAD_RETRIES, "0")))
				.onRetry((o) -> log.info("Download attempt failed: {} : Retrying... ", o.getLastFailure().toString()))
				.onFailure((o) -> {
					log.error("Download failed: {} ", o.getFailure().toString());
					throw new IllegalStateException(o.getFailure());
				});
		String value = Failsafe.with(retryPolicy).get(() ->
				url.request("application/vnd.docker.container.image.v1+json").get(String.class)
		);

		log.info("Image manifest downloaded");
		return value;
	}

	private WebTarget getRegistryWebTarget(ImageRef imageRef) {
		if (!webTargets.containsKey(imageRef.getRegistryUrl())) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			// TODO : This client doesn't handle mandatory Oauth2 Bearer token imposed by some registries implementations (ie : docker hub)
			ResteasyClient client = new ResteasyClientBuilder().build();
			String auth = config.getAuthFor(imageRef.getRegistryName());
			String[] credentials = new String(Base64.getDecoder().decode(auth), StandardCharsets.UTF_8).split(":");
			WebTarget webTarget = client.target(imageRef.getRegistryUrl());
			webTarget.register(new BasicAuthentication(credentials[0], credentials[1]));
			webTargets.put(imageRef.getRegistryUrl(), webTarget);
		}
		return webTargets.get(imageRef.getRegistryUrl());
	}

}
