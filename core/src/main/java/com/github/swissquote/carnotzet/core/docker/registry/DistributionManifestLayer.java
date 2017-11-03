package com.github.swissquote.carnotzet.core.docker.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistributionManifestLayer {
	@JsonProperty("mediaType")
	private String mediaType;

	@JsonProperty("size")
	private Long size;

	@JsonProperty("digest")
	private String digest;
}
