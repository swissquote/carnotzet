package com.github.swissquote.carnotzet.core.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;

import lombok.RequiredArgsConstructor;

/**
 * Topologically order the dependency graph<br>
 * https://en.wikipedia.org/wiki/Topological_sorting<br>
 */
public class TopologicalSorter {

	public List<GAV> sort(Node root) {
		return new Sort(root).compute();
	}

	@RequiredArgsConstructor
	private static final class Sort {

		private final Node root;

		private final Map<GA, Node> resolvedNodes = new HashMap<>();

		// for cycles detection
		private final Set<GA> temporaryMarkers = new HashSet<>();
		private final Set<GA> permanentMarkers = new HashSet<>();

		private List<GAV> result = new ArrayList<>();

		private List<GAV> compute() {
			// sorting the resolved tree is not enough because of omitted nodes, see unit tests for counter examples.
			getGraphFromResolvedTree(root);
			depthFirst(root);
			return result;
		}

		private void getGraphFromResolvedTree(Node n) {
			findResolvedNodes(n);
			for (Node r : resolvedNodes.values()) {
				replaceOmittedNodesByResolved(r.getChildNodes());
			}
		}

		private void replaceOmittedNodesByResolved(List<Node> nodes) {
			ListIterator<Node> i = nodes.listIterator();
			while (i.hasNext()) {
				Node child = i.next();
				if (child.isOmitted()) {
					GA childGA = new GA(child.getGroupId(), child.getArtifactId());
					Node resolved = resolvedNodes.get(childGA);
					if (resolved == null) {
						i.remove();
					} else {
						i.set(resolved);
					}
				}
			}
		}

		private void findResolvedNodes(Node n) {
			if (!n.isOmitted()) {
				GA nGA = new GA(n.getGroupId(), n.getArtifactId());
				resolvedNodes.put(nGA, n);
				n.getChildNodes().forEach(this::findResolvedNodes);
			}
		}

		private void depthFirst(Node n) {
			GA nGA = new GA(n.getGroupId(), n.getArtifactId());
			if (permanentMarkers.contains(nGA)) {
				return;
			}
			if (temporaryMarkers.contains(nGA)) {
				throw new CarnotzetDefinitionException("Cycle detected in dependencies graph (not a DAG). Fix your dependencies to remove cycles and try again.");
			}
			temporaryMarkers.add(nGA);
			for (Node child : n.getChildNodes()) {
				depthFirst(child);
			}
			GAV gav = new GAV(n.getGroupId(), n.getArtifactId(), n.getVersion());
			permanentMarkers.add(nGA);
			result.add(gav);

		}
	}
}


