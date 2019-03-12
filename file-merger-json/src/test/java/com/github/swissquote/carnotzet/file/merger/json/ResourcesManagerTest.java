package com.github.swissquote.carnotzet.file.merger.json;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.swissquote.carnotzet.core.CarnotzetModule;
import com.github.swissquote.carnotzet.core.maven.ResourcesManager;

public class ResourcesManagerTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void merge_files() throws IOException {
		// Given
		URL url = Thread.currentThread().getContextClassLoader().getResource("example_merge");
		File example = new File(url.getPath());
		Path resources = temp.newFolder().toPath();
		FileUtils.copyDirectory(example, resources.toFile());
		ResourcesManager manager = new ResourcesManager(resources, null);
		List<CarnotzetModule> modules = Arrays.asList(
				CarnotzetModule.builder().name("service3").serviceId("service3").build(),
				CarnotzetModule.builder().name("service2").serviceId("service2").build(),
				CarnotzetModule.builder().name("service1").serviceId("service1").build()
		);

		// When
		manager.resolveResources(modules);

		// Then
		ObjectMapper mapper = new ObjectMapper();
		JsonNode service3datadesired = mapper.readTree(resources.resolve("desired/service3/files/data.json").toFile());
		JsonNode service3dataresolved = mapper.readTree(resources.resolve("resolved/service3/files/data.json").toFile());
		assertThat(service3dataresolved.equals(service3datadesired), is(true));
	}

}
