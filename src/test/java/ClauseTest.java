import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class ClauseTest {

    @Test
    public void testDefaultConstructor() {
        Clause clause = new Clause();
        assertTrue(clause.isEmpty());
        assertEquals(0, clause.size());
    }

    @Test
    public void testConstructorWithLiterals() {
        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("Q", "y", false);
        Set<Literal> literals = new HashSet<>();
        literals.add(literal1);
        literals.add(literal2);

        Clause clause = new Clause(literals);
        assertFalse(clause.isEmpty());
        assertEquals(2, clause.size());
        assertEquals(literals, clause.getLiterals());
    }

    @Test
    public void testAddLiteral() {
        Clause clause = new Clause();
        Literal literal = new Literal("P", "x", true);
        clause.addLiteral(literal);

        assertFalse(clause.isEmpty());
        assertEquals(1, clause.size());
        assertTrue(clause.contains(literal));
    }

    @Test
    public void testGetLiterals() {
        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("Q", "y", false);
        Set<Literal> literals = new HashSet<>();
        literals.add(literal1);
        literals.add(literal2);

        Clause clause = new Clause(literals);
        Set<Literal> retrievedLiterals = clause.getLiterals();

        assertEquals(literals, retrievedLiterals);
        // Ensure it's a copy
        retrievedLiterals.add(new Literal("R", "z", true));
        assertNotEquals(retrievedLiterals, clause.getLiterals());
    }

    @Test
    public void testIsEmpty() {
        Clause emptyClause = new Clause();
        assertTrue(emptyClause.isEmpty());

        Clause nonEmptyClause = new Clause(Collections.singleton(new Literal("P", "x", true)));
        assertFalse(nonEmptyClause.isEmpty());
    }

    @Test
    public void testContains() {
        Literal literal = new Literal("P", "x", true);
        Clause clause = new Clause(Collections.singleton(literal));

        assertTrue(clause.contains(literal));
        assertFalse(clause.contains(new Literal("Q", "y", false)));
    }

    @Test
    public void testSize() {
        Clause clause = new Clause();
        assertEquals(0, clause.size());

        clause.addLiteral(new Literal("P", "x", true));
        assertEquals(1, clause.size());

        clause.addLiteral(new Literal("Q", "y", false));
        assertEquals(2, clause.size());

        // Adding the same literal should not increase the size
        clause.addLiteral(new Literal("P", "x", true));
        assertEquals(2, clause.size());
    }

    @Test
    public void testEquals() {
        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("Q", "y", false);
        Set<Literal> literals1 = new HashSet<>();
        literals1.add(literal1);
        literals1.add(literal2);

        Set<Literal> literals2 = new HashSet<>(literals1);

        Set<Literal> literals3 = new HashSet<>();
        literals3.add(literal1);

        Clause clause1 = new Clause(literals1);
        Clause clause2 = new Clause(literals2);
        Clause clause3 = new Clause(literals3);

        assertEquals(clause1, clause2);
        assertNotEquals(clause1, clause3);
        assertNotEquals(clause1, null);
        assertNotEquals(clause1, new Object());
    }

    @Test
    public void testToString() {
        Clause emptyClause = new Clause();
        assertEquals(Constants.EMPTY_CLAUSE, emptyClause.toString());

        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("Q", "y", false);
        Clause clause = new Clause();
        clause.addLiteral(literal1);
        clause.addLiteral(literal2);

        // The order is not guaranteed due to HashSet, so we check for both possibilities
        String expected1 = "P(x), ¬Q(y)";
        String expected2 = "¬Q(y), P(x)";
        String actual = clause.toString();
        assertTrue(actual.equals(expected1) || actual.equals(expected2));
    }

    @Test
    public void testCopy() {
        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("Q", "y", false);
        Set<Literal> literals = new HashSet<>();
        literals.add(literal1);
        literals.add(literal2);

        Clause original = new Clause(literals);
        Clause copy = original.copy();

        assertEquals(original, copy);
        assertNotSame(original, copy);
    }
}
