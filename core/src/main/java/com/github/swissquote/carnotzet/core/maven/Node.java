package com.github.swissquote.carnotzet.core.maven;

import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString(exclude = "childNodes")
public class Node {

	private final String groupId;

	private final String artifactId;

	private final String packaging;

	private final String classifier;

	private final String version;

	private final String scope;

	private final String description;

	private boolean omitted;

	private Node parent;

	private List<Node> childNodes = new LinkedList<>();

	public void addChildNode(final Node o) {
		o.parent = this;
		this.childNodes.add(o);
	}

}
