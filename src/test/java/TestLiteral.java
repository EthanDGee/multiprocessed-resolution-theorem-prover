import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestLiteral {

    @Test
    public void testConstructorAndGetters() {
        Literal literal = new Literal("P", "x", true);
        assertEquals("P", literal.getPredicate());
        assertEquals("x", literal.getArgument());
        assertTrue(literal.isPositive());
    }

    @Test
    public void testIsPositiveAndIsNegative() {
        Literal positiveLiteral = new Literal("P", "x", true);
        assertTrue(positiveLiteral.isPositive());
        assertFalse(positiveLiteral.isNegative());

        Literal negativeLiteral = new Literal("Q", "y", false);
        assertFalse(negativeLiteral.isPositive());
        assertTrue(negativeLiteral.isNegative());
    }

    @Test
    public void testCanResolveWith() {
        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("P", "y", false);
        Literal literal3 = new Literal("P", "z", true);
        Literal literal4 = new Literal("Q", "x", true);

        assertTrue(literal1.canResolveWith(literal2));
        assertFalse(literal1.canResolveWith(literal3));
        assertFalse(literal1.canResolveWith(literal4));
    }

    @Test
    public void testNegate() {
        Literal positiveLiteral = new Literal("P", "x", true);
        Literal negatedLiteral = positiveLiteral.negate();
        assertEquals("P", negatedLiteral.getPredicate());
        assertEquals("x", negatedLiteral.getArgument());
        assertFalse(negatedLiteral.isPositive());

        Literal negativeLiteral = new Literal("Q", "y", false);
        Literal negatedNegativeLiteral = negativeLiteral.negate();
        assertEquals("Q", negatedNegativeLiteral.getPredicate());
        assertEquals("y", negatedNegativeLiteral.getArgument());
        assertTrue(negatedNegativeLiteral.isPositive());
    }

    @Test
    public void testEquals() {
        Literal literal1 = new Literal("P", "x", true);
        Literal literal2 = new Literal("P", "x", true);
        Literal literal3 = new Literal("P", "x", false);
        Literal literal4 = new Literal("Q", "x", true);
        Literal literal5 = new Literal("P", "y", true);

        assertEquals(literal1, literal2);
        assertNotEquals(literal1, literal3);
        assertNotEquals(literal1, literal4);
        assertNotEquals(literal1, literal5);
        assertNotEquals(literal1, null);
        assertNotEquals(literal1, new Object());
    }

    @Test
    public void testToString() {
        Literal positiveLiteral = new Literal("P", "x", true);
        assertEquals("P(x)", positiveLiteral.toString());

        Literal negativeLiteral = new Literal("Q", "y", false);
        assertEquals("¬Q(y)", negativeLiteral.toString());
    }

    @Test
    public void testCopy() {
        Literal original = new Literal("R", "z", true);
        Literal copy = original.copy();

        assertEquals(original, copy);
        assertNotSame(original, copy);
    }
}
