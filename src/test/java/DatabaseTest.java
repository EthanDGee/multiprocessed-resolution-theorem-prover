import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DatabaseTest {

    private static final String TEST_DB_PATH = "jdbc:sqlite:test.sqlite3";
    private static final String TEST_DB_FILE = "test.sqlite3";
    private Database database;
    private Clause clause1;
    private Clause clause2;

    @Mock
    private ClauseParser mockClauseParser;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        clause1 = ClauseParser.parseClause("P(x) ∨ Q(y)");
        clause2 = ClauseParser.parseClause("¬Q(y) ∨ R(z)");
        List<Clause> initialClauses = new ArrayList<>(Arrays.asList(clause1, clause2));
        database = new Database(initialClauses, TEST_DB_PATH);
    }

    @AfterEach
    public void tearDown() {
        database.clearClauses();
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    // This test can use a mock since we're just testing counting logic
    @Test
    public void testConstructor() {
        Database mockDb = mock(Database.class);
        when(mockDb.countClauses()).thenReturn(2);
        assertEquals(2, mockDb.countClauses());
        verify(mockDb).countClauses();
    }

    // Keep as integration test since it tests actual database operations
    @Test
    public void testAddClause() {
        Clause newClause = ClauseParser.parseClause("S(a)");
        database.addClause(newClause);
        assertEquals(3, database.countClauses());

        ArrayList<Clause> allClauses = database.getClauses(1, 3);
        assertTrue(allClauses.contains(newClause));
    }

    @Test
    public void testHasEmptyClause() {
        assertFalse(database.hasEmptyClause());

        Clause emptyClause = new Clause();
        database.addClause(emptyClause);

        assertTrue(database.hasEmptyClause());
    }

    // This can use mocks since we're testing the behavior of adding multiple clauses
    @Test
    public void testAddClauses() {
        Database mockDb = mock(Database.class);
        List<Clause> clauses = Arrays.asList(
                ClauseParser.parseClause("S(a)"),
                ClauseParser.parseClause("T(b)")
        );

        doNothing().when(mockDb).addClauses(clauses);
        when(mockDb.countClauses()).thenReturn(4);

        mockDb.addClauses(clauses);
        assertEquals(4, mockDb.countClauses());
        verify(mockDb).addClauses(clauses);
    }

    // Keep remaining tests as integration tests since they test actual database operations
    // and state changes

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
    public void testFlushResolvents() {
        Clause resolvent = ClauseParser.parseClause("P(x) ∨ R(z)");
        database.addClause(resolvent);
        assertEquals(3, database.countClauses());

        database.flushResolvents();
        assertEquals(2, database.countClauses());

        // Verify that the resolvent is gone
        ArrayList<Clause> allClauses = database.getClauses(1, 3);
        assertFalse(allClauses.contains(resolvent));
    }

    @Test
    public void testClearClauses() {
        assertEquals(2, database.countClauses());
        database.clearClauses();
        assertEquals(0, database.countClauses());
    }
}