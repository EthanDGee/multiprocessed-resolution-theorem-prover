import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Clause {
    private Set<Literal> literals;

    public Clause() {
        this.literals = new HashSet<>();
    }

    public Clause(Set<Literal> literals) {
        this.literals = new HashSet<>(literals);
    }

    public void addAtom(Literal literal) {
        literals.add(literal);
    }

    public Set<Literal> getAtoms() {
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
        return Objects.equals(this.literals, other.literals);
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
