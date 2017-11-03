package com.github.swissquote.carnotzet.core.docker.registry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistributionManifestV2 {
	@JsonProperty("config")
	private DistributionManifestConfig config;

	@JsonProperty("schemaVersion")
	private Integer schemaVersion;

	@JsonProperty("mediaType")
	private String mediaType;

	@JsonProperty("layers")
	private List<DistributionManifestLayer> layers;
}
