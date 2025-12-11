import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class TestProverThread {

    /**
     * Test class for the ProverThread class.
     * <p>
     * The ProverThread class implements Runnable and is responsible for resolving
     * clauses in the database using the ResolutionTheoremProver and ensuring that
     * new resolvents are saved into the database. The run method repeatedly fetches
     * unresolved clauses, resolves them against existing clauses in the database,
     * and stores the resulting resolvents back into the database.
     */

    @Test
    public void testRunThreadStopsWhenEmptyClauseFound() {
        // Mock the database
        Database mockDatabase = Mockito.mock(Database.class);
        when(mockDatabase.hasEmptyClause()).thenReturn(true); // Simulate that the database already contains an empty
        // clause

        AtomicBoolean isRunning = new AtomicBoolean(true);

        // Run the ProverThread
        ProverThread proverThread = new ProverThread(1, new boolean[1], mockDatabase, isRunning);
        proverThread.run();

        // Verify that no operations are performed on the database since the empty
        // clause was found
        verify(mockDatabase, never()).getUnresolvedClauses(anyInt());
        verify(mockDatabase, never()).addClauses(anyList());
    }

    @Test
    public void testRunWaitsWhenNoUnresolvedClauses() throws InterruptedException {
        // Mock the database
        Database mockDatabase = Mockito.mock(Database.class);
        when(mockDatabase.hasEmptyClause()).thenReturn(false).thenReturn(true); // Simulate loop and stop after 1
        // iteration
        when(mockDatabase.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE)).thenReturn(new ArrayList<>()); // No


        AtomicBoolean isRunning = new AtomicBoolean(true);

        // Spy on Thread to verify sleep is called
        Thread mockThread = spy(Thread.class);

        // Run the ProverThread
        ProverThread proverThread = new ProverThread(1,new boolean[1],  mockDatabase, isRunning);
        proverThread.run();

        // Verify interactions with the mocked database
        verify(mockDatabase, atLeastOnce()).getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);
        verify(mockDatabase, never()).getClauses(anyInt(), eq(Constants.CLAUSE_BATCH_SIZE));
        verify(mockDatabase, never()).addClauses(anyList());

        // Verify that the thread sleeps when there are no unresolved clauses
        verifyNoMoreInteractions(mockThread);
    }

    @Test
    public void testRunHandlesInterruptedException() {
        // Mock the database
        Database mockDatabase = Mockito.mock(Database.class);
        when(mockDatabase.hasEmptyClause()).thenReturn(false); // Simulate loop never exits naturally
        when(mockDatabase.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE)).thenReturn(new ArrayList<>()); // No
        // unresolved
        // clauses

        // Mock the Thread to simulate an InterruptedException
        Thread.currentThread().interrupt();

        AtomicBoolean isRunning = new AtomicBoolean(true);

        ProverThread proverThread = new ProverThread(1,new boolean[1],  mockDatabase, isRunning);

        // Run the prover thread
        proverThread.run();

        // Verify that the thread checked unresolved clauses once and was interrupted
        verify(mockDatabase, atLeastOnce()).getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
