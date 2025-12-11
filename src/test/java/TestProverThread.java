import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TestProverThread {

    @Test
    public void testRunThreadStopsWhenEmptyClauseFound() throws InterruptedException {
        Database mockDatabase = Mockito.mock(Database.class);
        AtomicBoolean emptyClauseFound = new AtomicBoolean(true); // Simulate empty clause is already found

        ProverThread proverThread = new ProverThread(1, mockDatabase, emptyClauseFound);
        proverThread.run();

        // Verify that no database interaction occurs if the empty clause is already found
        verify(mockDatabase, never()).getUnresolvedClauses(anyInt());
    }

    @Test
    public void testRunContinuesWhenNoUnresolvedClauses() throws InterruptedException {
        Database mockDatabase = Mockito.mock(Database.class);
        AtomicBoolean emptyClauseFound = new AtomicBoolean(false);

        // Simulate getUnresolvedClauses returning an empty list, then interrupting the thread
        when(mockDatabase.getUnresolvedClauses(anyInt()))
                .thenReturn(new ArrayList<>()) // First, return no work
                .thenAnswer(invocation -> {
                    emptyClauseFound.set(true); // Then, stop the thread
                    return new ArrayList<>();
                });

        ProverThread proverThread = new ProverThread(1, mockDatabase, emptyClauseFound);
        proverThread.run();

        // Verify that getUnresolvedClauses was called, but no processing happened
        verify(mockDatabase, atLeastOnce()).getUnresolvedClauses(anyInt());
        verify(mockDatabase, never()).getClauses(anyInt(), anyInt());
        verify(mockDatabase, never()).addClauses(anyList());
    }

    @Test
    public void testRunHandlesInterruptedException() throws InterruptedException {
        Database mockDatabase = Mockito.mock(Database.class);
        AtomicBoolean emptyClauseFound = new AtomicBoolean(false);

        // Simulate getUnresolvedClauses throwing an InterruptedException
        when(mockDatabase.getUnresolvedClauses(anyInt())).thenThrow(new InterruptedException());

        ProverThread proverThread = new ProverThread(1, mockDatabase, emptyClauseFound);
        proverThread.run();

        // Verify the thread was interrupted and stopped
        assertTrue(Thread.currentThread().isInterrupted());
        assertFalse(emptyClauseFound.get(), "emptyClauseFound should not be set on interrupt");
    }

    @Test
    public void testRunResolvesAndSavesClauses() throws InterruptedException {
        Database mockDatabase = Mockito.mock(Database.class);
        AtomicBoolean emptyClauseFound = new AtomicBoolean(false);

        // Prepare mock data
        Clause unresolvedClause = ClauseParser.parseClause("P(x)");
        unresolvedClause.setId(1);
        ArrayList<Clause> unresolvedClauses = new ArrayList<>(Collections.singletonList(unresolvedClause));

        Clause dbClause = ClauseParser.parseClause("¬P(A)");
        dbClause.setId(2);
        ArrayList<Clause> dbClauses = new ArrayList<>(Collections.singletonList(dbClause));

        // Mock database calls
        when(mockDatabase.getUnresolvedClauses(anyInt()))
                .thenReturn(unresolvedClauses)
                .thenAnswer(invocation -> {
                    emptyClauseFound.set(true); // Stop after one loop
                    return new ArrayList<>();
                });
        when(mockDatabase.getClauses(anyInt(), anyInt())).thenReturn(dbClauses);


        ProverThread proverThread = new ProverThread(1, mockDatabase, emptyClauseFound);
        proverThread.run();

        // Verify that resolvents were added to the database
        verify(mockDatabase, atLeastOnce()).addClauses(anyList());
        verify(mockDatabase, atLeastOnce()).setResolved(unresolvedClauses);
    }
}

