package com.github.swissquote.carnotzet.core.welcome;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class WelcomePageGenerator {

	private final String before;
	private final String after;
	private final List<WelcomePagePostProcessor> postProcessors;

	// Redundant null check is not friends with try-with-resource
	@SuppressFBWarnings({"RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE", "NP_LOAD_OF_KNOWN_NULL_VALUE"})
	private String resourceAsString(String fileName) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream is = classLoader.getResourceAsStream(fileName)) {
			if (is == null) {
				return null;
			}
			try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
				 BufferedReader reader = new BufferedReader(isr)) {
				return reader.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}
	}

	@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
	public WelcomePageGenerator() {
		try {
			this.before = resourceAsString("welcome/before.html");
			this.after = resourceAsString("welcome/after.html");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.postProcessors = Collections.emptyList();
	}

	@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
	public WelcomePageGenerator(List<WelcomePagePostProcessor> postProcessors) {
		try {
			this.before = resourceAsString("welcome/before.html");
			this.after = resourceAsString("welcome/after.html");
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.postProcessors = postProcessors;
	}

	public void buildWelcomeHtmlFile(Path moduleResources, Path outputFile) throws IOException {
		StringBuilder welcomePage = new StringBuilder();
		welcomePage.append(before);

		File[] children = moduleResources.toFile().listFiles();
		if (children == null) {
			throw new CarnotzetDefinitionException(
					"Resources folder does not exist or is not a directory : [" + moduleResources + "]");
		}
		Arrays.sort(children);
		for (File child : children) {
			appendWelcomeFrom(child, welcomePage);
		}

		welcomePage.append(after);

		String welcomePageStr = welcomePage.toString();

		for (WelcomePagePostProcessor postProcessor : postProcessors) {
			welcomePageStr = postProcessor.process(welcomePageStr);
		}

		Files.write(outputFile, welcomePageStr.getBytes(StandardCharsets.UTF_8));
	}

	private void appendWelcomeFrom(File child, StringBuilder welcomePage) throws IOException {
		File welcomeFile = new File(child, "welcome/welcome.html");
		if (welcomeFile.exists()) {
			welcomePage.append(new String(Files.readAllBytes(welcomeFile.toPath()), StandardCharsets.UTF_8));
		}
	}

}
