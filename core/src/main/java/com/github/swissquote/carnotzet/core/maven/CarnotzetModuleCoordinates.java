package com.github.swissquote.carnotzet.core.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

/**
 * Utility class that can be used to describe the root maven artifact for creating a carnotzet
 */
@Value
@AllArgsConstructor
public class CarnotzetModuleCoordinates {

	@NonNull
	private final String groupId;
	@NonNull
	private final String artifactId;
	@NonNull
	private final String version;
	private final String classifier;

	public CarnotzetModuleCoordinates(String groupId, String artifactId, String version) {
		this(groupId, artifactId, version, null);
	}

	public static CarnotzetModuleCoordinates fromPom(@NonNull Path pom) {
		Model result;
		try {
			BufferedReader in = new BufferedReader(Files.newBufferedReader(pom, StandardCharsets.UTF_8));
			MavenXpp3Reader reader = new MavenXpp3Reader();
			result = reader.read(in);
		}
		catch (XmlPullParserException | IOException e) {
			throw new CarnotzetDefinitionException(e);
		}
		String groupId = result.getGroupId();
		String version = result.getVersion();
		if (groupId == null) {
			groupId = result.getParent().getGroupId();
		}
		if (version == null) {
			version = result.getParent().getVersion();
		}
		return new CarnotzetModuleCoordinates(groupId, result.getArtifactId(), version, null);
	}

}
