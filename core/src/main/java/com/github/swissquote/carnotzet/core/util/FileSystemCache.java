package com.github.swissquote.carnotzet.core.util;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileSystemCache<T> {

	private final Path cachePath;
	private final Properties cache;
	private final Class<T> deserializationType;
	private final ObjectMapper jsonMapper;

	public FileSystemCache(Path cachePath, Class<T> deserializationType) {
		this.cachePath = cachePath;
		this.deserializationType = deserializationType;
		this.jsonMapper = new ObjectMapper();
		this.jsonMapper.registerModule(new JavaTimeModule());
		cache = new Properties();
		if (Files.notExists(cachePath)) {
			try {
				Files.createFile(cachePath);
			}
			catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}

	public void load() throws IOException {
		cache.load(new FileInputStream(cachePath.toFile()));
	}

	public T computeIfAbsent(String key, Function<String, String> mappingFunction) throws IOException {
		this.cache.load(new FileInputStream(this.cachePath.toFile()));

		String value = (String) this.cache.get(key);
		if(value == null)
		{
			value = mappingFunction.apply(key);
			this.cache.put(key, value);
			this.cache.store(new FileWriter(this.cachePath.toFile()), "image manifest cache");
		}

		T deserializedValue = this.jsonMapper.readValue(value, this.deserializationType);
		return deserializedValue;
	}
}
