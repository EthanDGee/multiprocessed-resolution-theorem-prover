import java.util.*;
import java.util.regex.*;

public class ClauseParser {

    private static final String NEGATIVE_SYMBOL = "¬";
    private static final Pattern ATOM_PATTERN = Pattern.compile(NEGATIVE_SYMBOL + "?([A-Z][a-zA-Z]*)\\(([a-zA-Z])\\)");
    private static final Pattern CLAUSE_PATTERN = Pattern.compile("\\s*∨\\s*|\\s*\\|\\s*|\\s*,\\s*");

    public static Clause parseClause(String clauseString) {
        Clause clause = new Clause();

        String[] atomStrings = clauseString.split(CLAUSE_PATTERN.pattern());

        for (String atomString : atomStrings) {
            atomString = atomString.trim();
            if (atomString.isEmpty())
                continue;

            Atom atom = parseAtom(atomString);
            if (atom != null) {
                clause.addAtom(atom);
            }
        }

        return clause;
    }

    public static Atom parseAtom(String atomString) {
        Matcher matcher = ATOM_PATTERN.matcher(atomString.trim());

        if (matcher.matches()) {
            boolean isPositive = !atomString.startsWith(NEGATIVE_SYMBOL);
            String predicate = matcher.group(1);
            String argument = matcher.group(2);

            return new Atom(predicate, argument, isPositive);
        }

        throw new IllegalArgumentException("Invalid literal format: " + atomString);
    }

    public static List<Clause> parseClauses(List<String> clauseStrings) {
        List<Clause> clauses = new ArrayList<>();

        for (String clauseStr : clauseStrings) {
            clauses.add(parseClause(clauseStr));
        }

        return clauses;
    }

}
