import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Clause {

    private int id; // Non negative number (defaults to -1 if there is no id)
    private Set<Literal> literals;

    public Clause() {
        this(-1, new HashSet<Literal>());
    }

    public Clause(Set<Literal> literals) {
        this(-1, literals);
    }

    public Clause(int id, Set<Literal> literals) {
        this.id = id;
        this.literals = literals;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id <= 0)
            throw new IllegalArgumentException("ID must be a postive number");

        // this can only be set once
        if (id != -1)
            throw new IllegalStateException("ID has already been set (" + this.id + ").");
        this.id = id;
    }

    public void addLiteral(Literal literal) {
        literals.add(literal);
    }

    public Set<Literal> getLiterals() {
        return new HashSet<>(this.literals);
    }

    public boolean isEmpty() {
        return this.literals.isEmpty();
    }

    public boolean contains(Literal literal) {
        return this.literals.contains(literal);
    }

    public int size() {
        return this.literals.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Clause other = (Clause) obj;
        return Objects.equals(this.toString(), other.toString());
    }

    @Override
    public String toString() {
        if (this.literals.isEmpty()) {
            return "nil";
        }

        List<String> atomStrings = new ArrayList<>();
        for (Literal literal : this.literals) {
            atomStrings.add(literal.toString());
        }

        Collections.sort(atomStrings);
        return String.join(", ", atomStrings);
    }

    public Clause copy() {
        return new Clause(this.literals);
    }
}
