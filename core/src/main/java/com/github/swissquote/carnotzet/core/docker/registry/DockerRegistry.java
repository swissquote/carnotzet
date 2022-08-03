package com.github.swissquote.carnotzet.core.docker.registry;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.runtime.DefaultCommandRunner;
import com.github.swissquote.carnotzet.core.runtime.api.PullPolicy;
import com.github.swissquote.carnotzet.core.util.FileSystemCache;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@RequiredArgsConstructor
@Slf4j
public class DockerRegistry {

	public static final DockerRegistry INSTANCE = new DockerRegistry();
	public static final String CARNOTZET_IMAGE_MANIFESTS_CACHE_FILENAME = ".carnotzet_image_manifests.cache";
	public static final String CARNOTZET_MANIFEST_DOWNLOAD_RETRIES = "manifest.download.retries.number.max";
	public static final String CARNOTZET_MANIFEST_RETRY_DELAY_SECONDS = "manifest.download.retries.delay.secs";

	private static final Map<ImageRef, ImageMetaData> IMAGE_META_DATA_CACHE = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private final DockerConfig config = DockerConfig.fromEnv();
	private final FileSystemCache<ContainerImageV1> imageManifestCache =
			new FileSystemCache<ContainerImageV1>(Paths.get(System.getProperty("user.home"), CARNOTZET_IMAGE_MANIFESTS_CACHE_FILENAME),
					ContainerImageV1.class);

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
			log.debug("Could not determine timestamp of local image [" + imageName + "], assuming it doesn't exist on the local docker host",
					e);
			return null;
		}
	}

	public ImageMetaData getImageMetaData(ImageRef imageRef) {
		return IMAGE_META_DATA_CACHE.computeIfAbsent(imageRef, ref -> {
			DistributionManifestV2 di = getDistributionManifest(ref);
			ContainerImageV1 im = getImageManifest(ref, di);
			return new ImageMetaData(di, im);
		});
	}

	private DistributionManifestV2 getDistributionManifest(ImageRef imageRef) {
		try {
			URL distributionManifestUrl =
					new URL(imageRef.getRegistryUrl() + "/v2/" + imageRef.getImageName() + "/manifests/" + imageRef.getTag());
			String responseBody = downloadWithoutRetry(distributionManifestUrl, "application/vnd.docker.distribution.manifest.v2+json",
					config.getAuthFor(imageRef.getRegistryName()));
			return objectMapper.readValue(responseBody, DistributionManifestV2.class);
		}
		catch (RuntimeException | JsonProcessingException | MalformedURLException e) {
			throw new CarnotzetDefinitionException("Could not fetch distribution manifest of [" + imageRef + "]", e);
		}
	}

	private String downloadWithRetry(URL url, String accept, String auth) throws IOException {
		RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
				.handle(Exception.class)
				.withDelay(Duration.ofSeconds(Integer.parseInt(System.getProperty(CARNOTZET_MANIFEST_RETRY_DELAY_SECONDS, "1"))))
				.withMaxRetries(Integer.parseInt(System.getProperty(CARNOTZET_MANIFEST_DOWNLOAD_RETRIES, "0")))
				.onRetry((o) -> log.info("Download attempt failed: {} : Retrying... ", o.getLastFailure().toString()))
				.onFailure((o) -> {
					log.error("Download failed: {} ", o.getFailure().toString());
					throw new IllegalStateException(o.getFailure());
				});
		return Failsafe.with(retryPolicy).get(() -> downloadWithoutRetry(url, accept, auth));
	}

	private String downloadWithoutRetry(URL url, String accept, String auth) {
		try {
			String oldValue = System.getProperty("java.net.useSystemProxies");
			System.setProperty("java.net.useSystemProxies", "true"); // default is false...
			Proxy proxy = ProxySelector.getDefault().select(url.toURI()).get(0);
			if (oldValue != null) {
				System.setProperty("java.net.useSystemProxies", oldValue);
			} else {
				System.getProperties().remove("java.net.useSystemProxies");
			}
			log.debug("Using proxy: [{}]", proxy);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
			try {
				connection.setRequestMethod("GET");
				connection.setRequestProperty("Accept", accept);
				if (auth != null) {
					connection.setRequestProperty("Authorization", "Basic " + auth);
				}
				connection.setConnectTimeout(1000);
				connection.setReadTimeout(5000);
				connection.connect();
				int responseCode = connection.getResponseCode();
				String responseBody = readInputStreamToString(connection);
				if (responseCode < 200 || responseCode >= 400) {
					throw new CarnotzetDefinitionException("Received response code [" + responseCode + "] with body: [" + responseBody + "]");
				}
				log.debug("Received response code [{}] from [{}]", responseCode, url);
				return responseBody;
			}
			finally {
				connection.disconnect();
			}
		}
		catch (IOException | URISyntaxException e) {
			throw new CarnotzetDefinitionException("Could not download from [" + url + "]", e);
		}

	}

	private String readInputStreamToString(@NonNull HttpURLConnection connection) throws IOException {
		InputStream inputStream = connection.getInputStream();
		InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(reader);
		try {
			return bufferedReader.lines().collect(joining("\n"));
		}
		finally {
			bufferedReader.close();
			reader.close();
			inputStream.close();
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
		try {
			URL imageManifestUrl = new URL(imageRef.getRegistryUrl() + "/v2/" + imageRef.getImageName() + "/blobs/" + digest);
			log.info("Downloading image manifest from {} ...", imageManifestUrl);
			String result = downloadWithRetry(imageManifestUrl, "application/vnd.docker.container.image.v1+json",
					config.getAuthFor(imageRef.getRegistryName()));
			log.info("Image manifest downloaded");
			return result;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
