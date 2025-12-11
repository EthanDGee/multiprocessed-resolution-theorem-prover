import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResolutionTheoremProver {

    private final Database database;

    public ResolutionTheoremProver(List<Clause> clauses) {
        this.database = new Database(clauses);
    }

    public boolean prove(Clause negativeCase) {
        database.flushResolvents(); // clear resolvents from previous runs
        database.addClause(negativeCase);

        while (!database.hasEmptyClause()) {
            // get batch of unresolved clauses
            ArrayList<Clause> unresolved = database.getUnresolvedClauses(Constants.UNRESOLVED_BATCH_SIZE);

            // if there is nothing left to resolve we have failed
            if (unresolved.isEmpty()) {
                return false;
            }

            // the max unresolved id (this will be the last in unresolved)
            int startingId = unresolved.getLast().getId() - Constants.CLAUSE_BATCH_SIZE;

            // Use a set to maximize the amount of new things added to the database
            Set<Clause> newResolutions = new HashSet<>();

            // iterate backwards from the latest resolved chosen, get clauses in chunks and resolve them
            int databaseIndex = startingId;
            while (databaseIndex >= -Constants.CLAUSE_BATCH_SIZE) {
                ArrayList<Clause> batch_clauses = database.getClauses(databaseIndex, Constants.CLAUSE_BATCH_SIZE);

                // resolve unresolved against clauses from database
                for (Clause unresolved_clause : unresolved) {
                    for (Clause batch_clause : batch_clauses) {
                        List<Clause> resolvents = resolve(unresolved_clause, batch_clause);
                        newResolutions.addAll(resolvents);

                        // Check if we found an empty clause
                        for (Clause resolvent : resolvents) {
                            if (resolvent.isEmpty()) {
                                return true;
                            }
                        }
                    }
                }

                // Save resolvents
                database.addClauses(new ArrayList<>(newResolutions));
                newResolutions.clear();

                databaseIndex -= Constants.CLAUSE_BATCH_SIZE;
            }

            // Save any remaining resolvents
            database.addClauses(new ArrayList<>(newResolutions));
            database.setResolved(unresolved);
        }

        return true;
    }

    public static List<Clause> resolve(Clause clause1, Clause clause2) {
        List<Clause> resolvents = new ArrayList<>();

        for (Literal literal1 : clause1.getLiterals()) {
            for (Literal literal2 : clause2.getLiterals()) {
                if (literal1.canResolveWith(literal2)) {
                    Map<String, String> substitution = unify(literal1, literal2);
                    if (substitution != null) {
                        Clause resolvent = createResolvent(clause1, clause2, literal1, literal2, substitution);
                        resolvents.add(resolvent);
                    }
                }
            }
        }

        return resolvents;
    }

    private static Map<String, String> unify(Literal literal1, Literal literal2) {
        if (!literal1.getPredicate().equals(literal2.getPredicate())) {
            return null;
        }

        Map<String, String> substitution = new HashMap<>();

        String arg1 = literal1.getArgument();
        String arg2 = literal2.getArgument();

        if (arg1.equals(arg2)) {
            return substitution;
        }

        if (isVariable(arg1) && !isVariable(arg2)) {
            substitution.put(arg1, arg2);
            return substitution;
        }

        if (isVariable(arg2) && !isVariable(arg1)) {
            substitution.put(arg2, arg1);
            return substitution;
        }

        if (isVariable(arg1) && isVariable(arg2)) {
            substitution.put(arg1, arg2);
            return substitution;
        }

        return null;
    }

    private static boolean isVariable(String term) {
        return term.matches("^[a-z]$");
    }

    private static Clause createResolvent(Clause clause1, Clause clause2, Literal literal1, Literal literal2,
                                          Map<String, String> substitution) {
        Clause resolvent = new Clause();

        for (Literal literal : clause1.getLiterals()) {
            if (!literal.equals(literal1)) {
                Literal newLiteral = applySubstitution(literal, substitution);
                resolvent.addLiteral(newLiteral);
            }
        }

        for (Literal literal : clause2.getLiterals()) {
            if (!literal.equals(literal2)) {
                Literal newLit = applySubstitution(literal, substitution);
                resolvent.addLiteral(newLit);
            }
        }

        return resolvent;
    }

    private static Literal applySubstitution(Literal literal, Map<String, String> substitution) {
        String newArg = substitution.getOrDefault(literal.getArgument(), literal.getArgument());
        return new Literal(literal.getPredicate(), newArg, literal.isPositive());
    }

    public static void main(String[] args) {

        List<Clause> clauses = new ArrayList<>();

        // P(x) => Q(x) becomes ¬P(x) ∨ Q(x)
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("P", "x", false));
        clause1.addLiteral(new Literal("Q", "x", true));
        clauses.add(clause1);

        // P(a)
        Clause clause2 = new Clause();
        clause2.addLiteral(new Literal("P", "a", true));
        clauses.add(clause2);

        ResolutionTheoremProver prover = new ResolutionTheoremProver(clauses);

        // Negation of conclusion: ¬Q(a)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("Q", "a", false));

        System.out.println("Attempting to prove: From P(x)=>Q(x) and P(a), derive Q(a)");
        System.out.println("Clauses:");
        for (int i = 0; i < clauses.size(); i++) {
            System.out.println("  " + (i + 1) + ": " + clauses.get(i));
        }

        boolean result = prover.prove(negatedConclusion);
        System.out.println("\nProof " + (result ? "succeeded" : "failed"));
    }

}
