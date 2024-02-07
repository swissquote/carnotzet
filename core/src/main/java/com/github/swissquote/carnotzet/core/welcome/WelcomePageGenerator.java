package com.github.swissquote.carnotzet.core.welcome;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class WelcomePageGenerator {

	private final String before;
	private final String after;
	private final List<WelcomePagePostProcessor> postProcessors;

	@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
	public WelcomePageGenerator() {

		try {
			this.before = Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/before.html"), Charsets.UTF_8);
			this.after = Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/after.html"), Charsets.UTF_8);

		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		this.postProcessors = Collections.emptyList();
	}

	@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
	public WelcomePageGenerator(List<WelcomePagePostProcessor> postProcessors) {
		try {
			this.before = Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/before.html"), Charsets.UTF_8);
			this.after = Resources.toString(Thread.currentThread().getContextClassLoader().getResource("welcome/after.html"), Charsets.UTF_8);

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

		Files.write(welcomePageStr, outputFile.toFile(), Charsets.UTF_8);
	}

	private void appendWelcomeFrom(File child, StringBuilder welcomePage) throws IOException {
		File welcomeFile = new File(child, "welcome/welcome.html");
		if (welcomeFile.exists()) {
			welcomePage.append(Files.toString(welcomeFile, Charsets.UTF_8));
		}
	}

}
