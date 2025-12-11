import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestMultiThreadedResolver {

    private static final String DB_FILE = "test.sqlite3";
    private static final String DB_WAL_FILE = DB_FILE + "-wal";
    private static final String DB_SHM_FILE = DB_FILE + "-shm";

    @AfterEach
    public void tearDown() {
        // Clean up database files after each test
        deleteDbFiles();
    }

    private void deleteDbFiles() {
        File dbFile = new File(DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        File dbWalFile = new File(DB_WAL_FILE);
        if (dbWalFile.exists()) {
            dbWalFile.delete();
        }
        File dbShmFile = new File(DB_SHM_FILE);
        if (dbShmFile.exists()) {
            dbShmFile.delete();
        }
    }

    @Test
    public void testProve_ReturnsTrue_WhenContradictionExists() {
        // Given a set of clauses with a simple contradiction
        // Man(Socrates)
        Clause clause1 = ClauseParser.parseClause("Man(Socrates)");
        List<Clause> clauses = new ArrayList<>(Collections.singletonList(clause1));

        MultiThreadedResolver resolver = new MultiThreadedResolver(clauses);

        // When we try to prove the negation, which is ¬Man(Socrates)
        Clause negatedConclusion = ClauseParser.parseClause("¬Man(Socrates)");

        // Then the proof should succeed by finding an empty clause
        boolean result = resolver.prove(negatedConclusion);
        assertTrue(result, "Proof should succeed when a contradiction is provable.");
    }

    @Test
    public void testProve_ReturnsFalse_WhenNoContradiction() {
        // Given a set of consistent clauses
        Clause clause1 = ClauseParser.parseClause("P(x)");
        Clause clause2 = ClauseParser.parseClause("Q(y)");
        List<Clause> clauses = new ArrayList<>();
        clauses.add(clause1);
        clauses.add(clause2);

        MultiThreadedResolver resolver = new MultiThreadedResolver(clauses);

        // When we try to prove a clause that doesn't create a contradiction
        Clause negatedConclusion = ClauseParser.parseClause("R(z)");

        // Then the proof should fail
        boolean result = resolver.prove(negatedConclusion);
        assertFalse(result, "Proof should fail when no contradiction is found.");
    }

    @Test
    public void testProve_AddsNegatedClauseToDatabase() throws NoSuchFieldException, IllegalAccessException {
        // Given a set of initial clauses
        List<Clause> initialClauses = new ArrayList<>(Collections.singletonList(ClauseParser.parseClause("P(x)")));
        MultiThreadedResolver resolver = new MultiThreadedResolver(initialClauses);
        Clause negatedQuery = ClauseParser.parseClause("Q(y)");

        // When the prove method is called
        resolver.prove(negatedQuery);

        // Then the negated clause should be added to the database
        // We use reflection to access the private database field for verification
        Field databaseField = MultiThreadedResolver.class.getDeclaredField("database");
        databaseField.setAccessible(true);
        Database db = (Database) databaseField.get(resolver);

        // The database will contain the initial clause + the negated query
        assertEquals(2, db.countClauses());
        ArrayList<Clause> allClauses = db.getClauses(1, 2);
        assertTrue(allClauses.contains(negatedQuery), "The negated clause should have been added to the database.");
    }

    @Test
    public void testSocratesExample_Integration() {
        // This is the example from the main method, converted to a test
        // Man(Socrates)
        Clause clause1 = ClauseParser.parseClause("Man(Socrates)");

        // Man(x) -> Mortal(x)  === ¬Man(x) ∨ Mortal(x)
        Clause clause2 = ClauseParser.parseClause("¬Man(x) ∨ Mortal(x)");

        List<Clause> clauses = new ArrayList<>();
        clauses.add(clause1);
        clauses.add(clause2);

        MultiThreadedResolver resolver = new MultiThreadedResolver(clauses);

        // We want to prove Mortal(Socrates). The negated conclusion is ¬Mortal(Socrates)
        Clause negatedConclusion = ClauseParser.parseClause("¬Mortal(Socrates)");

        boolean result = resolver.prove(negatedConclusion);
        assertTrue(result, "Should successfully prove that Socrates is mortal.");
    }
}
