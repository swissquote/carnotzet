package com.github.swissquote.carnotzet.core.maven;

import static org.junit.Assert.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

import com.github.swissquote.carnotzet.core.Carnotzet;
import com.github.swissquote.carnotzet.core.CarnotzetConfig;
import com.github.swissquote.carnotzet.core.CarnotzetModule;

public class MavenDependencyResolverTest {

	private Function<CarnotzetModuleCoordinates, String>  getDefaultModuleResolver() {
		Pattern moduleFilterPattern = Pattern.compile(CarnotzetConfig.DEFAULT_MODULE_FILTER_PATTERN);
		Pattern classifierIncludePattern = Pattern.compile(CarnotzetConfig.DEFAULT_CLASSIFIER_INCLUDE_PATTERN);

		return (module) -> Carnotzet.getModuleName(module, moduleFilterPattern, classifierIncludePattern);
	}

	@Test
	@Ignore("Dependencies are not published to Maven Central which makes it impossible to run this test in CI for now")
	public void resolveDependencies() {
		Path resourcesPath = Paths.get("/tmp/carnotzet_" + System.nanoTime());

		MavenDependencyResolver resolver = new MavenDependencyResolver(
				getDefaultModuleResolver(),
				resourcesPath.resolve("maven")
		);

		CarnotzetModuleCoordinates coord = new CarnotzetModuleCoordinates("com.github.swissquote.examples", "voting-all-carnotzet", "1.8.9-SNAPSHOT", null);

		List<CarnotzetModule> modules = resolver.resolve(coord, true);

		assertEquals(modules.size(), 6);
	}
}
