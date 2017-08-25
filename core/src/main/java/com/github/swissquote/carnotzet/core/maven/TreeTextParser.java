package com.github.swissquote.carnotzet.core.maven;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Parses the text ouptut of "mvn dependency:tree -Dverbose" into a java object tree.
 * Copied from https://github.com/adutra/maven-dependency-tree-parser/ (apache 2) and modified to keep only text parsing
 * and merge code in single class
 */
public class TreeTextParser {

	private int lineIndex;

	private List<String> lines;

	public TreeTextParser() {
		this.lineIndex = 0;
		this.lines = null;
	}

	public Node parse(Reader reader) throws ParseException {

		try {
			this.lines = ParseUtils.splitLines(reader);
		}
		catch (IOException e) {
			throw new ParseException(e);
		}

		if (lines.isEmpty()) {
			return null;
		}

		return parseInternal(0);

	}

	private Node parseInternal(final int depth) {

		//current node
		final Node node = this.parseLine();

		this.lineIndex++;

		//children
		while (this.lineIndex < this.lines.size() && this.computeDepth(this.lines.get(this.lineIndex)) > depth) {
			final Node child = this.parseInternal(depth + 1);
			//if (node != null) {
			node.addChildNode(child);
			//}
		}
		return node;

	}

	private int computeDepth(final String line) {
		return getArtifactIndex(line) / 3;
	}

	/**
	 * sample lineIndex structure:
	 * <pre>|  |  \- org.apache.activemq:activeio-core:test-jar:tests:3.1.0:compile</pre>
	 */
	private Node parseLine() {
		String line = this.lines.get(this.lineIndex);
		String artifact;
		if (line.contains("active project artifact:")) {
			artifact = extractActiveProjectArtifact();
		} else {
			artifact = extractArtifact(line);
		}
		return ParseUtils.parseArtifactString(artifact, lineIndex);
	}

	private String extractArtifact(String line) {
		return line.substring(getArtifactIndex(line));
	}

	private int getArtifactIndex(final String line) {
		for (int i = 0; i < line.length(); i++) {
			final char c = line.charAt(i);
			switch (c) {
				case ' '://whitespace, standard and extended
				case '|'://standard
				case '+'://standard
				case '\\'://standard
				case '-'://standard
				case '³'://extended
				case 'Ã'://extended
				case 'Ä'://extended
				case 'À'://extended
					continue;
				default:
					return i;
			}
		}
		return -1;
	}

	/**
	 * When doing an install at the same time on a multi-module project, one can get this kind of output:
	 * <pre>
	 * +- active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = active project artifact:
	 *     artifact = com.acme.org:foobar:jar:1.0.41-SNAPSHOT:compile;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml;
	 *     project: MavenProject: com.acme.org:foobar:1.0.41-SNAPSHOT @ /opt/jenkins/home/jobs/foobar/workspace/trunk/foobar/pom.xml
	 * </pre>
	 */
	protected String extractActiveProjectArtifact() {
		String artifact = null;
		//start at next line and consume all lines containing "artifact =" or "project: "; record the last line containing "artifact =".
		boolean artifactFound = false;
		while (this.lineIndex < this.lines.size() - 1) {
			String tempLine = this.lines.get(this.lineIndex + 1);
			boolean artifactLine = !artifactFound && tempLine.contains("artifact = ");
			boolean projectLine = artifactFound && tempLine.contains("project: ");
			if (artifactLine || projectLine) {
				if (tempLine.contains("artifact = ") && !tempLine.contains("active project artifact:")) {
					artifact = StringUtils.substringBefore(StringUtils.substringAfter(tempLine, "artifact = "), ";");
					artifactFound = true;
				}
				this.lineIndex++;
			} else {
				break;
			}
		}
		return artifact;
	}

}
