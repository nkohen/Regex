import java.util.*;

public class NFA { // Need to implement reductions
    static class Node {
        Map<Character, List<Node>> neighbors = new HashMap<>();

        void put(char key, Node neighbor) {
            if (!neighbors.containsKey(key)) {
                neighbors.put(key, new ArrayList<>());
            }

            neighbors.get(key).add(neighbor);
        }
    }

    Node startState = null;
    List<Node> acceptStates = new ArrayList<>();

    public static NFA makeNFA(String regex) {
        return makeNFA(new RegexAST(regex));
    }

    public static NFA makeNFA(RegexAST regex) {
        NFA nfa = new NFA();

        if (!regex.isOperator()) {
            Node start = new Node();
            Node end = new Node();
            start.put(regex.value(), end);
            nfa.startState = start;
            nfa.acceptStates.add(end);
            return nfa;
        }

        switch (regex.operator()) {
            case '^':
                NFA nfaLeft = makeNFA(regex.left());
                NFA nfaRight = makeNFA(regex.right());
                nfa.startState = nfaLeft.startState;
                nfa.acceptStates = nfaRight.acceptStates;
                for (Node node : nfaLeft.acceptStates) {
                    node.put('\0', nfaRight.startState);
                }
                break;
            case '|':
                nfaLeft = makeNFA(regex.left());
                nfaRight = makeNFA(regex.right());
                nfa = new NFA();
                nfa.startState = new Node();
                nfa.acceptStates = nfaLeft.acceptStates;
                nfa.acceptStates.addAll(nfaRight.acceptStates);
                nfa.startState.put('\0', nfaLeft.startState);
                nfa.startState.put('\0', nfaRight.startState);
                break;
            case '*':
                nfa = makeNFA(regex.left());
                for (Node node : nfa.acceptStates) {
                    node.put('\0', nfa.startState);
                }
                nfa.acceptStates.add(nfa.startState);
        }
        return nfa;
    }

    public String toString() {
        String out = "ahead [shape = plaintext, label = \"\"];\nahead-> a0;\n";
        int nextName = 0;
        Map<Node, Integer> name = new HashMap<>();
        java.util.Queue<Node> toProcess = new LinkedList<>();
        toProcess.add(startState);
        name.put(startState, nextName++);
        while (!toProcess.isEmpty()) {
            Node currentNode = toProcess.poll();
            for (Character c : currentNode.neighbors.keySet()) {
                for(Node neighbor : currentNode.neighbors.get(c)) {
                    if (!name.keySet().contains(neighbor)) {
                        name.put(neighbor, nextName++);
                        toProcess.add(neighbor);
                    }
                    out += "a" + name.get(currentNode) + " -> a" + name.get(neighbor) + " [label = \"" + ((c == '\0')?"eps":c) + "\"];\n";
                }
            }
        }

        for (Node node : acceptStates) {
            out += "a" + name.get(node) + " [shape = doublecircle];\n";
        }

        return out;
    }
}
