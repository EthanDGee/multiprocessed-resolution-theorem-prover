import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Benchmark {
    public static void main(String[] args) {
        Example example = new Benchmark().largeExample();
        try {
            example.runExample();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Example simpleExample() {
        System.out.println("--- Running Simple Example ---");
        List<Clause> clauses = new ArrayList<>();

        // Man(Socrates)
        Clause clause1 = new Clause();
        clause1.addLiteral(new Literal("Man", "Socrates", true));
        clauses.add(clause1);

        // Man(x) -> Mortal(x) === ¬Man(x) ∨ Mortal(x)
        Clause clause2 = new Clause();
        clause2.addLiteral(new Literal("Man", "x", false));
        clause2.addLiteral(new Literal("Mortal", "x", true));
        clauses.add(clause2);


        // Negation of conclusion: ¬Mortal(Socrates)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("Mortal", "Socrates", false));

        return new Example(clauses, negatedConclusion);
    }

    public Example moderateExample() {
        System.out.println("--- Running Moderate Example (12 clauses) ---");
        List<Clause> clauses = new ArrayList<>();

        // 1. PassesExams(x) -> Happy(x) === ¬PassesExams(x) v Happy(x)
        Clause c1 = new Clause();
        c1.addLiteral(new Literal("PassesExams", "x", false));
        c1.addLiteral(new Literal("Happy", "x", true));
        clauses.add(c1);

        // 2. Studies(x) -> PassesExams(x) === ¬Studies(x) v PassesExams(x)
        Clause c2 = new Clause();
        c2.addLiteral(new Literal("Studies", "x", false));
        c2.addLiteral(new Literal("PassesExams", "x", true));
        clauses.add(c2);

        // 3. ¬Sleepy(x) -> Studies(x) === Sleepy(x) v Studies(x)
        Clause c3 = new Clause();
        c3.addLiteral(new Literal("Sleepy", "x", true));
        c3.addLiteral(new Literal("Studies", "x", true));
        clauses.add(c3);

        // 4. DrinksCoffee(x) -> ¬Sleepy(x) === ¬DrinksCoffee(x) v ¬Sleepy(x)
        Clause c4 = new Clause();
        c4.addLiteral(new Literal("DrinksCoffee", "x", false));
        c4.addLiteral(new Literal("Sleepy", "x", false));
        clauses.add(c4);

        // 5. DrinksCoffee(Jack)
        Clause c5 = new Clause();
        c5.addLiteral(new Literal("DrinksCoffee", "Jack", true));
        clauses.add(c5);

        // 6. LikesSubject(x) -> Motivated(x) === ¬LikesSubject(x) v Motivated(x)
        Clause c6 = new Clause();
        c6.addLiteral(new Literal("LikesSubject", "x", false));
        c6.addLiteral(new Literal("Motivated", "x", true));
        clauses.add(c6);

        // 7. Motivated(x) -> Studies(x) === ¬Motivated(x) v Studies(x)
        Clause c7 = new Clause();
        c7.addLiteral(new Literal("Motivated", "x", false));
        c7.addLiteral(new Literal("Studies", "x", true));
        clauses.add(c7);

        // 8. LikesSubject(Jack)
        Clause c8 = new Clause();
        c8.addLiteral(new Literal("LikesSubject", "Jack", true));
        clauses.add(c8);

        // 9. PassesExams(x) -> GoodGrade(x) === ¬PassesExams(x) v GoodGrade(x)
        Clause c9 = new Clause();
        c9.addLiteral(new Literal("PassesExams", "x", false));
        c9.addLiteral(new Literal("GoodGrade", "x", true));
        clauses.add(c9);

        // 10. GoodGrade(x) -> Celebrates(x) === ¬GoodGrade(x) v Celebrates(x)
        Clause c10 = new Clause();
        c10.addLiteral(new Literal("GoodGrade", "x", false));
        c10.addLiteral(new Literal("Celebrates", "x", true));
        clauses.add(c10);

        // 11. Celebrates(x) -> Happy(x) === ¬Celebrates(x) v Happy(x)
        Clause c11 = new Clause();
        c11.addLiteral(new Literal("Celebrates", "x", false));
        c11.addLiteral(new Literal("Happy", "x", true));
        clauses.add(c11);

        // 12. Happy(x) -> HasFun(x) === ¬Happy(x) v HasFun(x)
        Clause c12 = new Clause();
        c12.addLiteral(new Literal("Happy", "x", false));
        c12.addLiteral(new Literal("HasFun", "x", true));
        clauses.add(c12);


        // Negation of conclusion: ¬Happy(Jack)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("Happy", "Jack", false));

        return new Example(clauses, negatedConclusion);

    }

    public Example largeExample() {
        List<Clause> clauses = new ArrayList<>();

        // Main chain: P1(x) -> P2(x) -> ... -> P50(x)
        for (int i = 1; i < 50; i++) {
            Clause clause = new Clause();
            clause.addLiteral(new Literal("P" + i, "x", false));
            clause.addLiteral(new Literal("P" + (i + 1), "x", true));
            clauses.add(clause);
        }
        Clause p1 = new Clause();
        p1.addLiteral(new Literal("P1", "BigTest", true));
        clauses.add(p1);

        // Distractor chain 1: Q1(x) -> ... -> Q10(x)
        for (int i = 1; i < 10; i++) {
            Clause clause = new Clause();
            clause.addLiteral(new Literal("Q" + i, "x", false));
            clause.addLiteral(new Literal("Q" + (i + 1), "x", true));
            clauses.add(clause);
        }
        Clause q1 = new Clause();
        q1.addLiteral(new Literal("Q1", "BigTest", true));
        clauses.add(q1);

        // Distractor chain 2: R1(x) -> ... -> R20(x)
        for (int i = 1; i < 40; i++) {
            Clause clause = new Clause();
            clause.addLiteral(new Literal("R" + i, "x", false));
            clause.addLiteral(new Literal("R" + (i + 1), "x", true));
            clauses.add(clause);
        }
        Clause r1 = new Clause();
        r1.addLiteral(new Literal("R1", "BigTest", true));
        clauses.add(r1);

        // Shuffle all clauses randomly to better demonstrate a more complex problem
        Collections.shuffle(clauses, new Random(33));


        // Negation of conclusion: ¬P50(BigTest)
        Clause negatedConclusion = new Clause();
        negatedConclusion.addLiteral(new Literal("P50", "BigTest", false));
        return new Example(clauses, negatedConclusion);
    }

    public class Example {
        List<Clause> clauses;
        Clause negation;

        public Example(List<Clause> clauses, Clause negation) {
            this.clauses = clauses;
            this.negation = negation;
        }

        public void runExample() throws InterruptedException {
            System.out.println(this);

            // Run ResolutionTheoremProver
            long startTimeSingle = System.currentTimeMillis();
            ResolutionTheoremProver singleResolver = new ResolutionTheoremProver(clauses);
            boolean singleResult = singleResolver.prove(negation);
            long endTimeSingle = System.currentTimeMillis();
            long singleTime = endTimeSingle - startTimeSingle;
            System.out.println("SingleThreadResolver: " + singleResult + " (Time: " + singleTime + "ms)");

            // Run MultiThreadResolver
            long startTimeMulti = System.currentTimeMillis();
            MultiThreadedResolver multiResolver = new MultiThreadedResolver(clauses);
            boolean multiResult = multiResolver.prove(negation);
            long endTimeMulti = System.currentTimeMillis();
            long multiTime = endTimeMulti - startTimeMulti;


            // Print results
            System.out.println("\nResults:");
            System.out.println("MultiThreadResolver: " + multiResult + " (Time: " + multiTime + "ms)");
            System.out.println("ResolutionTheoremProver: " + singleResult + " (Time: " + singleTime + "ms)");
            System.out.println("Difference: " + Math.abs(multiTime - singleTime) + "ms");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Clauses: \n");
            for (int i = 0; i < clauses.size(); i++) {
                sb.append("  ").append(i + 1).append(": ").append(clauses.get(i)).append("\n");
            }
            sb.append("Negation").append(negation);
            return sb.toString();
        }
    }
}
