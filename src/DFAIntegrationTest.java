import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DFAIntegrationTest {
    public static final int TEST_SIZE = 10;

    @Test
    public void concatTest() {
        DFA dfa = new DFA("ab");
        assertTrue(dfa.match("ab"));
        assertFalse(dfa.match(""));
        assertFalse(dfa.match("a"));
        assertFalse(dfa.match("b"));
        assertFalse(dfa.match("abx"));
    }

    @Test
    public void orTest() {
        DFA dfa = new DFA("a|b");
        assertFalse(dfa.match("ab"));
        assertFalse(dfa.match(""));
        assertTrue(dfa.match("a"));
        assertTrue(dfa.match("b"));
        assertFalse(dfa.match("x"));
    }

    @Test
    public void starTest() {
        DFA dfa = new DFA("a*");
        String as = "";
        for (int i = 0; i < TEST_SIZE; i++) {
            assertTrue(dfa.match(as));
            as += "a";
        }
    }

    @Test
    public void plusTest() {
        DFA dfa = new DFA("a+");
        String as = "";
        assertFalse(dfa.match(as));
        for (int i = 0; i < TEST_SIZE; i++) {
            as += "a";
            assertTrue(dfa.match(as));
        }
    }

    @Test
    public void questionMarkTest() {
        assertTrue(new DFA("a?").match(""));
    }

    @Test
    public void repeatTest() {
        DFA dfa = new DFA("a{" + TEST_SIZE + "}");
        String as = "";
        for (int i = 0; i < TEST_SIZE; i++) {
            assertFalse(dfa.match(as));
            as += "a";
        }
        assertTrue(dfa.match(as));
        as += "a";
        assertFalse(dfa.match(as));
    }

    @Test
    public void minMaxTest() {
        int min = TEST_SIZE;
        int max = 2 * TEST_SIZE;
        DFA dfa = new DFA("a{" + min + "," + max + "}");
        String as = "";
        for (int i = 0; i < min; i++) {
            assertFalse(dfa.match(as));
            as += "a";
        }
        for (int i = 0; i <= max - min; i++) {
            assertTrue(dfa.match(as));
            as += "a";
        }
        assertFalse(dfa.match(as));
    }

    @Test
    public void emptyWordTest() {
        DFA dfa = new DFA("");
        assertTrue(dfa.match(""));
        assertFalse(dfa.match("a"));

        dfa = new DFA("\0*");
        assertTrue(dfa.match(""));
        assertFalse(dfa.match("a"));

        dfa = new DFA("text\0text");
        assertTrue(dfa.match("texttext"));
        assertFalse(dfa.match("text\0text"));
    }
}