import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProverThread implements Runnable {

    private final int id;
    private final Database database;
    private final MultiThreadedResolver resolver;

    public ProverThread(int id, Database database, MultiThreadedResolver resolver) {
        this.id = id;
        this.database = database;
        this.resolver = resolver;
    }

    private Set<Clause> resolveArrayLists(ArrayList<Clause> unresolved, ArrayList<Clause> clauses) {
        Set<Clause> newResolutions = new HashSet<>();

        // iterate over all clauses in unresolved and resolves them against clauses
        for (Clause clause1 : unresolved) {
            for (Clause clause2 : clauses) {
                if (resolver.solutionFound()) {
                    return newResolutions;
                }
                List<Clause> resolvents = ResolutionTheoremProver.resolve(clause1, clause2);
                for (Clause resolvent : resolvents) {
                    if (resolvent.isEmpty()) {
                        resolver.solutionWasFound();
                    }
                    newResolutions.add(resolvent);
                }
            }
        }

        return newResolutions;
    }

    private void saveResolvents(Set<Clause> resolvents) {
        if (!resolvents.isEmpty()) {
            database.addClauses(new ArrayList<>(resolvents));
            resolvents.clear();
        }
    }

    public void run() {
        System.out.println("Thread " + id + " started.");
        // while there is no empty clause and not interrupted, try and solve the problem
        while (!resolver.solutionFound() && !Thread.currentThread().isInterrupted()) {
            try {
                ArrayList<Clause> unresolved = database.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);

                // double check to see if getUnresolvedClauses() returned anything
                if (unresolved.isEmpty()) {
                    continue;
                }

                resolver.workerStarted();
                System.out.println("Working on unresolved on Thread: " + id);
                // the max unresolved id (this will be the last in unresolved)
                int startingId = unresolved.getLast().getId() - Constants.CLAUSE_BATCH_SIZE;

                Set<Clause> newResolutions = new HashSet<>();

                // iterate backwards from the latest resolved chosen, get clauses in chunks and
                // resolve them
                int databaseIndex = startingId;

                while (databaseIndex >= -Constants.CLAUSE_BATCH_SIZE) {
                    if (resolver.solutionFound()) {
                        break;
                    }
                    ArrayList<Clause> batch_clauses = database.getClauses(databaseIndex, Constants.CLAUSE_BATCH_SIZE);
                    newResolutions.addAll(resolveArrayLists(unresolved, batch_clauses));

                    databaseIndex -= Constants.CLAUSE_BATCH_SIZE;

                    // once newResolutions reaches the save threshold, save resolvents and clear
                    if (newResolutions.size() >= Constants.RESOLVENT_SAVE_THRESHOLD) {
                        saveResolvents(newResolutions);
                    }
                }
                saveResolvents(newResolutions);
                database.setResolved(unresolved);
                resolver.workerStopped();

            } catch (InterruptedException e) {
                System.out.println("Thread " + id + " interrupted.");
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Thread " + id + " finished.");
    }
}
