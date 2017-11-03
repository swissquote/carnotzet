package com.github.swissquote.carnotzet.core.docker.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistributionManifestConfig {
	private String digest;
}
