import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {

    private static final String TEST_DB_PATH = "jdbc:sqlite:test.sqlite3";
    private static final String TEST_DB_FILE = "test.sqlite3";
    private Database database;
    private Clause clause1;
    private Clause clause2;

    @BeforeEach
    public void setUp() {
        // Ensure the old DB file is deleted before each test
        tearDown();
        clause1 = ClauseParser.parseClause("P(x) ∨ Q(y)");
        clause2 = ClauseParser.parseClause("¬Q(y) ∨ R(z)");
        List<Clause> initialClauses = new ArrayList<>(Arrays.asList(clause1, clause2));
        database = new Database(initialClauses, TEST_DB_PATH);
    }

    @AfterEach
    public void tearDown() {
        // deletes the database file and the associated lock and write ahead log files
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        File dbWalFile = new File(TEST_DB_FILE + "-wal");
        if (dbWalFile.exists()) {
            dbWalFile.delete();
        }
        File dbShmFile = new File(TEST_DB_FILE + "-shm");
        if (dbShmFile.exists()) {
            dbShmFile.delete();
        }
    }

    @Test
    public void testConstructor_Success() {
        assertEquals(2, database.countClauses());
        ArrayList<Clause> clauses = database.getClauses(1, 2);
        assertTrue(clauses.contains(clause1));
        assertTrue(clauses.contains(clause2));
    }

    @Test
    public void testConstructor_NullClauses() {
        assertThrows(IllegalArgumentException.class, () -> new Database(null, TEST_DB_PATH));
    }

    @Test
    public void testConstructor_EmptyClauses() {
        assertThrows(IllegalArgumentException.class, () -> new Database(new ArrayList<>(), TEST_DB_PATH));
    }

    @Test
    public void testConstructor_NullDbPath() {
        List<Clause> clauses = Collections.singletonList(clause1);
        assertThrows(IllegalArgumentException.class, () -> new Database(clauses, null));
    }

    @Test
    public void testConstructor_EmptyDbPath() {
        List<Clause> clauses = Collections.singletonList(clause1);
        assertThrows(IllegalArgumentException.class, () -> new Database(clauses, ""));
    }

    @Test
    public void testAddClause() {
        Clause newClause = ClauseParser.parseClause("S(a)");
        int initialCount = database.countClauses();
        database.addClause(newClause);
        assertEquals(initialCount + 1, database.countClauses());

        ArrayList<Clause> allClauses = database.getClauses(1, initialCount + 1);
        assertTrue(allClauses.stream().anyMatch(c -> c.equals(newClause)));
    }

    @Test
    public void testAddClause_Duplicate() {
        int initialCount = database.countClauses();
        // The UNIQUE constraint in the DB schema should prevent duplicates.
        // The current implementation prints an error but doesn't throw, which is acceptable.
        database.addClause(clause1);
        assertEquals(initialCount, database.countClauses());
    }

    @Test
    public void testAddClauses() {
        List<Clause> newClauses = Arrays.asList(
                ClauseParser.parseClause("S(a)"),
                ClauseParser.parseClause("T(b)")
        );
        int initialCount = database.countClauses();
        database.addClauses(newClauses);
        assertEquals(initialCount + 2, database.countClauses());

        ArrayList<Clause> allClauses = database.getClauses(1, initialCount + 2);
        assertTrue(allClauses.containsAll(newClauses));
    }

    @Test
    public void testAddClauses_EmptyList() {
        int initialCount = database.countClauses();
        database.addClauses(new ArrayList<>());
        assertEquals(initialCount, database.countClauses());
    }

    @Test
    public void testAddClauses_WithDuplicatesInBatch() {
        List<Clause> newClauses = Arrays.asList(
                ClauseParser.parseClause("S(a)"),
                ClauseParser.parseClause("S(a)")
        );
        int initialCount = database.countClauses();
        database.addClauses(newClauses);
        assertEquals(initialCount + 1, database.countClauses());
    }

    @Test
    public void testGetClauses() {
        ArrayList<Clause> clauses = database.getClauses(1, 2);
        assertEquals(2, clauses.size());
        assertTrue(clauses.contains(clause1));
        assertTrue(clauses.contains(clause2));
    }

    @Test
    public void testGetClausesWithOffset() {
        Clause newClause = ClauseParser.parseClause("S(a)");
        database.addClause(newClause);

        ArrayList<Clause> clauses = database.getClauses(3, 1);
        assertEquals(1, clauses.size());
        assertTrue(clauses.contains(newClause));
    }

    @Test
    public void testGetClauses_RequestMoreThanExists() {
        ArrayList<Clause> clauses = database.getClauses(1, 5);
        assertEquals(2, clauses.size());
    }

    @Test
    public void testGetClauses_RequestZero() {
        ArrayList<Clause> clauses = database.getClauses(1, 0);
        assertEquals(0, clauses.size());
    }

    @Test
    public void testGetClauses_InvalidStartIndex() {
        ArrayList<Clause> clauses = database.getClauses(100, 5);
        assertTrue(clauses.isEmpty());
    }

    @Test
    public void testGetUnresolvedClauses() {
        Clause clause3 = ClauseParser.parseClause("A(x)");
        database.addClause(clause3);

        // lastRetrieved is initialized to the ID of the first clause (ID=1).
        // The query uses `id > lastRetrieved`, so it will start fetching from ID=2.
        ArrayList<Clause> unresolved1 = database.getUnresolvedClauses(2);
        assertEquals(2, unresolved1.size());
        assertTrue(unresolved1.contains(clause2));
        assertTrue(unresolved1.contains(clause3));

        // `lastRetrieved` is now the ID of clause3 (ID=3).
        // Another call should yield no results.
        ArrayList<Clause> unresolved2 = database.getUnresolvedClauses(2);
        assertTrue(unresolved2.isEmpty());
    }

    @Test
    public void testSetResolved() {
        Clause clause3 = ClauseParser.parseClause("A(x)");
        database.addClause(clause3); // Has ID 3

        // Get the clauses to have their DB IDs assigned
        ArrayList<Clause> clausesFromDb = database.getClauses(1, 3);
        Clause clause2FromDb = clausesFromDb.stream().filter(c -> c.equals(clause2)).findFirst().get();

        // Resolve clause2
        database.setResolved(Collections.singletonList(clause2FromDb));

        // lastRetrieved is at ID 1. Query will be for id > 1.
        // It should skip resolved clause2 (ID 2) and return clause3 (ID 3).
        ArrayList<Clause> unresolved = database.getUnresolvedClauses(5);
        assertEquals(1, unresolved.size());
        assertTrue(unresolved.contains(clause3));
    }

    @Test
    public void testSetResolved_EmptyAndNullList() {
        int initialCount = database.countClauses();
        database.setResolved(null);
        database.setResolved(new ArrayList<>());
        assertEquals(initialCount, database.countClauses());
    }

    @Test
    public void testHasEmptyClause() {
        assertFalse(database.hasEmptyClause());
        database.addClause(new Clause());
        assertTrue(database.hasEmptyClause());
    }

    @Test
    public void testFlushResolvents() {
        Clause resolvent = ClauseParser.parseClause("P(x) ∨ R(z)");
        database.addClause(resolvent);
        assertEquals(3, database.countClauses());

        database.flushResolvents();
        assertEquals(2, database.countClauses());

        ArrayList<Clause> allClauses = database.getClauses(1, 3);
        assertFalse(allClauses.contains(resolvent));
    }

    @Test
    public void testFlushResolvents_ResetsLastRetrieved() {
        // Advance lastRetrieved by fetching
        database.getUnresolvedClauses(5);

        // Add a resolvent and flush
        database.addClause(ClauseParser.parseClause("P(x)"));
        database.flushResolvents();

        // After flushing, only starting set (clause1, clause2) should exist.
        // lastRetrieved should be reset to the first ID (1).
        // A call to getUnresolvedClauses should return clause1 and clause2.
        ArrayList<Clause> unresolved = database.getUnresolvedClauses(5);
        assertEquals(2, unresolved.size());
        assertTrue(unresolved.contains(clause1));
        assertTrue(unresolved.contains(clause2));
    }


    @Test
    public void testClearClauses() {
        assertEquals(2, database.countClauses());
        database.clearClauses();
        assertEquals(0, database.countClauses());
    }

    @Test
    public void testCountClauses() {
        assertEquals(2, database.countClauses());
        database.addClause(ClauseParser.parseClause("A(x)"));
        assertEquals(3, database.countClauses());
        database.clearClauses();
        assertEquals(0, database.countClauses());
    }
}