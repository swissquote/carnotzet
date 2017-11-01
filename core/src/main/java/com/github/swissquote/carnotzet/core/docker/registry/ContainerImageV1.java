package com.github.swissquote.carnotzet.core.docker.registry;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContainerImageV1 {

	@JsonProperty("architecture")
	private String architecture;

	@JsonProperty("config")
	private ContainerImageConfig config;

	@JsonProperty("container")
	private String container;

	@JsonProperty("container_config")
	private ContainerImageConfig containerConfig;

	@JsonProperty("created")
	private Instant created;

	@JsonProperty("docker_version")
	private String dockerVersion;

	@JsonProperty("history")
	private List<ContainerImageHistoryItem> history;

	@JsonProperty("os")
	private String os;

	@JsonProperty("rootfs")
	private ContainerImageFilesystem rootfs;
}
