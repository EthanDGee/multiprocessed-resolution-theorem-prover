import java.util.*;
import java.util.regex.*;

public class ClauseParser {

    private static final Pattern LITERAL_PATTERN = Pattern
            .compile(Constants.NEGATIVE_SYMBOL + "?([a-zA-Z0-9]+)\\(([a-zA-Z0-9]+)\\)");
    private static final Pattern CLAUSE_PATTERN = Pattern.compile("\\s*∨\\s*|\\s*\\|\\s*|\\s*,\\s*");

    public static Clause parseClause(String clauseString) {
        Clause clause = new Clause();

        String[] atomStrings = clauseString.split(CLAUSE_PATTERN.pattern());

        for (String atomString : atomStrings) {
            atomString = atomString.trim();
            if (atomString.isEmpty())
                continue;

            Literal literal = parseLiteral(atomString);
            if (literal != null) {
                clause.addLiteral(literal);
            }
        }

        return clause;
    }

    public static Literal parseLiteral(String atomString) {
        Matcher matcher = LITERAL_PATTERN.matcher(atomString.trim());

        if (matcher.matches()) {
            boolean isPositive = !atomString.startsWith(Character.toString(Constants.NEGATIVE_SYMBOL));
            String predicate = matcher.group(1);
            String argument = matcher.group(2);

            return new Literal(predicate, argument, isPositive);
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
