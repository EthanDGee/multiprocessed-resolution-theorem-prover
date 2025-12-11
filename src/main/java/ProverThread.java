import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProverThread implements Runnable {

    private final int id;
    private final boolean[] working;
    private final Database database;

    public ProverThread(int id, boolean[] working, Database database) {
        this.id = id;
        this.working = working;
        this.database = database;
    }

    public boolean threadWorking() {
        for (boolean working : working) {
            if (working) {
                return true;
            }
        }
        return false;
    }

    private Set<Clause> resolveArrayLists(ArrayList<Clause> unresolved, ArrayList<Clause> clauses) {

        Set<Clause> newResolutions = new HashSet<>();

        // iterate over all clauses in unresolved and resolves them against clauses
        for (Clause clause1 : unresolved) {
            for (Clause clause2 : clauses) {
                List<Clause> resolvents = ResolutionTheoremProver.resolve(clause1, clause2);
                newResolutions.addAll(resolvents);
            }
        }

        return newResolutions;
    }

    private void saveResolvents(Set<Clause> resolvents) {
        database.addClauses(new ArrayList<>(resolvents));
        resolvents.clear();
    }

    public void run() {
        int SLEEP_MS = 500;

        // while there is no empty clause try and solve the problem
        while (!database.hasEmptyClause() & threadWorking()) {

            ArrayList<Clause> unresolved = database.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);

            if (unresolved.isEmpty()) {
                working[id] = false;
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
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
