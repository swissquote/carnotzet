package com.github.swissquote.carnotzet.core.maven;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.github.swissquote.carnotzet.core.CarnotzetDefinitionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Topologically order the dependency graph<br>
 * https://en.wikipedia.org/wiki/Topological_sorting<br>
 */
@Slf4j
public class TopologicalSorter {

	public List<Node> sort(Node root, boolean failOnCycles) {
		return new Sort(root, failOnCycles).compute();
	}

	@RequiredArgsConstructor
	private static final class Sort {

		private final Node root;

		private final Boolean failOnCycles;

		private final Map<GA, Node> resolvedNodes = new HashMap<>();

		// Only used to provide nice error messages in case of cycles
		private final Stack<Node> stack = new Stack<>();

		// for cycles detection
		private final Set<GA> temporaryMarkers = new HashSet<>();
		private final Set<GA> permanentMarkers = new HashSet<>();

		private final List<Node> result = new ArrayList<>();

		private List<Node> compute() {
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
			stack.push(n);
			if (temporaryMarkers.contains(nGA)) {
				String message = buildCycleMessage();
				if (failOnCycles) {
					throw new CarnotzetDefinitionException(message);
				} else {
					log.warn(message);
					return; // ignore the dependency
				}

			}
			temporaryMarkers.add(nGA);
			for (Node child : n.getChildNodes()) {
				depthFirst(child);
			}
			stack.pop();
			permanentMarkers.add(nGA);
			result.add(n);

		}

		private String buildCycleMessage() {
			Stack<Node> s = (Stack<Node>) stack.clone();
			Node root = s.pop();
			List<Node> cycle = new LinkedList<>();
			cycle.add(root);
			Node current = s.pop();
			while (current != root) {
				cycle.add(0, current);
				current = s.pop();
			}

			StringBuilder sb = new StringBuilder();

			sb.append("Cycle detected in dependencies graph (not a DAG). This can cause configuration overrides to be ignored.");
			sb.append(" To ensure correctness, remove the following cycle : ");

			sb.append(formatNode(root));

			cycle.forEach(node -> {
				sb.append(" -> ");
				sb.append(formatNode(node));
			});

			return sb.toString();
		}

		private String formatNode(Node n) {
			return "[" + n.getGroupId() + ":" + n.getArtifactId() + ":" + n.getVersion() + "]";
		}
	}
}


