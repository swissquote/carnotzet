package com.github.swissquote.carnotzet.runtime.docker.compose;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class DockerComposeGenerator {

	private final DockerCompose model;
	private final ObjectMapper mapper;

	public DockerComposeGenerator(DockerCompose model) {
		this.model = model;
		YAMLFactory factory = new YAMLFactory();
		mapper = new YAMLMapper(factory);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	public String generateDockerComposeFile() throws JsonProcessingException {
		return mapper.writeValueAsString(this.model);
	}
}
