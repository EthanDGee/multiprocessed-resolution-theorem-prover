import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProverThread implements Runnable {

    private final int id;
    private final Database database;

    public ProverThread(int id, Database database) {
        this.id = id;
        this.database = database;
    }

    private Set<Clause> resolveArrayLists(ArrayList<Clause> unresolved, ArrayList<Clause> clauses) {

        Set<Clause> newResolutions = new HashSet<Clause>();

        for (Clause clause1 : unresolved) {
            for (Clause clause2 : clauses) {
                List<Clause> resolvents = ResolutionTheoremProver.resolve(clause1, clause2);

                for (Clause resolvent : resolvents)

                    newResolutions.add(resolvent);

            }

        }

        return newResolutions;
    }

    private void saveResolvents(Set<Clause> resolvents) {
        database.addClauses((List<Clause>) resolvents);
        resolvents.clear();
    }

    public void run() {
        int SLEEP_TIME = 500;

        // while there is no empty clause try and solve the problem
        while (!database.hasEmptyClause()) {

            ArrayList<Clause> unresolved = database.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);

            if (unresolved.size() == 0) {
                Thread.sleep(SLEEP_TIME);
                continue;
            }

            // the max unresolved id (this will be the last in unresolved)
            int startingId = unresolved.get(unresolved.size() - 1).getId() - Constants.CLAUSE_BATCH_SIZE;

            // A set is chosen to maxize the amount of new thigns added to the database. The
            // database has a Unique constraint
            // to prevent duplicates but the overhead from querying would defeat the purpose
            // of the multiprocessing.
            Set<Clause> newResolutions = new HashSet<Clause>();

            // iterate backwords from the latest resolved chosen, get clauses in chunks and
            // resolve them
            for (int databaseIndex = startingId; startingId <= -Constants.CLAUSE_BATCH_SIZE; databaseIndex -= Constants.CLAUSE_BATCH_SIZE) {
                ArrayList<Clause> clauses = database.getClauses(databaseIndex, Constants.CLAUSE_BATCH_SIZE);
                newResolutions.addAll(newResolutions);

                // once we reach the threshold save resolvents and clear cache
                if (newResolutions.size() >= Constants.RESOLVENT_SAVE_THRESHOLD) {
                    saveResolvents(newResolutions);
                }

            }

            saveResolvents(newResolutions);
        }

    }
}
