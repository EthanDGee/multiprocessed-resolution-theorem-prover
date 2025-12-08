import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Test
    public void testConstructor() {
        assertEquals(2, database.countClauses());
    }

    @Test
    public void testAddClause() {
        Clause newClause = ClauseParser.parseClause("S(a)");
        database.addClause(newClause);
        assertEquals(3, database.countClauses());
        
        ArrayList<Clause> allClauses = database.getClauses(1, 3);
        assertTrue(allClauses.contains(newClause));
    }

    @Test
    public void testAddClauses() {
        Clause newClause1 = ClauseParser.parseClause("S(a)");
        Clause newClause2 = ClauseParser.parseClause("T(b)");
        database.addClauses(Arrays.asList(newClause1, newClause2));
        assertEquals(4, database.countClauses());
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
    public void testGetUnresolvedClauses() {
        ArrayList<Clause> unresolved = database.getUnresolvedClauses(5);
        assertEquals(2, unresolved.size());

        database.setResolved(Arrays.asList(clause1));
        unresolved = database.getUnresolvedClauses(5);
        assertEquals(1, unresolved.size());
        assertTrue(unresolved.contains(clause2));
    }

    @Test
    public void testSetResolved() {
        assertEquals(2, database.getUnresolvedClauses(5).size());

        database.setResolved(Arrays.asList(clause1));
        
        ArrayList<Clause> unresolved = database.getUnresolvedClauses(5);
        assertEquals(1, unresolved.size());
        assertFalse(unresolved.contains(clause1));
        assertTrue(unresolved.contains(clause2));
    }
    
    @Test
    public void testHasEmptyClause() {
        assertFalse(database.hasEmptyClause());
        
        Clause emptyClause = new Clause();
        database.addClause(emptyClause);
        
        assertTrue(database.hasEmptyClause());
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
