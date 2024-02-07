package com.github.swissquote.carnotzet.runtime.docker.compose;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = Network.NetworkBuilder.class)
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Network {

	@JsonProperty
	private final String driver;

	@JsonProperty
	private final boolean external;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class NetworkBuilder {
	}

}
