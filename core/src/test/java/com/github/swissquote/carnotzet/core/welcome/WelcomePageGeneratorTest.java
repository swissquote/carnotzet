package com.github.swissquote.carnotzet.core.welcome;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class WelcomePageGeneratorTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void generate_welcome_page() throws IOException {
		WelcomePageGenerator generator = new WelcomePageGenerator();
		URL url = Thread.currentThread().getContextClassLoader().getResource("example_welcome");
		Path example = new File(url.getPath()).toPath();
		Path output = temp.newFolder().toPath().resolve("welcome.html");
		generator.buildWelcomeHtmlFile(example.resolve("expanded-jars"), output);

		Assert.assertEquals("The files differ!",
				new String(Files.readAllBytes(example.resolve("expected/welcome.html")), StandardCharsets.UTF_8),
				new String(Files.readAllBytes(output), StandardCharsets.UTF_8)
		);

	}

}
