import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadedResolver {

    private final Database database;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private final AtomicBoolean solutionFound = new AtomicBoolean(false);

    public MultiThreadedResolver(List<Clause> clauses) {
        this.database = new Database(clauses);
    }

    public void workerStarted() {
        activeWorkers.incrementAndGet();
    }

    public void workerStopped() {
        activeWorkers.decrementAndGet();
    }

    public void solutionWasFound() {
        solutionFound.set(true);
    }

    public boolean solutionFound() {
        return solutionFound.get();
    }

    public void closeDatabase() {
        database.close();
    }

    public Boolean prove(Clause negated) {
        database.flushResolvents();
        solutionFound.set(false);

        database.addClause(negated);

        // Create Thread Pool
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        List<Thread> resolverThreads = new ArrayList<>();

        for (int i = 0; i < availableProcessors; i++) {
            // Creates a new Runnable
            Runnable worker = new ProverThread(i, database, this); // adds the new thread
            resolverThreads.add(new Thread(worker));
            // runs the thread
            resolverThreads.get(i).start();
        }

        // Coordinator loop to check for termination conditions
        while (true) {
            if (solutionFound.get()) {
                System.out.println("Coordinator: Solution found, terminating.");
                break;
            }

            if (activeWorkers.get() == 0 && !database.hasUnresolvedClauses()) {
                System.out.println("Coordinator: No active workers and no new clauses. Double checking...");
                try {
                    // Wait a moment to ensure this is a stable state, not a transient one
                    Thread.sleep(100);
                    if (activeWorkers.get() == 0 && !database.hasUnresolvedClauses()) {
                        System.out.println("Coordinator: Confirmed saturation, terminating.");
                        break; // saturation reached
                    } else {
                        System.out.println("Coordinator: Double check failed. Interruption cancelled.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Interrupt all worker threads to ensure they exit their loops
        for (Thread thread : resolverThreads) {
            thread.interrupt();
        }

        for (Thread thread : resolverThreads) {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println("Error joining thread: " + e.getMessage());
            }
        }

        System.out.println("Coordinator: All threads finished.");
        return solutionFound.get();
    }

    public static void main(String[] args) {
        System.out.println("--- Running Simple Example ---");
        List<Clause> clauses = new ArrayList<>();

        // Man(Socrates)
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("Man", "Socrates", true));
        clauses.add(clause1);

        // Man(x) -> Mortal(x) === ¬Man(x) ∨ Mortal(x)
        Clause clause2 = new Clause();
        clause2.addLiteral(new Literal("Man", "x", false));
        clause2.addLiteral(new Literal("Mortal", "x", true));
        clauses.add(clause2);

        MultiThreadedResolver prover = new MultiThreadedResolver(clauses);

        try {
            // Negation of conclusion: ¬Mortal(Socrates)
            Clause negatedConclusion = new Clause();
            negatedConclusion.addLiteral(new Literal("Mortal", "Socrates", false));

            System.out.println("Attempting to prove: Mortal(Socrates)");
            System.out.println("Clauses:");
            for (int i = 0; i < clauses.size(); i++) {
                System.out.println("  " + (i + 1) + ": " + clauses.get(i));
            }
            System.out.println("Negated Conclusion: " + negatedConclusion);

            boolean result = prover.prove(negatedConclusion);
            System.out.println("\nProof " + (result ? "succeeded" : "failed"));
            System.out.println("----------------------------\n");
        } finally {
            prover.closeDatabase();
        }
    }
}
