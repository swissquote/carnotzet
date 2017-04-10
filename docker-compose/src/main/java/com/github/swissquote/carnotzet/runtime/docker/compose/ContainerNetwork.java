package com.github.swissquote.carnotzet.runtime.docker.compose;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@JsonDeserialize(builder = ContainerNetwork.ContainerNetworkBuilder.class)
@Value
@Builder
public class ContainerNetwork {

	@JsonProperty
	private final Set<String> aliases;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class ContainerNetworkBuilder {
	}

}
