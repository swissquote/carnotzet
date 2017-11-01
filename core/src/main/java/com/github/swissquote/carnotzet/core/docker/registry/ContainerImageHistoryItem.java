package com.github.swissquote.carnotzet.core.docker.registry;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContainerImageHistoryItem {
	@JsonProperty("created")
	private Instant created;

	@JsonProperty("created_by")
	private String createdBy;

	@JsonProperty(value = "empty_layer")
	private boolean emptyLayer; // default value is false in the Json schema. As it happens, it is the same in java
}
