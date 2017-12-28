package com.github.swissquote.carnotzet.core.docker.registry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * For json deserialization
 */
@Slf4j
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerConfig {

	private Map<String, Auth> auths = new HashMap<>();

	public String getAuthFor(String registry) {
		Auth auth = auths.get(registry);
		if (auth == null) {
			auth = auths.get("https://" + registry);
		}
		if (auth == null) {
			return null;
		}
		return auth.getAuth();
	}

	public static DockerConfig fromEnv() {
		Path configJson = Paths.get(System.getProperty("user.home")).resolve(".docker/config.json");

		if (!configJson.toFile().exists()) {
			log.warn("No registry auth found in [{}]", configJson);
			return new DockerConfig();
		}

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(Files.readAllBytes(configJson), DockerConfig.class);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
