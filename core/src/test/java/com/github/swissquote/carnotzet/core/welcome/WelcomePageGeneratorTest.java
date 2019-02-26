package com.github.swissquote.carnotzet.core.welcome;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
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
		File example = new File(url.getPath());
		Path output = temp.newFolder().toPath().resolve("welcome.html");
		generator.buildWelcomeHtmlFile(example.toPath().resolve("expanded-jars"), output);

		Assert.assertEquals("The files differ!",
				FileUtils.readFileToString(example.toPath().resolve("expected/welcome.html").toFile(), "utf-8"),
				FileUtils.readFileToString(output.toFile(), "utf-8"));

	}

}
