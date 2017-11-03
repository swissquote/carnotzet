package com.github.swissquote.carnotzet.core.docker.registry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmptyObject {
	/**
	 * From https://github.com/opencontainers/image-spec/blob/master/config.md :
	 *
	 * This JSON structure value is unusual because it is a direct JSON serialization of
	 * the Go type map[string]struct{} and is represented in JSON as an object
	 * mapping its keys to an empty object.
	 *
	 */
}
