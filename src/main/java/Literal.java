import java.util.Objects;

public class Literal {
    private final String predicate;

    private final String argument;
    private final boolean positive;

    public Literal(String predicate, String argument, boolean positive) {

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

    public boolean canResolveWith(Literal other) {
        return this.predicate.equals(other.predicate) &&
                this.positive != other.positive;
    }

    public Literal negate() {
        return new Literal(predicate, argument, !positive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.argument, this.predicate, this.positive);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Literal other = (Literal) obj;
        return positive == other.positive &&
                Objects.equals(predicate, other.predicate) &&
                Objects.equals(argument, other.argument);
    }

    @Override
    public String toString() {
        String result = predicate + "(" + argument + ")";

        if (!this.positive)
            result = Constants.NEGATIVE_SYMBOL + result;

        return result;

    }

    public Literal copy() {
        return new Literal(predicate, argument, positive);
    }
}
