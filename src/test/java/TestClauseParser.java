import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

public class TestClauseParser {

    @Test
    public void testParsePositiveLiteral() {
        Literal expected = new Literal("P", "x", true);
        Literal actual = ClauseParser.parseLiteral("P(x)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseNegativeLiteral() {
        Literal expected = new Literal("Q", "y", false);
        Literal actual = ClauseParser.parseLiteral("¬Q(y)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseLiteralWithNumbers() {
        Literal expected = new Literal("P1", "arg2", true);
        Literal actual = ClauseParser.parseLiteral("P1(arg2)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseLiteralInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> ClauseParser.parseLiteral("P"));
        assertThrows(IllegalArgumentException.class, () -> ClauseParser.parseLiteral("P(x"));
        assertThrows(IllegalArgumentException.class, () -> ClauseParser.parseLiteral("P x)"));
        assertThrows(IllegalArgumentException.class, () -> ClauseParser.parseLiteral("¬P (x)"));
    }

    @Test
    public void testParseClauseSingleLiteral() {
        Clause expected = new Clause(Collections.singleton(new Literal("P", "x", true)));
        Clause actual = ClauseParser.parseClause("P(x)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseClauseWithOrSeparator() {
        Clause expected = new Clause();
        expected.addLiteral(new Literal("P", "x", true));
        expected.addLiteral(new Literal("Q", "y", false));
        Clause actual = ClauseParser.parseClause("P(x) ∨ ¬Q(y)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseClauseWithPipeSeparator() {
        Clause expected = new Clause();
        expected.addLiteral(new Literal("P", "x", true));
        expected.addLiteral(new Literal("R", "z", true));
        Clause actual = ClauseParser.parseClause("P(x) | R(z)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseClauseWithCommaSeparator() {
        Clause expected = new Clause();
        expected.addLiteral(new Literal("A", "a", false));
        expected.addLiteral(new Literal("B", "b", true));
        Clause actual = ClauseParser.parseClause("¬A(a), B(b)");
        assertEquals(expected, actual);
    }
    
    @Test
    public void testParseClauseWithMixedSeparators() {
        Clause expected = new Clause();
        expected.addLiteral(new Literal("P", "x", true));
        expected.addLiteral(new Literal("Q", "y", false));
        expected.addLiteral(new Literal("R", "z", true));
        Clause actual = ClauseParser.parseClause("P(x) ∨ ¬Q(y), R(z)");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseClauseWithExtraWhitespace() {
        Clause expected = new Clause();
        expected.addLiteral(new Literal("P", "x", true));
        expected.addLiteral(new Literal("Q", "y", false));
        Clause actual = ClauseParser.parseClause("  P(x)  ∨  ¬Q(y)  ");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseEmptyClause() {
        Clause expected = new Clause();
        Clause actual = ClauseParser.parseClause("");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseWhitespaceClause() {
        Clause expected = new Clause();
        Clause actual = ClauseParser.parseClause("   ");
        assertEquals(expected, actual);
    }

    @Test
    public void testParseClauses() {
        List<String> clauseStrings = Arrays.asList("P(x) ∨ ¬Q(y)", "R(z)", "¬S(a), T(b)");

        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("P", "x", true));
        clause1.addLiteral(new Literal("Q", "y", false));

        Clause clause2 = new Clause(Collections.singleton(new Literal("R", "z", true)));

        Clause clause3 = new Clause();
        clause3.addLiteral(new Literal("S", "a", false));
        clause3.addLiteral(new Literal("T", "b", true));

        List<Clause> expected = Arrays.asList(clause1, clause2, clause3);
        List<Clause> actual = ClauseParser.parseClauses(clauseStrings);

        assertEquals(expected, actual);
    }

    @Test
    public void testParseEmptyClauseList() {
        List<String> clauseStrings = Collections.emptyList();
        List<Clause> expected = Collections.emptyList();
        List<Clause> actual = ClauseParser.parseClauses(clauseStrings);
        assertEquals(expected, actual);
    }
}
