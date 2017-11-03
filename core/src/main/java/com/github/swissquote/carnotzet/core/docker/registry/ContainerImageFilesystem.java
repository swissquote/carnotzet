package com.github.swissquote.carnotzet.core.docker.registry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContainerImageFilesystem {

	@JsonProperty("type")
	private String cmd;

	@JsonProperty("diff_ids")
	private List<String> entrypoint;
}
