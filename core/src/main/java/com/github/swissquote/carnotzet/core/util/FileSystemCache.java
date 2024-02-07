package com.github.swissquote.carnotzet.core.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileSystemCache<T> {

	private final Path cachePath;
	private final Properties cache;
	private final Class<T> deserializationType;
	private final ObjectMapper jsonMapper;

	@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
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

	@SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION", justification = "https://github.com/spotbugs/spotbugs/issues/432")
	public T computeIfAbsent(String key, Function<String, String> mappingFunction) throws IOException {
		try (FileInputStream cacheFileInputStream = new FileInputStream(this.cachePath.toFile())) {
			this.cache.load(cacheFileInputStream);
		}

		String value = (String) this.cache.get(key);
		if (value == null) {
			value = mappingFunction.apply(key);
			this.cache.put(key, value);
			try (Writer out = new OutputStreamWriter(new FileOutputStream(this.cachePath.toFile()), Charset.forName("UTF-8"))) {
				this.cache.store(out, "Added value for key " + key);
			}
		}

		return this.jsonMapper.readValue(value, this.deserializationType);
	}
}
