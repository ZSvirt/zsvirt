package org.zstack.testlib.model.fsm

class DirectedGraphTraversal {
    static class Node {
        public final FSMState state
        public List<Edge> toEdges = []

        Node(FSMState state) {
            this.state = state
        }

        @Override
        String toString() {
            return state.name
        }
    }

    static class Edge {
        public final FSMEvent event
        public final Node from
        public final Node to

        Edge(FSMEvent event, Node from, Node to) {
            this.event = event
            this.from = from
            this.to = to
        }

        @Override
        String toString() {
            return event.toString()
        }
    }

    public Map<String, Node> nodes = [:]
    public Edge[][] paths = []

    static DirectedGraphTraversal create(FiniteStateMachineTestingSpec spec) {
        def d = new DirectedGraphTraversal()

        Set<Edge> edgesHaveNotVisitedYet = new HashSet<>()
        Set<Node> nodesHaveNotVisitedYet = new HashSet<>()
        Set<Edge> unreachableEdges = new HashSet<>()
        Set<Node> unreachableNodes = new HashSet<>()
        Node initNode = null

        spec.states.each {
            def node = new Node(it)
            d.nodes.put(it.name, node)
            nodesHaveNotVisitedYet << node

            if (it.initial) {
                assert initNode == null : "more than one initial node found"
                initNode = node
            }
        }
        spec.events.each {
            def fromNode = d.nodes[it.fromState]
            def toNode = d.nodes[it.toState]
            def e = new Edge(it, fromNode, toNode)
            fromNode.toEdges << e
            edgesHaveNotVisitedYet << e
        }

        Node currentNode = Objects.requireNonNull(initNode, "no initial node found")
        nodesHaveNotVisitedYet.remove(currentNode)

        List<List<Edge>> sequences = []
        List<Edge> currentSeq = []

        def takeEdge = { Edge edge ->
            currentSeq.add(edge)
            nodesHaveNotVisitedYet.remove(edge.to)
            edgesHaveNotVisitedYet.remove(edge)
            currentNode = edge.to
        }

        def flushSeq = {
            if (!currentSeq.isEmpty()) {
                sequences.add(currentSeq)
                currentSeq = []
            }
        }

        def unusedOutFrom = { Node node ->
            node.toEdges.find { edgesHaveNotVisitedYet.contains(it) }
        }

        while (!nodesHaveNotVisitedYet.isEmpty() || !edgesHaveNotVisitedYet.isEmpty()) {
            Edge nextEdge = unusedOutFrom(currentNode)
            if (nextEdge != null) {
                takeEdge(nextEdge)
                continue
            }

            def progressed = false

            for (Node targetNode : new ArrayList<Node>(nodesHaveNotVisitedYet)) {
                def path = findPath(currentNode, targetNode)
                if (path == null) {
                    continue
                }
                path.each { takeEdge(it) }
                progressed = true
                break
            }
            if (progressed) {
                continue
            }

            for (Edge targetEdge : new ArrayList<Edge>(edgesHaveNotVisitedYet)) {
                def path = findPath(currentNode, targetEdge.from)
                if (path == null) {
                    continue
                }
                path.each { takeEdge(it) }
                takeEdge(targetEdge)
                progressed = true
                break
            }
            if (progressed) {
                continue
            }

            if (currentNode != initNode) {
                flushSeq()
                currentNode = initNode
                continue
            }

            unreachableNodes.addAll(nodesHaveNotVisitedYet)
            unreachableEdges.addAll(edgesHaveNotVisitedYet)
            nodesHaveNotVisitedYet.clear()
            edgesHaveNotVisitedYet.clear()
        }

        flushSeq()
        d.paths = sequences as Edge[][]

        def unreachableMessages = []
        if (!unreachableNodes.isEmpty()) {
            unreachableMessages << "unreachable nodes: ${unreachableNodes}"
        }
        if (!unreachableEdges.isEmpty()) {
            unreachableMessages << "unreachable edges: ${unreachableEdges}"
        }
        assert unreachableMessages.isEmpty() : unreachableMessages.join("\n")

        return d
    }

    static List<Edge> findPath(Node from, Node to) {
        if (from == to) {
            return []
        }

        // if node A is not visited, paths[A] == null
        // if node A is visited, path[A] != null
        // node[from] is always null
        Map<String, Edge> paths = [:]

        List<Edge> queue = []
        queue.addAll(from.toEdges)
        Collections.shuffle(queue)

        while (!queue.isEmpty()) {
            def edge = queue.remove(0)
            if (edge.to == from || paths[edge.to.state.name] != null) {
                continue
            }

            paths[edge.to.state.name] = edge
            if (edge.to == to) {
                break
            }

            List<Edge> moreEdges = new ArrayList<>(edge.to.toEdges)
            Collections.shuffle(queue)
            queue.addAll(moreEdges)
        }

        if (paths[to.state.name] == null) {
            return null // no way: failed to find path
        }

        List<Edge> results = []
        Node current = to

        while (current != from) {
            def path = paths[current.state.name]
            results << path
            current = path.from
        }

        Collections.reverse(results)
        return results
    }

    @Override
    String toString() {
        def builder = new StringBuilder()

        builder.append("Paths list below:\n=========")
        int index = 0
        for (def list in paths) {
            builder.append("Sequence #").append(index++).append("\n")
            builder.append(list.join("\n")).append("\n")
        }

        return builder.toString()
    }
}
