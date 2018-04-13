import java.util.*;

// TODO: Add Unit tests
public class Lexer extends DFA {
    public static final String DIGIT = "(0|1|2|3|4|5|6|7|8|9)";
    public static final String LOWER_CASE = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)";
    public static final String UPPER_CASE = "(A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z)";
    public static final String LETTER = "(" + LOWER_CASE + "|" + UPPER_CASE + ")";
    public static final String SINGLE_WHITESPACE = "( |\t|\n|\f|\r)";
    public static final String OPTIONAL_WHITESPACE = SINGLE_WHITESPACE + "*";
    public static final String WHITESPACE = SINGLE_WHITESPACE + "+";

    public static class Token {
        String value;
        String type;

        public Token(String value, String type) {
            this.value = value;
            this.type = type;
        }

        public String toString() {
            return value + " : " + type;
        }
    }

    public Lexer(String[] names, String[] tokenRegex) {
        super(names, tokenRegex);
        this.omitNames = new ArrayList<>();
        this.priority = null;
    }

    public Lexer(String[] names, String[] tokenRegex, String[] omitNames) {
        super(names, tokenRegex);
        this.omitNames = List.of(omitNames);
        this.priority = null;
    }

    public Lexer(String[] names, Map<String, Integer> priority, String[] tokenRegex) {
        super(names, tokenRegex);
        this.omitNames = new ArrayList<>();
        this.priority = priority;
    }

    public Lexer(String[] names, Map<String, Integer> priority, String[] tokenRegex, String[] omitNames) {
        super(names, tokenRegex);
        this.omitNames = List.of(omitNames);
        this.priority = priority;
    }

    private int index;
    private String input;
    private String nextToken;
    private List<String> omitNames;
    private Set<String> lastMatchNames;
    private Map<String, Integer> priority;

    public Lexer init(String input) {
        this.index = 0;
        this.input = input;
        nextToken = null;

        return this;
    }

    public String lastMatchType() {
        if (priority != null) {
            return DFA.toName(highestPriority(lastMatchNames));
        } else {
            return DFA.toName(lastMatchNames);
        }
    }

    private Set<String> highestPriority(Set<String> nameSet) {
        Set<String> highest = new HashSet<>();
        int max = Integer.MIN_VALUE;
        for (String name : nameSet) {
            int num = priority.getOrDefault(name, 0);

            if (num > max) {
                max = num;
                highest = new HashSet<>();
            }
            if (max == num)
                highest.add(name);
        }
        return highest;
    }

    public Token nextToken() {
        return new Token(next(), lastMatchType());
    }

    public String next() {
        String nextToken = null;
        boolean skip = true;
        while (skip) {
            nextToken = nextMatch();
            skip = false;
            for (String name : lastMatchNames) {
                if (omitNames.contains(name)) {
                    skip = true;
                    break;
                }
            }
        }
        return nextToken;
    }

    public Token nextMatchedToken() {
        return new Token(nextMatch(), lastMatchType());
    }

    // This does not omit
    public String nextMatch() {
        if (nextToken != null) {
            String temp = nextToken;
            nextToken = null;
            return temp;
        }
        lastMatchNames = null;

        Node current = startState;
        int startIndex = index;
        int lastMatchIndex = -1;

        while (index < input.length()) {
            if (!current.neighbors.containsKey(input.charAt(index))) {
                if (current.neighbors.containsKey(NFA.WILDCARD)) {
                    current = current.neighbors.get(NFA.WILDCARD);
                } else {
                    index = lastMatchIndex;
                    break;
                }
            } else
                current = current.neighbors.get(input.charAt(index));

            index++;
            if (acceptStates.contains(current)) {
                lastMatchIndex = index;
                lastMatchNames = current.regexMatch;
            }
        }

        if (lastMatchIndex == -1) {
            index = input.length();
            lastMatchNames = null;
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

    public boolean hasNextMatch() {
        if (nextToken != null)
            return true;

        try {
            nextToken = nextMatch();
        } catch (NoSuchElementException e) {
            return false;
        }

        return true;
    }

    public Token[] tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (index < input.length()) {
            tokens.add(nextToken());
        }
        Token[] finalTokens = new Token[tokens.size()];
        tokens.toArray(finalTokens);
        return finalTokens;
    }

    public Token[] tokenizeUntilError() {
        List<Token> tokens = new ArrayList<>();
        while (hasNext()) {
            tokens.add(nextToken());
        }
        Token[] finalTokens = new Token[tokens.size()];
        tokens.toArray(finalTokens);
        return finalTokens;
    }

    public static void main(String[] args) {
        String identifier = "(" + LETTER + "|" + DIGIT + ")*";
        String number = DIGIT + "+";
        String operation = "\\+|\\*|/|-|%";
        String comment = "\\\\\\*.*\\*\\\\|//.*\n";

        String[] omit = {"WhiteSpace", "Comment"};
        String[] names = {"Name", "Int", "Operation", "WhiteSpace", "EQ", "Comment"};
        String[] tokens = {identifier, number, operation, OPTIONAL_WHITESPACE, "=", comment};
        Lexer lexer = new Lexer(names, tokens);
        Lexer lexer1 = new Lexer(names, tokens, omit);
        Map<String, Integer> priority = new HashMap<>();
        priority.put("Name", -1);
        Lexer lexer2 = new Lexer(names, priority, tokens, omit);

        lexer.init("AYY +LMAO\\* Just a comment sakd.f/qer89\nqpon;asoifj\0\127*\\= 42// Hi!\n");
        System.out.println("lexer\n------");
        while(lexer.hasNext()) {
            System.out.println(lexer.next() + " : " + lexer.lastMatchType());
        }

        System.out.println();

        lexer1.init("AYY +LMAO\\* Just a comment sakd.f/qer89\nqpon;asoifj\0\127*\\= 42// Hi!\n");
        System.out.println("lexer1\n------");
        while(lexer1.hasNext()) {
            System.out.println(lexer1.next() + " : " + lexer1.lastMatchType());
        }

        System.out.println();

        lexer2.init("AYY +LMAO\\* Just a comment sakd.f/qer89\nqpon;asoifj\0\127*\\= 42// Hi!\n");
        System.out.println("lexer2\n------");
        while(lexer2.hasNext()) {
            System.out.println(lexer2.next() + " : " + lexer2.lastMatchType());
        }

        System.out.println();

        System.out.println("lexer3");
        Arrays.stream(new Lexer(names, priority, tokens, omit)
                .init("Hello + World= \\*Oops*\\Java9").tokenize()).forEach(System.out::println);
    }
}
