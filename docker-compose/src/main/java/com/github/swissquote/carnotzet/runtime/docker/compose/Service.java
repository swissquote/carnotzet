package com.github.swissquote.carnotzet.runtime.docker.compose;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.Map;
import java.util.Set;

@JsonDeserialize(builder = Service.ServiceBuilder.class)
@Value
@Builder
public class Service {

	@JsonProperty
	private final String image;
	@JsonProperty
	private final Set<String> volumes;
	@JsonProperty
	private final Set<String> env_file;
//	@JsonProperty
//	private final String container_name;
	@JsonProperty
	private final String entrypoint;
	@JsonProperty
	private final String command;
	@JsonProperty
	private final Map<String, ContainerNetwork> networks;
	@JsonProperty
	private final Set<String> expose;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class ServiceBuilder {
	}

}
