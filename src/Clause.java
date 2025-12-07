import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class Clause {
    private Set<Atom> atoms;

    public Clause() {
        this.atoms = new HashSet<>();
    }

    public Clause(Set<Atom> atoms) {
        this.atoms = new HashSet<>(atoms);
    }

    public void addAtom(Atom atom) {
        atoms.add(atom);
    }

    public Set<Atom> getAtoms() {
        return new HashSet<>(this.atoms);
    }

    public boolean isEmpty() {
        return this.atoms.isEmpty();
    }

    public boolean contains(Atom atom) {
        return this.atoms.contains(atom);
    }

    public int size() {
        return this.atoms.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Clause other = (Clause) obj;
        return Objects.equals(this.atoms, other.atoms);
    }

    @Override
    public String toString() {
        if (this.atoms.isEmpty()) {
            return "nil";
        }

        List<String> atomStrings = new ArrayList<>();
        for (Atom atom : this.atoms) {
            atomStrings.add(atom.toString());
        }

        Collections.sort(atomStrings);
        return String.join(", ", atomStrings);
    }

    public Clause copy() {
        return new Clause(this.atoms);
    }
}
