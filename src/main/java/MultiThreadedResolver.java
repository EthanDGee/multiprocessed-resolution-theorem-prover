import java.util.ArrayList;
import java.util.List;

public class MultiThreadedResolver {

    private Database database;

    public MultiThreadedResolver(List<Clause> clauses) {
        this.database = new Database(clauses);

    }

    public Boolean solve(Clause negated) {
        database.flushResolvents();

        database.addClause(negated);

        // Create Thread Pool
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        List<Thread> resolverThreads = new ArrayList<>();

        for (int i = 0; i < availableProcessors; i++) {
            // Creates a new Runnable
            Runnable worker = new ProverThread(i, database); // adds the new thread
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
                System.out.println(e);
            }
        }

        return database.hasEmptyClause();
    }
}
