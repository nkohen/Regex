import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

// TODO: Add priorities to the given REGEX's and their corresponding accept states
// TODO: Have a way of knowing which type of token was matched (by marking accept states?)
// TODO: Add Unit tests
public class Lexer extends DFA {
    public static final String DIGIT = "(0|1|2|3|4|5|6|7|8|9)";
    public static final String LOWER_CASE = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)";
    public static final String UPPER_CASE = "(A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z)";
    public static final String LETTER = "(" + LOWER_CASE + "|" + UPPER_CASE + ")";
    public static final String SINGLE_WHITESPACE = "( |\t|\n|\11|\f|\r)";
    public static final String OPTIONAL_WHITESPACE = SINGLE_WHITESPACE + "*";
    public static final String WHITESPACE = SINGLE_WHITESPACE + "+";

    public Lexer(String... tokenRegex) {
        super(combineTokens(tokenRegex));
        this.omitter = new DFA("");
    }

    public Lexer(String[] omit, String... tokenRegex) {
        super(combineTokens(tokenRegex));
        this.omitter = new DFA(combineTokens(omit));
    }

    private static String combineTokens(String... tokenRegex) {
        StringBuilder regex = new StringBuilder("(" + tokenRegex[0] + ")");
        for (String token : tokenRegex) {
            regex.append("|(").append(token).append(")");
        }
        return regex.toString();
    }

    private int index;
    private String input;
    private String nextToken;
    private DFA omitter;

    public void init(String input) {
        this.index = 0;
        this.input = input;
        nextToken = null;
    }

    public String next() {
        String nextToken = nextMatch();
        while (omitter.match(nextToken)) {
            nextToken = nextMatch();
        }
        return nextToken;
    }

    public String nextMatch() {
        if (nextToken != null) {
            String temp = nextToken;
            nextToken = null;
            return temp;
        }

        Node current = startState;
        int startIndex = index;
        int lastMatchIndex = -1;

        while (index < input.length()) {
            if (!current.neighbors.containsKey(input.charAt(index))) {
                index = lastMatchIndex;
                break;
            }

            current = current.neighbors.get(input.charAt(index));
            index++;
            if (acceptStates.contains(current)) {
                lastMatchIndex = index;
            }
        }

        if (lastMatchIndex == -1) {
            index = input.length();
            throw new NoSuchElementException();
        } else {
            index = lastMatchIndex;
            return input.substring(startIndex, lastMatchIndex);
        }
    }

    public boolean hasNext() {
        if (nextToken != null)
            return true;

        try {
            nextToken = next();
        } catch (NoSuchElementException e) {
            return false;
        }

        return true;
    }

    public String[] tokenize() {
        List<String> tokens = new ArrayList<>();
        while (index < input.length()) {
            tokens.add(next());
        }
        String[] finalTokens = new String[tokens.size()];
        tokens.toArray(finalTokens);
        return finalTokens;
    }

    public String[] tokenizeUntilError() {
        List<String> tokens = new ArrayList<>();
        while (hasNext()) {
            tokens.add(next());
        }
        String[] finalTokens = new String[tokens.size()];
        tokens.toArray(finalTokens);
        return finalTokens;
    }

    public static void main(String[] args) {
        String identifier = LETTER + "(" + LETTER + "|" + DIGIT + ")*";
        String number = DIGIT + "+";
        String operation = "\\+|\\*|/|-|%";
        String[] omit = {OPTIONAL_WHITESPACE};
        Lexer lexer = new Lexer(omit, identifier, number, operation, OPTIONAL_WHITESPACE, "=");

        lexer.init("a+b + c+d + JP09jagp90J9P9 + 5");
        while(lexer.hasNext()) {
            System.out.println(lexer.next());
        }

        lexer.init(" A1+A2+A3 + 23456=");
        Stream.of(lexer.tokenizeUntilError()).forEach(System.out::println);
    }
}
