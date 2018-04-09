import java.util.*;

public class DFA {
    private class Node {
        // true iff this Node's neighbors field has been finished
        boolean neighborSet = false;

        // Transitions
        Map<Character, Node> neighbors = new HashMap<>();

        // The set of NFA nodes this Node represents
        Set<NFA.Node> set;

        Node(Set<NFA.Node> set) {
            this.set = set;
        }
    }

    private Node startState;
    private List<Node> acceptStates = new ArrayList<>();

    public DFA(String regex) {
        initFrom(NFA.makeNFA(regex));
    }

    public DFA(RegexAST regex) {
        initFrom(NFA.makeNFA(regex));
    }

    public DFA(NFA nfa) {
        initFrom(nfa);
    }

    private void initFrom(NFA nfa) {
        List<Node> allNodes = new ArrayList<>();

        Set<NFA.Node> startSet = closure(List.of(nfa.startState));

        startState = new Node(startSet);
        allNodes.add(startState);

        // Add startState to acceptStates if startSet contains any of nfa's acceptStates
        if (containsFinalState(nfa.acceptStates, startSet))
            acceptStates.add(startState);

        // Nodes to be set
        Queue<Node> nodeLine = new LinkedList<>();
        // Beginning with startState
        nodeLine.add(startState);

        while (!nodeLine.isEmpty()) {
            Node currentNode = nodeLine.poll();

            // Map each character to the set of NFA Nodes that can be reached from currentNode.set
            Map<Character, Set<NFA.Node>> transition = new HashMap<>();

            // Initialize transition
            for (NFA.Node node : currentNode.set) {
                for (Character c : node.neighbors.keySet()) {
                    // Ignore empty transitions
                    if (c == '\0')
                        continue;

                    if (!transition.containsKey(c))
                        transition.put(c, new HashSet<>());
                    transition.get(c).addAll(closure(node.neighbors.get(c)));
                }
            }

            // Initialize currentNode.transition (using put)
            for (Character c : transition.keySet()) {
                Set<NFA.Node> set = transition.get(c);
                Node neighbor = getOrCreateNode(allNodes, set);
                currentNode.neighbors.put(c, neighbor);

                // If neighbor has not yet been processed or added to nodeLine, then add it to nodeLine
                if (!neighbor.neighborSet && !nodeLine.contains(neighbor) && neighbor != currentNode) {
                    nodeLine.add(neighbor);
                    allNodes.add(neighbor);

                    // If neighbor.set (= set) contains an accept state of nfa, then add neighbor to acceptStates
                    if (containsFinalState(nfa.acceptStates, set))
                        acceptStates.add(neighbor);
                }
            }

            // Mark currentNode as initialized
            currentNode.neighborSet = true;
        }

        // Remove information in Nodes that doesn't pertain to transitions
        clearPowerSetStates(allNodes);
    }

    private void clearPowerSetStates(List<Node> nodes) {
        for (Node node : nodes) {
            node.set = null;
        }
    }

    // Returns the set of all NFA Nodes that can be reached from inSet by using empty transitions
    private Set<NFA.Node> closure(List<NFA.Node> inSet) {
        Set<NFA.Node> set = new HashSet<>(inSet);
        boolean flag = true;
        Set<NFA.Node> prevExtra = set;

        // While new Nodes are being reached, add Nodes to the set
        while (flag) {
            flag = false;
            List<NFA.Node> extra = new LinkedList<>();
            for (NFA.Node node : prevExtra) {
                if (node.neighbors.containsKey('\0')) {
                    extra.addAll(node.neighbors.get('\0'));
                    flag = true;
                }
            }
            set.addAll(extra);
            prevExtra = new HashSet<>(extra);
        }

        return set;
    }

    // Returns true if the intersection of finalStates and states is non-trivial
    private boolean containsFinalState(List<NFA.Node> finalStates, Set<NFA.Node> states) {
        for (NFA.Node node : states) {
            if (finalStates.contains(node))
                return true;
        }
        return false;
    }

    // Gets the node in nodes whose set is set, or creates such a Node and adds it to nodes
    private Node getOrCreateNode(List<Node> nodes, Set<NFA.Node> set) {
        for (Node node : nodes) {
            if (node.set.equals(set))
                return node;
        }
        Node node = new Node(set);
        nodes.add(node);
        return node;
    }

    public boolean match(String input) {
        Node current = startState;
        for (int i = 0; i < input.length(); i++) {
            if (!current.neighbors.containsKey(input.charAt(i)))
                return false;
            current = current.neighbors.get(input.charAt(i));
        }

        return acceptStates.contains(current);
    }

    // Output the graph in GraphViz
    public String toString() {
        StringBuilder out = new StringBuilder("ahead [shape = plaintext, label = \"\"];\nahead-> a0;\n");
        int nextName = 0;
        Map<Node, Integer> name = new HashMap<>();
        Queue<Node> toProcess = new LinkedList<>();
        toProcess.add(startState);
        name.put(startState, nextName++);
        while(!toProcess.isEmpty()) {
            Node currentNode = toProcess.poll();
            for (Character c : currentNode.neighbors.keySet()) {
                Node neighbor = currentNode.neighbors.get(c);
                if (!name.keySet().contains(neighbor)) {
                    name.put(neighbor, nextName++);
                    toProcess.add(neighbor);
                }
                // out += "a" + name.get(currentNode) + " -> a" + name.get(neighbor) + " [label = \"" + c + "\"];\n";
                out.append("a").append(name.get(currentNode))
                        .append(" -> a").append(name.get(neighbor)).append(" [label = \"").append(c).append("\"];\n");
            }
        }

        for (Node node : acceptStates) {
            // out += "a" + name.get(node) + " [shape = doublecircle];\n";
            out.append("a").append(name.get(node)).append(" [shape = doublecircle];\n");
        }
        return out.toString();
    }

    public static void main(String[] args) {

        DFA dfa = new DFA("(a|b)+ab*");
        System.out.println(dfa);

        dfa = new DFA("a?b{3}*");
        System.out.println(dfa);

        dfa = new DFA("((1|0(00)*01)((11|10(00)*01))*|(0(00)*1|(1|0(00)*01)((11|10(00)*01))*(0|10(00)*1))((1(00)*1|(0|1(00)*01)((11|10(00)*01))*(0|10(00)*1)))*(0|1(00)*01)((11|10(00)*01))*)");
        System.out.println(dfa);
    }
}
