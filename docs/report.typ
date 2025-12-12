= Multithreaded Resolution Theorem Prover

== Background

I expanded the resolution theorem prover discussed in class to improve its scalability. The original system was resource-intensive with high time and space complexities, limiting its viability for complex tasks. To address these issues without altering the fundamental algorithm, I implemented two key enhancements: storing the disjunction pool in a database and incorporating multiprocessing.

By using a database for the disjunction pool, I exceeded RAM limitations. Storage is cheaper, more scalable, and not constrained by CPU memory limits, broadening access to the tool. While this introduced I/O overhead, it significantly increased the maximum size of the disjunction pool on consumer and professional hardware.

Multiprocessing reduced runtime through parallelization. A one-to-many architecture with batch handling allowed multiple processes to work concurrently, enhancing speed without compromising the soundness of the proofs.

== Limitations of RTP

The first and perhaps most glaring weakness is both the complexity of the algorithm. (INSERT COMPLEXITY). This makes the usability of it for more complex tasks questionable. There are two key problem areas; the time it takes to run, and the size of the clauses.

To address this I implemented two key technologies.
1. *A local database* -  This allows for the total number of clauses to grow much higher than the limitations of the computers RAM.
2. *Multithreading* - While this doesn't reduce the complexity of the algorithm itself it does significantly speed up the process.


== Implementation

=== Multithreading

With the primary goal of this model being the introduction of multithreading, I made the decision to switch from lisp to java. This meant that I did have to reimplement the RTP, but it made the process of extending the prover far easier.

I implemented using a manager worker hierarchy, I created a created a management class called `MultiThreadedResolver` this is in charge of starting/stopping all of the threads, checking to see if the proof failed (none of the threads are running, there's no unresolved items in the database, and the there is no empty clause) and initialzing the database.

In addition to `MultiThreadedResolver`, I also created `ProverThread`. It handles all of the actual resolving started with taking my base reimplementation of the RTP and


=== Database

- why sqlite? and WAL

- clause storing and general structure

- concurrency hadnling

-

== challenges

=== Tuning the Multithreading Hyper Parameters

And here I was thinking that I could get away from hyperparameter tuning. While this is not exactly the same thing, the sizes of the various parameters for the Multi-Threaded Resolver did have major impacts on performance. Tuning it for best performance was a delicate balance of reducing the amount of interactions with the datbase, and minimizing the amount of thread starvation.

There are three key variable that were at the root of this `UNRESOLVED_BATCH_SIZE` which determines the max amount of unresolved clauses handled by each thread, `CLAUSE_BATCH_SIZE` which controls the number of clauses that are compared with the unresolved at each stage, and finally, `RESOLVENT_SAVE_THRESHOLD` which controls how many unique resolutions must be made before they are saved to the database.




```java
  // Database Constants
    public static final int CLAUSE_BATCH_SIZE = 300;
    public static final int UNRESOLVED_BATCH_SIZE = 100;
    public static final int RESOLVENT_SAVE_THRESHOLD = 100;
```
== Results
=== Benchmarking Solution

There currently exists no common benchmark for RTP algorithms. This is for a variety of reasons, but most importantly comparing speed isn't too common of a problem. RTP is more result oriented than anything else, so perfrormance isn't as saught after of a metric. So with that being said I created the following function that generates a set of clause of size \'n\'. It works be creating a chain of clauses that all reduce into each other so that we can guarantee there are resolutions that result from the process. We then add one negated conclusion to serve as our final case, this is what completes the chain. Critically in order to better mimic real world performance the clauses are shuffled.

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

    // Negation of conclusion: ¬Pn(x) completing the chain causing the empty clause
    Clause negatedConclusion = new Clause();
    negatedConclusion.addLiteral(new Literal("P" + n, "x", false));

    return new Example(clauses, negatedConclusion);
}
```
All of the below bench marks were done on my laptop running Linux Mint 25.1 Zara with 13th Gen Intel(R) Core(TM) i7-1360P CPU using all 16 threads. The program runs using all available processors by default. I tested the runtime of both the single threaded and multithreaded resolvers on clause sizes from 5, 10, 125. Each test was ran 5 times to ensure consistency and th average result is the one reported.125 was selected as the stopping point as it has an extremely high number of resolved threads, and high complexity level without being overly extreme to the point that averaging along with calculating the preceding numbers would become too time intensive. With all that being said these tests took around 90 minutes run.

#figure(
  // align: center,
  caption: [Average run time (ms) of the model at various Disjoint Set Sizes after 5 runs],
  table(
    columns: 4,
    table.header([ *\# of Clauses* ], [*Single Threaded (ms)*], [*Multithreaded(ms)*], [*x Improvement*]),
    [ $ 5 $ ], [ $ 37 $ ], [ $ 76 $ ], [$ #(calc.floor(37 / 76 * 100) / 100) $ ],
    [ $ 10 $ ], [ $ 60 $ ], [ $ 112 $ ], [$ #(calc.floor(60 / 112 * 100) / 100) $ ],
    [ $ 25 $ ], [ $ 162 $ ], [ $ 160 $ ], [$ #(calc.floor(162 / 160 * 100) / 100) $ ],
    [ $ 50 $ ], [ $ 1481 $ ], [ $ 278 $ ], [$ #(calc.floor(1478 / 278 * 100) / 100) $ ],
    [ $ 75 $ ], [ $ 17660 $ ], [ $ 1363 $ ], [$ #(calc.floor(17660 / 1363 * 100) / 100) $ ],
    [ $ 100 $ ], [ $ 82973 $ ], [ $ 1969 $ ], [$ #(calc.floor(82973 / 1969 * 100) / 100) $ ],
    [ $ 125 $ ], [ $ 349778 $ ], [ $ 4950 $ ], [$ #(calc.floor(349778 / 4950 * 100) / 100) $ ],
  ),
)

#figure(
  caption: [Average Runtimes (ms) of the Single Threaded vs Multithreaded RTPs],
  image("performance_graph.png", width: 75%),
)
=== Interpreting Results

As expected in the earlier stages, single threaded performance had the lead in the earlier stages, this is due to the far higher time in initial set up and overhead. But as complexity increases the multithreaded outperforms the single threaded in performance, notably coming to a near tie at a complexity of 25.

The interesting thing that I didn't expect is that the multithreadeds improvement over the doesn't level off at around 16x that comes from the dividing of labor. Instead it continues past that point reaching an astonishing 70x improvement over base performance at a clause size of 125. This is likely due to a combination of things. This is likely primarily causes by the fact that `ProverThread`s go from the back of the pool to the front. So in addition to the perfromance benifit of splitting up the labor. They start their deductions focusing on resolving with previous resolvents. These are far more valuable calcaulations as the complexity of a set grows the likelihood that the solution is going to come from deeper resolvent also increases. Its also likley partially due to the tests themselves. The benchmark I created Is unwittingly very much focused on deeper levels of resolution.


#figure(
  image("performance_improvement.png", width: 75%),
  caption: [Performance Improvement of Multi Threaded Resolver over Single Threaded Resolver],
)



== Future work

- create simple reinforcement learning linear regression to optimize the parameters of the model for faster performance.

- analyze performance across various number of cpus

- Real world performance by using legitimate use cases as tests


== Conclusions
