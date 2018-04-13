import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexASTTest {
    private static RegexAST makeAST(String regex) {
        return new RegexAST(regex);
    }

    private static final RegexAST EMPTYWORD = makeAST("");
    private static final RegexAST SINGLE_CHAR = makeAST("a");
    private static final RegexAST WILDCARD = makeAST(".");
    private static final RegexAST STAR = makeAST("a*");
    private static final RegexAST CONCAT = makeAST("ab");
    private static final RegexAST OR = makeAST("a|b");

    @Test
    public void isEmptyWordTest() {
        assertTrue(EMPTYWORD.isEmptyWord());
        assertFalse(SINGLE_CHAR.isEmptyWord());
        assertFalse(WILDCARD.isEmptyWord());
        assertFalse(STAR.isEmptyWord());
        assertFalse(CONCAT.isEmptyWord());
        assertFalse(OR.isEmptyWord());
        assertFalse(makeAST("a?").isEmptyWord());
    }

    @Test
    public void emptyWordTest() {
        assertEquals(EMPTYWORD, makeAST("\0"));
        assertEquals(SINGLE_CHAR, makeAST("a\0"));
        assertEquals(SINGLE_CHAR, makeAST("\0a"));
        assertEquals(EMPTYWORD, makeAST("\0*"));
    }

    @Test
    public void isOperatorTest() {
        assertFalse(EMPTYWORD.isOperator());
        assertFalse(SINGLE_CHAR.isOperator());
        assertFalse(WILDCARD.isOperator());
        assertTrue(STAR.isOperator());
        assertTrue(CONCAT.isOperator());
        assertTrue(OR.isOperator());
    }

    @Test
    public void valueTest() {
        assertEquals('\0', EMPTYWORD.value());
        assertEquals('a', SINGLE_CHAR.value());
        assertEquals('.', WILDCARD.value());
        assertEquals('\0', STAR.value());
        assertEquals('\0', CONCAT.value());
        assertEquals('\0', OR.value());
    }

    @Test
    public void operatorTest() {
        assertEquals('\0', EMPTYWORD.operator());
        assertEquals('\0', SINGLE_CHAR.operator());
        assertEquals('\0', WILDCARD.operator());
        assertEquals('*', STAR.operator());
        assertEquals('^', CONCAT.operator());
        assertEquals('|', OR.operator());
    }

    @Test
    public void toStringTest() {
        assertEquals("emptyword", EMPTYWORD.toString());
        assertEquals("a", SINGLE_CHAR.toString());
        assertEquals("(WILDCARD)", WILDCARD.toString());
        assertEquals("(* a)", STAR.toString());
        assertEquals("(^ a b)", CONCAT.toString());
        assertEquals("(| a b)", OR.toString());
    }

    @Test
    public void optionalTest() {
        assertTrue(makeAST("a?").equals(makeAST("a|\0")) ||
                makeAST("a?").equals(makeAST("\0|a")));

        assertEquals(EMPTYWORD, makeAST("\0?"));
    }

    @Test
    public void plusTest() {
        assertTrue(makeAST("a+").equals(makeAST("aa*")) ||
                makeAST("a+").equals(makeAST("a*a")));

        assertEquals(EMPTYWORD, makeAST("\0+"));
    }

    @Test
    public void numQuantifyTest() {
        assertEquals(makeAST("aaa"), makeAST("a{3}"));
        assertEquals(EMPTYWORD, makeAST("a{0}"));

        assertEquals(EMPTYWORD, makeAST("\0{3}"));
    }

    @Test
    public void minMaxQuantifyTest() {
        assertEquals(makeAST("aaa(a?){2}"), makeAST("a{3,5}"));
        assertEquals(makeAST("a{3}"), makeAST("a{3,3}"));

        assertEquals(EMPTYWORD, makeAST("\0{3,5}"));
    }

    private void escapeAndTest(char specialCharacter) {
        RegexAST escaped = makeAST("\\" + specialCharacter);
        assertFalse(escaped.isOperator());
        assertEquals(specialCharacter, escaped.value());
    }

    @Test
    public void escapeTest() {
        escapeAndTest('*');
        escapeAndTest('^');
        escapeAndTest('|');
        escapeAndTest('+');
        escapeAndTest('?');
        escapeAndTest('{');
        escapeAndTest('}');
        escapeAndTest('(');
        escapeAndTest(')');
        escapeAndTest('.');
    }

    @Test
    public void multipleQuantifiersTest() {
        assertEquals(makeAST("(a*)*"), makeAST("a**"));
        assertEquals(makeAST("(.*)*"), makeAST(".**"));
        assertEquals(makeAST("(a+)*"), makeAST("a+*"));
        assertEquals(makeAST("(a{3,5})?"), makeAST("a{3,5}?"));
    }

    @Test
    public void quantifyCharacterAndConcat() {
        assertEquals(makeAST("(a*)b"), makeAST("a*b"));
        assertEquals(makeAST("b((a*)+)"), makeAST("ba*+"));
    }

    @Test
    public void isWildcardTest() {
        assertTrue(WILDCARD.isWildcard());
        assertFalse(makeAST("\\.").isWildcard());
    }
}