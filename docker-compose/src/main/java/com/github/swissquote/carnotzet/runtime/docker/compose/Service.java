package com.github.swissquote.carnotzet.runtime.docker.compose;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = Service.ServiceBuilder.class)
@Value
@Builder
public class Service {

	@JsonProperty
	private final String image;
	@JsonProperty
	private final Set<String> volumes;
	//CHECKSTYLE:OFF
	@JsonProperty
	private final Set<String> env_file;
	//CHECKSTYLE:ON
	@JsonProperty
	private final Map<String, String> environment;
	@JsonProperty
	private final List<String> entrypoint;
	@JsonProperty
	private final Map<String, String> labels;
	@JsonProperty
	private final List<String> command;
	@JsonProperty
	private final Map<String, ContainerNetwork> networks;
	@JsonProperty
	private final Set<String> expose;
	@JsonProperty
	private final Set<String> ports;
	@JsonProperty
	private final String user;
	@JsonProperty
	//CHECKSTYLE:OFF
	private final String shm_size;
	//CHECKSTYLE:ON
	@JsonProperty
	//CHECKSTYLE:OFF
	private final Set<String> extra_hosts;
	//CHECKSTYLE:ON
	@JsonProperty
	//CHECKSTYLE:OFF
	private final Integer pids_limit;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class ServiceBuilder {
	}

}
