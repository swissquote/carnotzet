package com.github.swissquote.carnotzet.runtime.docker.compose;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@JsonDeserialize(builder = DockerCompose.DockerComposeBuilder.class)
@Value
@Builder
public class DockerCompose {
	@JsonProperty
	private final String version;

	@JsonProperty
	private final Map<String, Service> services;

	@JsonProperty
	private final Map<String, Network> networks;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class DockerComposeBuilder {
	}
}
