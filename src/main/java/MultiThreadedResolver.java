import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiThreadedResolver {

    private final Database database;
    private final AtomicBoolean emptyClauseFound = new AtomicBoolean(false);

    public MultiThreadedResolver(List<Clause> clauses) {
        this.database = new Database(clauses);

    }


    public void closeDatabase() {
        database.close();
    }

    public Boolean prove(Clause negated) {
        database.flushResolvents();
        emptyClauseFound.set(false);

        database.addClause(negated);

        // Create Thread Pool
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        //TODO: IMPLEMENT BETTER STOPPER
        List<Thread> resolverThreads = new ArrayList<>();

        for (int i = 0; i < availableProcessors; i++) {
            // Creates a new Runnable
            Runnable worker = new ProverThread(i, database, emptyClauseFound); // adds the new thread
            resolverThreads.add(new Thread(worker));
            // runs the thread
            resolverThreads.get(i).start();
        }

        // This loops through the threads and waits for them to finish/checks if they
        // are finished
        for (Thread thread : resolverThreads) {
            try {
                thread.join();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        return emptyClauseFound.get();
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
