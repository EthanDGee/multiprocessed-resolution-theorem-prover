import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProverThread implements Runnable {

    private final int id;
    private final Database database;
    private final AtomicBoolean emptyClauseFound;

    public ProverThread(int id, Database database, AtomicBoolean emptyClauseFound) {
        this.id = id;
        this.database = database;
        this.emptyClauseFound = emptyClauseFound;
    }

    private Set<Clause> resolveArrayLists(ArrayList<Clause> unresolved, ArrayList<Clause> clauses) {
        Set<Clause> newResolutions = new HashSet<>();

        // iterate over all clauses in unresolved and resolves them against clauses
        for (Clause clause1 : unresolved) {
            for (Clause clause2 : clauses) {
                if (emptyClauseFound.get()) {
                    return newResolutions;
                }
                List<Clause> resolvents = ResolutionTheoremProver.resolve(clause1, clause2);
                for (Clause resolvent : resolvents) {
                    if (resolvent.isEmpty()) {
                        emptyClauseFound.set(true);
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
        // while there is no empty clause try and solve the problem
        while (!emptyClauseFound.get()) {
            try {
                ArrayList<Clause> unresolved = database.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);

                if (unresolved.isEmpty()) {
                    continue;
                }

                System.out.println("Working on unresolved on Thread: " + id);
                // the max unresolved id (this will be the last in unresolved)
                int startingId = unresolved.getLast().getId() - Constants.CLAUSE_BATCH_SIZE;

                Set<Clause> newResolutions = new HashSet<>();

                // iterate backwards from the latest resolved chosen, get clauses in chunks and
                // resolve them
                int databaseIndex = startingId;

                while (databaseIndex >= -Constants.CLAUSE_BATCH_SIZE) {
                    if (emptyClauseFound.get()) {
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

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
