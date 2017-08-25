package com.github.swissquote.cartnotzet.core.maven;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.github.swissquote.carnotzet.core.maven.GAV;
import com.github.swissquote.carnotzet.core.maven.Node;
import com.github.swissquote.carnotzet.core.maven.TopologicalSorter;

public class TopologicalSorterTest {

	private Node createNode(String artifactId, String version) {
		return new Node("com.test", artifactId, null, null, version, null, null, false, null, new LinkedList<>());
	}

	private Node createOmittedNode(String artifactId, String version) {
		return new Node("com.test", artifactId, null, null, version, null, null, true, null, new LinkedList<>());
	}

	@Test
	public void happy_case() {

		Node a = createNode("a", "1");
		Node b = createNode("b", "1");
		Node c = createNode("c", "1");
		Node d = createNode("d", "1");
		Node e = createNode("e", "1");

		a.addChildNode(b);
		a.addChildNode(c);
		b.addChildNode(d);
		c.addChildNode(e);

		List<String> r = new TopologicalSorter().sort(a).stream().map(GAV::getArtifactId).collect(Collectors.toList());
		Assert.assertTrue(r.indexOf("a") > r.indexOf("b"));
		Assert.assertTrue(r.indexOf("a") > r.indexOf("c"));
		Assert.assertTrue(r.indexOf("b") > r.indexOf("d"));
		Assert.assertTrue(r.indexOf("c") > r.indexOf("e"));
	}

	@Test
	public void depth_first_counter_example() {
		Node a = createNode("a", "1");
		Node b = createNode("b", "1");
		Node c = createNode("c", "2");
		Node oc = createOmittedNode("c", "1");
		Node d = createNode("d", "1");

		a.addChildNode(oc);
		a.addChildNode(b);
		b.addChildNode(c);
		c.addChildNode(d);
		// unexpressed : oc depends on d

		List<GAV> resultGAVs = new TopologicalSorter().sort(a);
		List<String> r = resultGAVs.stream().map(GAV::getArtifactId).collect(Collectors.toList());
		Assert.assertTrue(r.indexOf("a") > r.indexOf("c"));
		Assert.assertTrue(r.indexOf("a") > r.indexOf("b"));
		Assert.assertTrue(r.indexOf("b") > r.indexOf("c"));
		Assert.assertTrue(r.indexOf("c") > r.indexOf("d"));
		Assert.assertTrue(resultGAVs.contains(new GAV("com.test", "c", "2")));
	}

	@Test
	public void breadth_first_counter_example() {
		Node a = createNode("a", "1");
		Node b = createNode("b", "1");
		Node c = createNode("c", "1");
		Node d = createNode("d", "1");
		Node od = createOmittedNode("d", "2");
		Node e = createNode("e", "1");

		a.addChildNode(b);
		b.addChildNode(c);
		c.addChildNode(od);
		a.addChildNode(d);
		d.addChildNode(e);
		// unexpressed : od depends on e

		List<GAV> resultGAVs = new TopologicalSorter().sort(a);
		List<String> r = resultGAVs.stream().map(GAV::getArtifactId).collect(Collectors.toList());
		Assert.assertTrue(r.indexOf("a") > r.indexOf("b"));
		Assert.assertTrue(r.indexOf("b") > r.indexOf("c"));
		Assert.assertTrue(r.indexOf("a") > r.indexOf("d"));
		Assert.assertTrue(r.indexOf("d") > r.indexOf("e"));
		Assert.assertTrue(resultGAVs.contains(new GAV("com.test", "d", "1")));
	}

}
