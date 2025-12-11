import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResolutionTheoremProverTest {

    /**
     * Test class for the ResolutionTheoremProver's `prove` method.
     * <p>
     * The `prove` method attempts to derive an empty clause (contradiction) from
     * a list of input clauses and a negated target clause. When an empty clause
     * is derived, the method returns true, signifying that the negated
     * conclusion is unsatisfiable and thus the conclusion is provable.
     */

    @Test
    public void testProveWithResolvableClausesReturnsTrue() {
        // Input clauses: ¬P(x) ∨ Q(x) and P(a)
        List<Clause> clauses = new ArrayList<>();
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("P", "x", false)); // ¬P(x)
        clause1.addLiteral(new Literal("Q", "x", true));  // Q(x)
        clauses.add(clause1);

        Clause clause2 = new Clause();
        clause2.addLiteral(new Literal("P", "a", true)); // P(a)
        clauses.add(clause2);

        // Target Clause: ¬Q(a)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("Q", "a", false)); // ¬Q(a)

        ResolutionTheoremProver prover = new ResolutionTheoremProver(clauses);

        // Act & Assert
        assertTrue(prover.prove(negatedConclusion), "The prover should return true when a contradiction (empty clause) can be derived.");
    }

    @Test
    public void testProveWithUnresolvableClausesReturnsFalse() {
        // Input clauses: ¬P(x) ∨ Q(x)
        List<Clause> clauses = new ArrayList<>();
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("P", "x", false)); // ¬P(x)
        clause1.addLiteral(new Literal("Q", "x", true));  // Q(x)
        clauses.add(clause1);

        // Target Clause: ¬R(a)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("R", "a", false)); // ¬R(a)

        ResolutionTheoremProver prover = new ResolutionTheoremProver(clauses);

        // Act & Assert
        assertFalse(prover.prove(negatedConclusion), "The prover should return false when no contradiction (empty clause) can be derived.");
    }

    @Test
    public void testProveWithEmptyClauseReturnsTrue() {
        // Input clauses: ¬P(a) ∨ Q(a) and P(a)
        List<Clause> clauses = new ArrayList<>();
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("P", "a", false)); // ¬P(a)
        clause1.addLiteral(new Literal("Q", "a", true));  // Q(a)
        clauses.add(clause1);

        Clause clause2 = new Clause();
        clause2.addLiteral(new Literal("P", "a", true)); // P(a)
        clauses.add(clause2);

        // Target Clause: ¬Q(a)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("Q", "a", false)); // ¬Q(a)

        ResolutionTheoremProver prover = new ResolutionTheoremProver(clauses);

        // Act & Assert
        assertTrue(prover.prove(negatedConclusion), "The prover should return true when the resolution process generates an empty clause (contradiction).");
    }

    @Test
    public void testProveWithComplexResolvableCaseReturnsTrue() {
        // Input clauses:
        // 1. Q(x) ∨ ¬R(x)
        // 2. R(a)
        // 3. ¬P(x) ∨ Q(x)
        // 4. P(a)
        List<Clause> clauses = new ArrayList<>();
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("Q", "x", true));  // Q(x)
        clause1.addLiteral(new Literal("R", "x", false)); // ¬R(x)
        clauses.add(clause1);

        Clause clause2 = new Clause();
        clause2.addLiteral(new Literal("R", "a", true)); // R(a)
        clauses.add(clause2);

        Clause clause3 = new Clause();
        clause3.addLiteral(new Literal("P", "x", false)); // ¬P(x)
        clause3.addLiteral(new Literal("Q", "x", true));  // Q(x)
        clauses.add(clause3);

        Clause clause4 = new Clause();
        clause4.addLiteral(new Literal("P", "a", true)); // P(a)
        clauses.add(clause4);

        // Target Clause: ¬Q(a)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("Q", "a", false)); // ¬Q(a)

        ResolutionTheoremProver prover = new ResolutionTheoremProver(clauses);

        // Act & Assert
        assertTrue(prover.prove(negatedConclusion), "The prover should return true for a complex case where a contradiction (empty clause) can be derived.");
    }
}