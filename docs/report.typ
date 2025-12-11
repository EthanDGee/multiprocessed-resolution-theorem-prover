= Multithreaded Resolution Theorem Prover


== Background

I expanded the resolution theorem prover discussed in class to improve its scalability. The original system was resource-intensive with high time and space complexities, limiting its viability for complex tasks. To address these issues without altering the fundamental algorithm, I implemented two key enhancements: storing the disjunction pool in a database and incorporating multiprocessing.

By using a database for the disjunction pool, I exceeded RAM limitations. Storage is cheaper, more scalable, and not constrained by CPU memory limits, broadening access to the tool. While this introduced I/O overhead, it significantly increased the maximum size of the disjunction pool on consumer and professional hardware.

Multiprocessing reduced runtime through parallelization. A one-to-many architecture with batch handling allowed multiple processes to work concurrently, enhancing speed without compromising the soundness of the proofs.

The implementation used Java with SQLite for database management and multiprocessing support, chosen over the original Lisp environment for better tooling.

Deliverables included:
- A working database with file-based disjunction loading
- Multiprocessed batch unification
- Performance graphs comparing scalability on the Towers of Hanoi problem against the standard implementation

== Limitations of RTP

The first and perhaps most glaring weakness is both the complexity of the algorithm. (INSERT COMPLEXITY). This makes the usability of it for more complex tasks questionable. There are two key problem areas; the time it takes to run, and the size of the clauses.

To address this I implemented two key technologies.
1. A local database -  This allows for the total number of clauses to grow much higher than the limitations of the computers RAM.
2. Multithreading - While this doesn't reduce the complexity of the algoirthm itse;


== Results
=== Benchmarking Solution

There currently exists no common benchmark for RTP algorithms. This is for a variety of reasons, but most importantly comparing speed isn't too common of a problem. RTP is more result oriented than anything else, so perfrormance isn't as saught after of a metric. So with that being said I created the following function that generates a set of clause of size \'n\'. It works be creating a chain of clauses that all reduce into each other so that we can guarantee there are resolutions that result from the process. We then add one negated conclusion to serve as our final case. Critically in order to better mimic real world performance the clauses are shuffled.

```java
public Example nSizedExample(int n) {
    List<Clause> clauses = new ArrayList<>();
    // Generate chain P1 -> P2 -> ... -> Pn/
    for (int i = 1; i < n; i++) {
        Clause clause = new Clause();
        clause.addLiteral(new Literal("P" + i, "x", false));
        clause.addLiteral(new Literal("P" + (i + 1), "x", true));
        clauses.add(clause);
    }
    // Add starting clause P1(x)
    Clause startClause = new Clause();
    startClause.addLiteral(new Literal("P1", "x", true));
    clauses.add(startClause);

    // Shuffle all clauses randomly to better demonstrate a more complex problem
    Collections.shuffle(clauses, new Random(33));

    // Negation of conclusion: ¬Pn(x)
    Clause negatedConclusion = new Clause();
    negatedConclusion.addLiteral(new Literal("P" + n, "x", false));

    return new Example(clauses, negatedConclusion);
}
```


=== Interpreting Results

One clear trend immmedietely shows up in h


#figure(
  // align: center,
  caption: [Comparitive run time (ms) of the model at various Disjoint Set Sizes],
  table(
    columns: 4,
    table.header([ *\# of Clauses* ], [*Single Threaded (ms)*], [*Multithreaded(ms)*], [*x Improvement*]),
    [ $ 5 $ ], [ $ 37 $ ], [ $ 76 $ ], [$ #(calc.floor(37 / 76 * 100) / 100) $ ],
    [ $ 10 $ ], [ $ 39 $ ], [ $ 69 $ ], [$ #(calc.floor(39 / 69 * 100) / 100) $ ],
    [ $ 25 $ ], [ $ 107 $ ], [ $ 113 $ ], [$ #(calc.floor(107 / 113 * 100) / 100) $ ],
    [ $ 50 $ ], [ $ 1046 $ ], [ $ 172 $ ], [$ #(calc.floor(1046 / 172 * 100) / 100) $ ],
    [ $ 75 $ ], [ $ 10601 $ ], [ $ 450 $ ], [$ #(calc.floor(10601 / 450 * 100) / 100) $ ],
    [ $ 100 $ ], [ $ 63905 $ ], [ $ 3708 $ ], [$ #(calc.floor(63905 / 3708 * 100) / 100) $ ],
  ),
)

#figure(
  caption: [Runtimes of the Single Threaded and Multithreaded RTPs],
  image("performance_graph.png"),
)
