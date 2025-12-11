import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MultiThreadedResolverTest {

    private Database mockDatabase;
    private Clause mockNegatedClause;
    private List<Clause> mockClauses;

    @BeforeEach
    public void setUp() {
        mockDatabase = mock(Database.class);
        mockNegatedClause = mock(Clause.class);
        mockClauses = new ArrayList<>();
        mockClauses.add(mock(Clause.class));
    }

    /**
     * Test class for the MultiThreadedResolver's `solve` method.
     * <p>
     * The `solve` method attempts to determine if a given negated clause can
     * result in an empty clause in the database, signifying a contradiction. This
     * process is multi-threaded, utilizing a thread pool to enable faster
     * computation by resolving clauses in parallel.
     */

    @Test
    public void testSolveReturnsTrueWhenEmptyClauseExists() {
        // Set up database behavior for this test
        when(mockDatabase.hasEmptyClause()).thenReturn(true);

        // Mock the negated clause
        Clause mockNegatedClause = mock(Clause.class);

        // Create a MultiThreadedResolver with mock clauses
        MultiThreadedResolver resolver = new MultiThreadedResolver(mockClauses);

        // Replace the real database with the mock using reflection
        try {
            Field databaseField = MultiThreadedResolver.class.getDeclaredField("database");
            databaseField.setAccessible(true);
            databaseField.set(resolver, mockDatabase);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set database field via reflection: " + e.getMessage());
        }

        // Assert that solve returns true
        assertTrue(resolver.solve(mockNegatedClause));

        // Verify database calls
        verify(mockDatabase).flushResolvents();
        verify(mockDatabase).addClause(mockNegatedClause);
    }

    @Test
    public void testSolveReturnsFalseWhenEmptyClauseDoesNotExist() {
        // Mock the database
        Database mockDatabase = mock(Database.class);
        when(mockDatabase.hasEmptyClause()).thenReturn(false);

        // Mock the negated clause
        Clause mockNegatedClause = mock(Clause.class);

        // Create a MultiThreadedResolver with mock clauses
        List<Clause> mockClauses = new ArrayList<>();
        MultiThreadedResolver resolver = new MultiThreadedResolver(mockClauses);

        // Replace the real database with the mock using reflection
        try {
            Field databaseField = MultiThreadedResolver.class.getDeclaredField("database");
            databaseField.setAccessible(true);
            databaseField.set(resolver, mockDatabase);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set database field via reflection: " + e.getMessage());
        }

        // Assert that solve returns false
        assertFalse(resolver.solve(mockNegatedClause));

        // Verify database calls
        verify(mockDatabase).flushResolvents();
        verify(mockDatabase).addClause(mockNegatedClause);
    }

    @Test
    public void testSolveStartsThreads() {
        // Mock the database
        Database mockDatabase = mock(Database.class);
        when(mockDatabase.hasEmptyClause()).thenReturn(false);

        // Mock the negated clause
        Clause mockNegatedClause = mock(Clause.class);

        // Create a MultiThreadedResolver with mock clauses
        List<Clause> mockClauses = new ArrayList<>();
        MultiThreadedResolver resolver = new MultiThreadedResolver(mockClauses);

        // Replace the real database with the mock using reflection
        try {
            Field databaseField = MultiThreadedResolver.class.getDeclaredField("database");
            databaseField.setAccessible(true);
            databaseField.set(resolver, mockDatabase);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set database field via reflection: " + e.getMessage());
        }

        // Spy on thread creation
        Thread mockThread = Mockito.mock(Thread.class);
        try (var mockedConstructor = Mockito.mockConstruction(Thread.class, (mock, context) -> {
            doNothing().when(mock).start();
        })) {
            resolver.solve(mockNegatedClause);

            // Verify that the number of threads corresponds to available processors
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            assertEquals(availableProcessors, mockedConstructor.constructed().size());
            mockedConstructor.constructed().forEach(thread -> verify(thread).start());
        }

        // Verify database calls
        verify(mockDatabase).flushResolvents();
        verify(mockDatabase).addClause(mockNegatedClause);
    }

    @Test
    public void testSolveHandlesInterruptedException() {
        // Set up database behavior for this test
        when(mockDatabase.hasEmptyClause()).thenReturn(false);

        // Mock the negated clause
        Clause mockNegatedClause = mock(Clause.class);

        // Create a MultiThreadedResolver with mock clauses
        List<Clause> mockClauses = new ArrayList<>();
        MultiThreadedResolver resolver = new MultiThreadedResolver(mockClauses);

        // Replace the real database with the mock using reflection
        try {
            Field databaseField = MultiThreadedResolver.class.getDeclaredField("database");
            databaseField.setAccessible(true);
            databaseField.set(resolver, mockDatabase);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set database field via reflection: " + e.getMessage());
        }

        // Spy on thread and simulate an interrupted exception
        Thread mockThread = mock(Thread.class);
        try (var mockedConstructor = Mockito.mockConstruction(Thread.class, (mock, context) -> {
            doNothing().when(mock).start();
            doThrow(new InterruptedException()).when(mock).join();
        })) {
            assertDoesNotThrow(() -> resolver.solve(mockNegatedClause));

            // Verify that threads attempted to join
            mockedConstructor.constructed().forEach(thread -> {
                try {
                    verify(thread).join();
                } catch (InterruptedException e) {
                    fail("join method should not rethrow the exception");
                }
            });
        }

        // Verify database calls
        verify(mockDatabase).flushResolvents();
        verify(mockDatabase).addClause(mockNegatedClause);
    }

    @Test
    public void testSolveAddsNegatedClause() {
        // Mock the database
        Database mockDatabase = mock(Database.class);

        // Create a MultiThreadedResolver with mock clauses
        MultiThreadedResolver resolver = new MultiThreadedResolver(mockClauses);

        // Replace the real database with the mock using reflection
        try {
            Field databaseField = MultiThreadedResolver.class.getDeclaredField("database");
            databaseField.setAccessible(true);
            databaseField.set(resolver, mockDatabase);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set database field via reflection: " + e.getMessage());
        }

        // Run solve method
        resolver.solve(mockNegatedClause);

        // Verify that the negated clause is added to the database
        verify(mockDatabase).addClause(mockNegatedClause);
    }
}
