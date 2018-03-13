package com.github.swissquote.carnotzet.core.docker;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DockerUtils {
	private DockerUtils() {

	}

	private final static ObjectMapper MAPPER = new ObjectMapper();

	// Parses a string exec format for ENTRYPOINT or CMD (json array representation)
	public static List<String> parseEntrypointOrCmd(String value) {
		if (value == null) {
			return null;
		}
		try {
			List<String> result = MAPPER.readValue(value.trim(), new TypeReference<List<String>>() {
			});
			return result.isEmpty() ? null : result;
		}
		catch (IOException e) {
			return Arrays.asList("/bin/sh", "-c", value);
		}
	}

	public static String formatExecEntrypointOrCmd(List<String> command) {
		if (command == null || command.isEmpty()) {
			return null;
		}
		try {
			return MAPPER.writeValueAsString(command);
		}
		catch (JsonProcessingException e) {
			throw new CarnotzetDefinitionException(e);
		}
	}

	public static boolean isShellFormat(String entrypoint) {
		return !entrypoint.trim().startsWith("[");
	}
}
