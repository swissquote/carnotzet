package com.github.swissquote.carnotzet.runtime.docker.compose;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

@JsonDeserialize(builder = Network.NetworkBuilder.class)
@Value
@Builder
public class Network {

	@JsonProperty
	private final String driver;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class NetworkBuilder {
	}

}
