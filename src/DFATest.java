import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DFATest {
    public static final int TEST_SIZE = 10;

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
}