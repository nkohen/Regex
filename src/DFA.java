import java.util.*;

public class DFA {
    private class Node {
        boolean neighborSet = false;
        Map<Character, Node> neighbors = new HashMap<>();
        Set<NFA.Node> set;

        public Node(Set<NFA.Node> set) {
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

    private void initFrom(NFA nfa) { // accepting states bug
        List<Node> allNodes = new ArrayList<>();

        Set<NFA.Node> startSet = closure(List.of(nfa.startState));
        /*
        startSet.add(nfa.startState);
        if (nfa.startState.neighbors.containsKey('\0'))
            startSet.addAll(nfa.startState.neighbors.get('\0'));
        */

        startState = new Node(startSet);
        allNodes.add(startState);
        if (nfa.acceptStates.contains(nfa.startState))
            acceptStates.add(startState);

        Queue<Node> nodeLine = new LinkedList<>();
        nodeLine.add(startState);

        while (!nodeLine.isEmpty()) {
            Node currentNode = nodeLine.poll();
            Map<Character, Set<NFA.Node>> transition = new HashMap<>();
            for (NFA.Node node : currentNode.set) {
                for (Character c : node.neighbors.keySet()) {
                    if (c == '\0')
                        continue;
                    if (!transition.containsKey(c))
                        transition.put(c, new HashSet<>());
                    transition.get(c).addAll(closure(node.neighbors.get(c)));
                }
            }

            for (Character c : transition.keySet()) {
                Set<NFA.Node> set = transition.get(c);
                Node neighbor = getNeighbor(allNodes, set);
                currentNode.neighbors.put(c, neighbor);
                if (!neighbor.neighborSet && !nodeLine.contains(neighbor) && neighbor != currentNode) {
                    nodeLine.add(neighbor);
                    allNodes.add(neighbor);
                    if (containsFinalState(nfa.acceptStates, set))
                        acceptStates.add(neighbor);
                }
            }

            currentNode.neighborSet = true;
        }

        clearPowerSetState(allNodes);
    }

    private void clearPowerSetState(List<Node> nodes) {
        for (Node node : nodes) {
            node.set = null;
        }
    }

    private Set<NFA.Node> closure(List<NFA.Node> inSet) {
        Set<NFA.Node> set = new HashSet<>(inSet);
        boolean flag = true;
        Set<NFA.Node> prevExtra = set;

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

    private boolean containsFinalState(List<NFA.Node> finalStates, Set<NFA.Node> states) {
        for (NFA.Node node : states) {
            if (finalStates.contains(node))
                return true;
        }
        return false;
    }

    private Node getNeighbor(List<Node> nodes, Set<NFA.Node> set) {
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

    public String toString() {
        String out = "ahead [shape = plaintext, label = \"\"];\nahead-> a0;\n";
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
                out += "a" + name.get(currentNode) + " -> a" + name.get(neighbor) + " [label = \"" + c + "\"];\n";
            }
        }

        for (Node node : acceptStates) {
            out += "a" + name.get(node) + " [shape = doublecircle];\n";
        }
        return out;
    }

    public static void main(String[] args) { // Needs some serious debugging

        DFA dfa = new DFA("(a|b)*ab*");
        System.out.println(dfa);

        dfa = new DFA("((1|0(00)*01)((11|10(00)*01))*|(0(00)*1|(1|0(00)*01)((11|10(00)*01))*(0|10(00)*1))((1(00)*1|(0|1(00)*01)((11|10(00)*01))*(0|10(00)*1)))*(0|1(00)*01)((11|10(00)*01))*)");
        System.out.println(dfa);
    }
}
