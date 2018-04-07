import java.util.ArrayList;
import java.util.List;

public class RegexAST {
    private static char[] operators = {'|', '*', '^'};
    private static boolean isCharOperator(char c) {
        for (char op : operators) {
            if (op == c)
                return true;
        }
        return false;
    }

    /**
     * Abstract Syntax Tree Node where every node is either an operation or
     * a leaf (in which case it contains a character).
     */
    private static class ASTNode {
        char operator;
        char value;
        boolean isOperator;
        ASTNode left;
        ASTNode right; // Not used by unary operators

        ASTNode(char c, ASTNode left, ASTNode right) {
            if (isCharOperator(c)) {
                isOperator = true;
                operator = c;
            } else {
                isOperator = false;
                value = c;
            }
            this.left = left;
            this.right = right;
        }

        public String toString() {
            if (!isOperator) {
                return Character.toString(value);
            }

            return "(" + operator + " " +
                    left.toString() +
                    ((right != null)?" " + right.toString():"")
                    + ")";
        }
    }

    private ASTNode root;
    private int index = 0;

    public boolean isOperator() {
        return root.isOperator;
    }

    public char value() {
        return root.value;
    }

    public char operator() {
        return root.operator;
    }

    public RegexAST left() {
        return new RegexAST(root.left);
    }

    public RegexAST right() {
        return new RegexAST(root.right);
    }

    private RegexAST(ASTNode node) {
        root = node;
    }

    private List<Character> compile(String genRegex) {
        List<Character> simpleRegex = new ArrayList<>();
        for (int i = 0; i < genRegex.length(); i++) {
            char c = genRegex.charAt(i);
            switch (c) {
                case '+':
                    simpleRegex.add(genRegex.charAt(i-1)); // This only works for single characters
                    simpleRegex.add('*');
                    break;
                default:
                    simpleRegex.add(c);
            }
        }
        return simpleRegex;
    }

    public RegexAST(String regex) {
        ASTNode current = null;
        regex = '(' + regex + ')';
        while (index < regex.length()) {
            current = matchRegex(regex, current);
        }
        root = current;
    }

    private boolean isQuantifier(char c) {
        return c == '*' || c == '+' || c == '{';
    }

    /* Want to add:
     * ? : zero or one -- Need support for empty word
     * {n} : previous is matched exactly n times
     * {min,max} : previous is matched between min and max times inclusive
     *
     * In the future, it will be more efficient to just add NFA support for
     * new quantifiers instead of compiling everything to be in terms of *,^, and |
     */
    private ASTNode quantify(String regex, ASTNode current) {
        ASTNode result = null;
        switch (regex.charAt(index)) {
            case '*':
                result = new ASTNode('*', current, null);
                index++;
                break;
            case '+':
                result = new ASTNode('^', current, new ASTNode('*', current, null));
                index++;
                break;
            case '{': //ADD HERE
                break;
        }
        return result;
    }

    /**
     * Matches a sub-regex of the regex starting at index.
     * @param regex The regular expression to be turned into an Abstract Syntax Tree.
     * @param current The ASTNode for the previously matched regex.
     * @return An ASTNode for a sub-regex of regex.
     */
    private ASTNode matchRegex(String regex, ASTNode current) {
        ASTNode result;
        switch (regex.charAt(index)) {
            case '(':
                // Move forward
                index++;
                result = null;

                // Match a regex up to the next ')'
                while (regex.charAt(index) != ')')
                    result = matchRegex(regex, result);

                // Move past the ')'
                index++;

                if (index < regex.length() && isQuantifier(regex.charAt(index))) {
                    // If the next char is a quantifier, add that quantifier to the top of the AST
                    result = quantify(regex, result);
                }

                // If this is not the outer-most (), concatenate the matched regex
                // with the previous regex
                if (current != null) {
                    result = new ASTNode('^', current, result);
                }
                break;
            case '|':
                // Move forward
                index++;

                // Make an OR with the previous regex, and the next one
                result = new ASTNode('|', current, matchRegex(regex, null));
                break;
            default:
                // Create a node for the character
                result = new ASTNode(regex.charAt(index), null, null);

                // Move forward
                index++;

                if (isQuantifier(regex.charAt(index))) {
                    // If the character is followed immediately by a quantifier, add that to the ASTNode
                    result = quantify(regex, result);
                }

                // If there is a previous regex concatenate with it
                if (current != null &&
                        (!current.isOperator ||
                                current.operator == '^' || isQuantifier(current.operator)))
                    result = new ASTNode('^', current, result);

                // If the next character is not ')', match more
                if (regex.charAt(index) != ')')
                    result = matchRegex(regex, result);
        }
        return result;
    }

    public String toString() {
        return root.toString();
    }

    public static void main(String[] args) {
        System.out.println(new RegexAST("ab|a|b"));
        System.out.println(new RegexAST("a*b|a"));
        System.out.println(new RegexAST("c|(a|b)*"));
        System.out.println(new RegexAST("(a|b)*ab*"));
        System.out.println(new RegexAST("ab*"));
        System.out.println(new RegexAST("01|001|010"));
        System.out.println(new RegexAST("(01)|(001)|(010)"));
        System.out.println(new RegexAST("(a|b)+"));
    }
}
