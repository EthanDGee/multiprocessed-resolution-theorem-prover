import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProverThread implements Runnable {

    private final int id;
    private final boolean[] working;
    private final Database database;
    private final AtomicBoolean emptyClauseFound;
    private final AtomicBoolean databaseHasWork;

    public ProverThread(int id, boolean[] working, Database database, AtomicBoolean emptyClauseFound, AtomicBoolean databaseHasWork) {
        this.id = id;
        this.working = working;
        this.database = database;
        this.emptyClauseFound = emptyClauseFound;
        this.databaseHasWork = databaseHasWork;
    }

    public boolean threadWorking() {
//        for (boolean working : working) {
//            if (working) {
//                return true;
//            }
//        }
//        return false;
        return true;
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
        database.addClauses(new ArrayList<>(resolvents));
        resolvents.clear();
    }

    public void run() {
        // while there is no empty clause try and solve the problem
        while (!emptyClauseFound.get() && threadWorking()) {

            //skip if the database doesn't have any work
            if (!databaseHasWork.get()) {
                continue;
            }
            System.out.println("Found Work " + id);
            ArrayList<Clause> unresolved = database.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);

            if (unresolved.isEmpty()) {
                working[id] = false;
                continue;
            }

            System.out.println("Working on unresolved on  Thread: " + id);
            working[id] = true;
            // the max unresolved id (this will be the last in unresolved)
            int startingId = unresolved.getLast().getId() - Constants.CLAUSE_BATCH_SIZE;

            // A set is chosen to maxize the amount of new thigns added to the database. The
            // database has a Unique constraint
            // to prevent duplicates but the overhead from querying would defeat the purpose
            // of the multiprocessing.
            Set<Clause> newResolutions = new HashSet<Clause>();

            // iterate backwords from the latest resolved chosen, get clauses in chunks and
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
        }

    }
}
