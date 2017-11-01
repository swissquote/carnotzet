package com.github.swissquote.carnotzet.core.docker.registry;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContainerImageConfig {

	@JsonProperty("Hostname")
	private String hostname;

	@JsonProperty("Domainname")
	private String domainname;

	@JsonProperty("User")
	private String user;

	@JsonProperty("AttachStdin")
	private Boolean attachStdin;

	@JsonProperty("AttachStdout")
	private Boolean attachStdout;

	@JsonProperty("AttachStderr")
	private Boolean attachStderr;

	@JsonProperty("ExposedPorts")
	private Map<String, EmptyObject> exposedPorts;

	@JsonProperty("Tty")
	private Boolean tty;

	@JsonProperty("OpenStdin")
	private Boolean openStdin;

	@JsonProperty("StdinOnce")
	private Boolean stdinOnce;

	@JsonProperty("Env")
	private List<String> env;

	@JsonProperty("Cmd")
	private List<String> cmd;

	@JsonProperty("Healthcheck")
	private ContainerImageHealthcheck healthcheck;

	@JsonProperty("ArgsEscaped")
	private Boolean argsEscaped;

	@JsonProperty("Image")
	private String image;

	@JsonProperty("WorkingDir")
	private String workingDir;

	@JsonProperty("Entrypoint")
	private List<String> entrypoint;

	@JsonProperty("Labels")
	private Map<String, String> labels;
}
