package com.github.swissquote.carnotzet.file.merger.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.swissquote.carnotzet.core.config.FileMerger;

/**
 * Know how to merge json files
 */
public class JsonMerger implements FileMerger {

	@Override
	public boolean knowsHowToMerge(Path path) {
		return path.toString().endsWith(".json");
	}

	/**
	 * Properties of file2 have precedence over the ones in file1
	 */
	@Override
	public void merge(Path file1Path, Path file2Path, Path output) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		try (InputStream in1 = Files.newInputStream(file1Path)) {
			try (InputStream in2 = Files.newInputStream(file2Path)) {
				JsonNode file1 = mapper.readTree(in1);
				JsonNode file2 = mapper.readTree(in2);
				JsonNode merged = merge(file1, file2);
				writer.writeValue(output.toFile(), merged);
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// https://stackoverflow.com/a/11459962
	public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

		Iterator<String> fieldNames = updateNode.fieldNames();
		while (fieldNames.hasNext()) {

			String fieldName = fieldNames.next();
			JsonNode jsonNode = mainNode.get(fieldName);
			// if field exists and is an embedded object
			if (jsonNode != null && jsonNode.isObject()) {
				merge(jsonNode, updateNode.get(fieldName));
			} else {
				if (mainNode instanceof ObjectNode) {
					// Overwrite field
					JsonNode value = updateNode.get(fieldName);
					((ObjectNode) mainNode).put(fieldName, value);
				}
			}

		}

		return mainNode;
	}

}
