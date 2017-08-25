package com.github.swissquote.carnotzet.core.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

final class ParseUtils {

	private ParseUtils() {
		// static method holder
	}

	/**
	 * Parses a string representing a Maven artifact in standard notation.
	 *
	 * @return an instance of {@link Node} representing the artifact.
	 */
	static Node parseArtifactString(final String artifact, int id) {
		final List<String> tokens = new ArrayList<String>(7);
		int tokenStart = 0;
		boolean tokenStarted = false;
		boolean hasDescription = false;
		boolean omitted = false;
		int tokenEnd = 0;
		for (; tokenEnd < artifact.length(); tokenEnd++) {
			final char c = artifact.charAt(tokenEnd);
			switch (c) {
				case ' ': // in descriptions only
					if (tokenStarted && !hasDescription) {
						tokens.add(artifact.substring(tokenStart, tokenEnd));
						tokenStarted = false;
						hasDescription = true;
					}
					continue;

				case ':':
				case ')': //end of descriptions and omitted artifacts
					tokens.add(artifact.substring(tokenStart, tokenEnd));
					tokenStarted = false;
					continue;

				case '-': // in omitted artifacts descriptions
					continue;

				case '(': // in omitted artifacts
					if (tokenEnd == 0) {
						omitted = true;
					}
					continue;

				default:
					if (!tokenStarted) {
						tokenStart = tokenEnd;
						tokenStarted = true;
					}
			}
		}

		//last token
		if (tokenStarted) {
			tokens.add(artifact.substring(tokenStart, tokenEnd));
		}

		String groupId;
		String artifactId;
		String packaging;
		String classifier;
		String version;
		String scope;
		String description;

		if (tokens.size() == 4) {

			groupId = tokens.get(0);
			artifactId = tokens.get(1);
			packaging = tokens.get(2);
			version = tokens.get(3);
			scope = null;
			description = null;
			classifier = null;

		} else if (tokens.size() == 5) {

			groupId = tokens.get(0);
			artifactId = tokens.get(1);
			packaging = tokens.get(2);
			version = tokens.get(3);
			scope = tokens.get(4);
			description = null;
			classifier = null;

		} else if (tokens.size() == 6) {

			if (hasDescription) {
				groupId = tokens.get(0);
				artifactId = tokens.get(1);
				packaging = tokens.get(2);
				version = tokens.get(3);
				scope = tokens.get(4);
				description = tokens.get(5);
				classifier = null;
			} else {
				groupId = tokens.get(0);
				artifactId = tokens.get(1);
				packaging = tokens.get(2);
				classifier = tokens.get(3);
				version = tokens.get(4);
				scope = tokens.get(5);
				description = null;
			}

		} else if (tokens.size() == 7) {

			groupId = tokens.get(0);
			artifactId = tokens.get(1);
			packaging = tokens.get(2);
			classifier = tokens.get(3);
			version = tokens.get(4);
			scope = tokens.get(5);
			description = tokens.get(6);

		} else {
			throw new IllegalStateException("Wrong number of tokens: " + tokens.size() + " for artifact: " + artifact);
		}

		final Node node = new Node(
				groupId,
				artifactId,
				packaging,
				classifier,
				version,
				scope,
				description,
				omitted,
				null,
				new LinkedList<>()
		);
		return node;

	}

	static List<String> splitLines(final Reader reader) throws IOException {
		String line = null;
		final BufferedReader br;
		if (reader instanceof BufferedReader) {
			br = (BufferedReader) reader;
		} else {
			br = new BufferedReader(reader);
		}
		final List<String> lines = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			lines.add(line);
		}
		return lines;
	}

}
