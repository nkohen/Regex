/**
 * This class represents an abstract syntax tree for a regular expression.
 * The constructor takes in a regular expression as a String.
 *
 * Supported regex operations and (greedy) quantifiers: (where A and B are regular expressions, and x is any character)
 * A|B - A or B
 * AB - A followed by B
 * A* - 0 or more A's
 * A? - 0 or 1 A's
 * A+ - 1 or more A's
 * A{n} - exactly n A's
 * A{n,m} - n to m A's inclusive
 * \x - escaped x (e.g. \*, \\, \+, \{n,m}, \., etc.); escaping a character unnecissarily has no effect
 * . - wildcard character (matches any single character)
 */
public class RegexAST {
    // TODO: Add support for NOT (~), intersection (&), and ranges (a-z)
    private static char[] operators = {'|', '*', '^', '~'};
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
     * null ASTNode => empty word.
     */
    private static class ASTNode {
        char operator;
        char value;
        boolean isOperator;
        boolean isWildcard = false;
        ASTNode left; // Not used by non-operators
        ASTNode right; // Not used by unary operators

        ASTNode(char c, ASTNode left, ASTNode right) {
            if (isCharOperator(c) && left != null) {
                isOperator = true;
                operator = c;
            } else {
                isOperator = false;
                value = c;
            }
            this.left = left;
            this.right = right;
        }

        private static ASTNode getWildcard() {
            ASTNode wildcard = new ASTNode('.', null, null);
            wildcard.isWildcard = true;
            return wildcard;
        }

        /**
         * @return A flattened representation of the syntax tree in the form (op left right?)
         */
        public String toString() {
            if (isWildcard)
                return "(WILDCARD)";
            if (!isOperator || left == null) {
                return Character.toString(value);
            }

            return "(" + operator + " " +
                    left.toString() +
                    ((right != null)?" " + right.toString():((operator == '|')?" emptyword":""))
                    + ")";
        }

        boolean equals(ASTNode that) {
            if (that == null || this.operator != that.operator || this.value != that.value ||
                    this.isOperator != that.isOperator || this.isWildcard != that.isWildcard)
                return false;

            if (this.left == null)
                return that.left == null;
            else {
                if (!this.left.equals(that.left))
                    return false;
                if (this.right == null)
                    return that.right == null;
                return this.right.equals(that.right);
            }
        }
    }

    private ASTNode root;
    private int index = 0;

    /**
     * @return True if this represents the empty word
     */
    public boolean isEmptyWord() {
        return root == null;
    }

    /**
     * @return True if this represents an operation or quantification on sub-regex's
     */
    public boolean isOperator() {
        return root != null && root.isOperator;
    }

    /**
     * @return True if this represents the wildcard character
     */
    public boolean isWildcard() {
        return root != null && root.isWildcard;
    }

    /**
     * @return The character this represents, with \0 as a default value (if this is not a character)
     */
    public char value() {
        if (root == null)
            return '\0';

        return root.value;
    }

    /**
     * @return The operator or quantifying character this represents,
     * with \0 as a default value (if this is not an operation)
     */
    public char operator() {
        if (root == null)
            return '\0';

        return root.operator;
    }

    /**
     * This is the method to call with quantifiers (which have only one argument)
     * @return The left subtree of this AST, i.e. the first argument to this operator or quantifier
     * The empty word is returned if this is not an operator or quantifier
     */
    public RegexAST left() {
        return new RegexAST(root.left);
    }

    /**
     * This is not the method to call with quantifiers (which have only one argument)
     * @return The right subtree of this AST, i.e. the second argument to this operator
     * The empty word is returned if this is not an operator
     */
    public RegexAST right() {
        return new RegexAST(root.right);
    }

    /**
     * Note that two RegexAST can represent the same regex while not being the same AST,
     * for example (^ (^ a b) c) and (^ a (^ b c)) both represent abc
     * @param that The RegexAST to be compared to this
     * @return True if this and that are the same AST
     */
    public boolean equals(Object that) {
        if (!(that instanceof RegexAST))
            return false;

        if (root == null)
            return ((RegexAST)that).root == null;

        return this.root.equals(((RegexAST)that).root);
    }

    private RegexAST(ASTNode node) {
        root = node;
    }

    /**
     * Constructs an Abstract Syntax Tree for the given regular expression
     * @param regex A valid regular expression
     */
    public RegexAST(String regex) {
        ASTNode current = null;
        regex = '(' + regex + ')';
        while (index < regex.length()) {
            current = matchRegex(regex, current);
        }
        root = current;
    }

    private boolean isQuantifier(char c) {
        return c == '*' || c == '+' || c == '{' || c == '?';
    }

    private ASTNode quantify(String regex, ASTNode current) {
        ASTNode result = null;
        switch (regex.charAt(index)) {
            case '?':
                // current OR emptyword
                result = new ASTNode('|', current, null);

                // Move forward
                index++;
                break;
            case '*':
                // Quantify with *
                result = new ASTNode('*', current, null);

                // Move forward
                index++;
                break;
            case '+':
                // (^ current (* current))
                result = new ASTNode('^', current, new ASTNode('*', current, null));

                // Move forward
                index++;
                break;
            case '{':
                // Move past {
                index++;

                // Parse in next int into strnum
                char currentChar = regex.charAt(index);
                StringBuilder strnum = new StringBuilder();
                while (Character.isDigit(currentChar)) {
                    strnum.append(currentChar);
                    index++;
                    currentChar = regex.charAt(index);
                }

                // If next char is a , then we are in the {min,max} case, else the {num} case
                boolean minmax = regex.charAt(index) == ',';

                // Move past , or }
                index++;

                // Make result num consecutive currents concatenated
                int num = Integer.parseInt(strnum.toString());
                if (num <= 0)
                    result = null;
                else
                    result = current;
                for (int i = 1; i < num; i++) {
                    result = new ASTNode('^', result, current);
                }

                if (minmax) {
                    // If minmax, then parse in max
                    strnum = new StringBuilder();
                    currentChar = regex.charAt(index);
                    while (Character.isDigit(currentChar)) {
                        strnum.append(currentChar);
                        index++;
                        currentChar = regex.charAt(index);
                    }

                    // Move past }
                    index++;

                    // Add (max-min) concatenations of (current?)
                    int max = Integer.parseInt(strnum.toString());
                    if (num == max)
                        break;
                    ASTNode result2 = new ASTNode('|', current, null);
                    for (int i = 1; i < max - num; i++) {
                        result2 = new ASTNode('^', result2, new ASTNode('|', current, null));
                    }
                    result = new ASTNode('^', result, result2);
                }
                break;
        }

        // Any quantification of emptyword is emptyword
        if (current == null)
            return null;
        else
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

                while (index < regex.length() && isQuantifier(regex.charAt(index))) {
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
            case '\\':
                // Move past escape, and drop into default and treat as non-operation character
                index++;
            default:
                // If character is unescaped quantifier, quantify current and break
                if (isQuantifier(regex.charAt(index)) && regex.charAt(index-1) != '\\') {
                    result = quantify(regex, current);
                    break;
                }

                // Create a node for the character
                if (regex.charAt(index) == '\0')
                    result = null;
                else if (regex.charAt(index) == '.' && index > 0 && regex.charAt(index - 1) != '\\')
                    result = ASTNode.getWildcard();
                else
                    result = new ASTNode(regex.charAt(index), null, null);

                // Move forward
                index++;

                while (isQuantifier(regex.charAt(index))) {
                    // If the character is followed immediately by a quantifier, add that to the ASTNode
                    result = quantify(regex, result);
                }

                // If there is a previous regex (that isn't emptyword) concatenate with it
                if (current != null) {
                    // Concatenating emptyword is the same as just returning the other
                    if (result == null)
                        result = current;
                    else
                        result = new ASTNode('^', current, result);
                }

                // If the next character is not ')', match more
                if (regex.charAt(index) != ')')
                    result = matchRegex(regex, result);
        }
        return result;
    }

    /**
     * @return A flattened representation of the syntax tree in the form (op left right?)
     */
    public String toString() {
        if (root == null)
            return "emptyword";

        return root.toString();
    }
}
