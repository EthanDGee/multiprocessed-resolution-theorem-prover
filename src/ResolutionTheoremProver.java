import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

public class ResolutionTheoremProver {

    private final int UNRESOLVED_BATCH_SIZE = 1000;
    private Database database;

    public ResolutionTheoremProver(List<Clause> clauses){
        this.database = new Database(clauses);
    }


    public boolean prove(Clause negativeCase) {
        database.flushResolvents(); // clear resolvents from previous runs
        database.addClause(negativeCase);

        // while there is no empty clause and new clauses can be generated
        while (true) {
            ArrayList<Clause> newClauses = new ArrayList<>();


            // get unresolved clauses from database
            ArrayList<Clause> clauseList = database.get_unresolved_clauses(UNRESOLVED_BATCH_SIZE);
            Set<Clause> clauseSet = new HashSet<>(clauseList);
            // if there are no clauses left, return true
            if (clauseList.isEmpty()) {
                return false;
            }

            // loop over all pairs of clauses and attempt to generate resolvents
            for (int i = 0; i < clauseList.size(); i++) {
                for (int j = i + 1; j < clauseList.size(); j++) {
                    Clause clause1 = clauseList.get(i);
                    Clause clause2 = clauseList.get(j);

                    List<Clause> resolvents = resolve(clause1, clause2);

                    for (Clause resolvent : resolvents) {
                        if (resolvent.isEmpty()) {
                            return true; // Empty clause found, contradiction
                        }

                        if (!clauseSet.contains(resolvent)) {
                            newClauses.add(resolvent);
                        }
                    }
                }
            }

            database.addClauses(newClauses);

        }
    }

    private List<Clause> resolve(Clause clause_1, Clause clause_2) {
        List<Clause> resolvents = new ArrayList<>();

        for (Atom atom_1 : clause_1.getAtoms()) {
            for (Atom atom_2 : clause_2.getAtoms()) {
                if (atom_1.canResolveWith(atom_2)) {
                    Map<String, String> substitution = unify(atom_1, atom_2);
                    if (substitution != null) {
                        Clause resolvent = createResolvent(clause_1, clause_2, atom_1, atom_2, substitution);
                        resolvents.add(resolvent);
                    }
                }
            }
        }

        return resolvents;
    }

    private Map<String, String> unify(Atom atom1, Atom atom2) {
        if (!atom1.getPredicate().equals(atom2.getPredicate())) {
            return null;
        }

        Map<String, String> substitution = new HashMap<>();

        String arg1 = atom1.getArgument();
        String arg2 = atom2.getArgument();

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

    private boolean isVariable(String term) {
        return term.matches("^[a-z]$");
    }

    private Clause createResolvent(Clause clause1, Clause clause2, Atom atom1, Atom atom2,
            Map<String, String> substitution) {
        Clause resolvent = new Clause();

        for (Atom atom : clause1.getAtoms()) {
            if (!atom.equals(atom1)) {
                Atom newAtom = applySubstitution(atom, substitution);
                resolvent.addAtom(newAtom);
            }
        }

        for (Atom atom : clause2.getAtoms()) {
            if (!atom.equals(atom2)) {
                Atom newLit = applySubstitution(atom, substitution);
                resolvent.addAtom(newLit);
            }
        }

        return resolvent;
    }

    private Atom applySubstitution(Atom atom, Map<String, String> substitution) {
        String newArg = substitution.getOrDefault(atom.getArgument(), atom.getArgument());
        return new Atom(atom.getPredicate(), newArg, atom.isPositive());
    }

    public static void main(String[] args) {

        // Example: Prove that from "P(x) => Q(x)" and "P(a)", we can derive "Q(a)"
        List<Clause> clauses = new ArrayList<>();

        // P(x) => Q(x) becomes ¬P(x) ∨ Q(x)
        Clause clause1 = new Clause();
        clause1.addAtom(new Atom("P", "x", false));
        clause1.addAtom(new Atom("Q", "x", true));
        clauses.add(clause1);

        // P(a)
        Clause clause2 = new Clause();
        clause2.addAtom(new Atom("P", "a", true));
        clauses.add(clause2);

        ResolutionTheoremProver prover = new ResolutionTheoremProver(clauses);

        // Negation of conclusion: ¬Q(a)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addAtom(new Atom("Q", "a", false));


        System.out.println("Attempting to prove: From P(x)=>Q(x) and P(a), derive Q(a)");
        System.out.println("Clauses:");
        for (int i = 0; i < clauses.size(); i++) {
            System.out.println("  " + (i + 1) + ": " + clauses.get(i));
        }

        boolean result = prover.prove(negatedConclusion);
        System.out.println("\nProof " + (result ? "succeeded" : "failed"));
    }

}
