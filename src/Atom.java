import java.util.Objects;

public class Atom {

    private static final char NEGATIVE_SYMBOL = '¬';

    private String predicate;
    private String argument;
    private boolean positive;

    public Atom(String predicate, String argument, boolean positive) {

        this.predicate = predicate;
        this.argument = argument;
        this.positive = positive;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getArgument() {
        return argument;
    }

    public boolean isPositive() {
        return positive;
    }

    public boolean isNegative() {
        return !positive;
    }

    public boolean canResolveWith(Atom other) {
        return this.predicate.equals(other.predicate) &&
                this.positive != other.positive;
    }

    public Atom negate() {
        return new Atom(predicate, argument, !positive);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Atom other = (Atom) obj;
        return positive == other.positive &&
                Objects.equals(predicate, other.predicate) &&
                Objects.equals(argument, other.argument);
    }

    @Override
    public String toString() {
        String result = predicate + "(" + argument + ")";

        if (!this.positive)
            result = NEGATIVE_SYMBOL + result;

        return result;

    }

    public Atom copy() {
        return new Atom(predicate, argument, positive);
    }
}
