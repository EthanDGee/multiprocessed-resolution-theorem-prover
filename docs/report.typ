= Multithreaded Resolution Theorem Prover

== Limitations of RTP

The first and perhaps most glaring weakness is the complexity of the algorithm. (INSERT COMPLEXITY). This makes its usability for more complex tasks questionable. There are two key problem areas: the time it takes to run, and the size of the clauses.

To address this I implemented two key technologies.
1. *A local database* -  This allows for the total number of clauses to grow much higher than the limitations of the computer's RAM.
2. *Multithreading* - While this doesn't reduce the complexity of the algorithm itself, it does significantly speed up the process.


== Implementation

=== Multithreading

With the primary goal of this model being the introduction of multithreading, I made the decision to switch from Lisp to Java. This meant that I did have to reimplement the RTP, but it made the process of extending the prover far easier. I implemented a manager-worker hierarchy composed of two key classes: `MultiThreadedResolver` and `ProverThread`.

The `MultiThreadedResolver` acts as manager,It starts/stops threads and determines when the proof is complete. When the `prove` method is called, it initializes a thread pool with one `ProverThread` per available CPU core. It then enters a monitoring loop that checks for one of two termination conditions: the creation of the empty clause, (I did think of a silly little joke I thought you might like while writing this paper "Who comes down the chimney to give all the good mathematicians proofs for Christmas? The empty clause". It probably needs some work, but I though you would like it) or the system has reached saturation (all clauses have been marked resolved, and no threads are working). It relies on the use atomic variables to monitor this: `solutionFound` (an `AtomicBoolean`) and `activeWorkers` (an `AtomicInteger`). It also uses an additional `hasUnresolvedClauses()` given by the database class. When a proof is completed, the `MultiThreadedResolver` interrupts all worker threads to ensure a clean shutdown and then waits for them to terminate. Finally it returns `solutionFound`.

The `ProverThread` class contains the logic for the individual worker threads, and handles the actual resolution. Each thread runs a main loop that continues until a solution is found or it is interrupted by the coordinator. While running the it first requests a batch of unresolved clauses from the database by calling `database.getUnresolvedClauses()`. This has integrated blocking so that the thread waits until work is available. Once a batch is acquired, the thread signals to the coordinator that it is active by incrementing `activeWorkers` and begins the resolution process. It starts by looping in batches back to front from the last index in the unresolved batch to the start of the database. This back to front method of unification comes with the added benefit of prioritizing clause combinations that are the most likely to lead to new resolutiions. All resolvents are stored in a set and periodically saved back to the database once a threshold is reached (this is done to minimize i/o bottlenecks). If a thread generates an empty clause, it immediately notifies the `MultiThreadedResolver` by calling the `solutionWasFound()` method, which sets the atomic flag and triggers the system-wide shutdown.

=== Database

For the database, I chose SQLite for its simplicity, my own familiarity, serverless, and its lightwieght nature. This is perfect for a self-contained local application. It allows for the project to easilt overcome the primary challenge of concurrent databases accessed from multiple threads all while not requiring a seperate server. A lot of this is thatnks to SQLite's Write-Ahead Logging (WAL) mode. WAL allows multiple threads to reader access to the database simultaneously even while another thread is writing to it. This is perfect as resoltion theorm proving is a very read heavy, and write light algorithm. This alone gave a huge performance boost.

Clauses are stored in a single table with the schema `clauses(id INTEGER, clause TEXT UNIQUE, starting_set BOOLEAN, resolved BOOLEAN)`. To enforce the `UNIQUE` constraint and prevent storing duplicate clauses, I overwrote the toString for the `Clause` object. I also integrated a `ClauseParser` that can then return them back to their original form.

While WAL handles file-level locking by the SQLite server, it doesn't completely resolve all race conditions caused by the sorrounding program. To solve this, I implemented a much  more fine-grained concurrency control strategy at the application level to manage the workflow of the `ProverThread`'s request. The `Database` class uses Java's concurrency utilities to create a producer-consumer pattern. A single `ReentrantLock` protects all read and write operations, guaranteeing that database transactions (like adding a batch of new resolvents) are atomic. Furthermore, a `Condition` variable (`hasNewClauses`) is paired with the lock. This allows the worker threads to wait efficiently for new work without the need for constantly polling the database for new unresolved clauses. When a `ProverThread` requests new clauses and finds none, it calls `await()` on the condition, putting it to sleep without consuming CPU cycles. When another thread adds new resolvents to the database, it calls `signalAll()` on the condition, waking up all waiting threads to resume processing.
== challenges

=== Tuning the Multithreading Hyper Parameters

And here I was thinking that I could get away from hyperparameter tuning. While this is not exactly the same thing, the sizes of the various parameters for the Multi-Threaded Resolver did have major impacts on performance. Tuning it for best performance was a delicate balance of reducing the amount of interactions with the database, and minimizing the amount of thread starvation.

There are three key variables that were at the root of this: `UNRESOLVED_BATCH_SIZE`, which determines the max amount of unresolved clauses handled by each thread; `CLAUSE_BATCH_SIZE`, which controls the number of clauses that are compared with the unresolved at each stage; and finally, `RESOLVENT_SAVE_THRESHOLD`, which controls how many unique resolutions must be made before they are saved to the database.

```java
  // Database Constants
    public static final int CLAUSE_BATCH_SIZE = 300;
    public static final int UNRESOLVED_BATCH_SIZE = 100;
    public static final int RESOLVENT_SAVE_THRESHOLD = 100;
```
== Results
=== Benchmarking Solution

There currently exists no common benchmark for RTP algorithms. This is for a variety of reasons, but most importantly, comparing speed isn't too common of a problem. RTP is more result-oriented than anything else, so performance isn't as sought after of a metric. So with that being said, I created the following function that generates a set of clauses of size 'n'. It works by creating a chain of clauses that all reduce into each other so that we can guarantee there are resolutions that result from the process. We then add one negated conclusion to serve as our final case; this is what completes the chain. Critically, in order to better mimic real world performance, the clauses are shuffled.

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

All of the below benchmarks were done on my laptop running Linux Mint 25.1 Zara with 13th Gen Intel(R) Core(TM) i7-1360P CPU using all 16 threads. The program runs using all available processors by default. I tested the runtime of both the single-threaded and multithreaded resolvers on clause sizes from 5, 10, 125. Each test was run 5 times to ensure consistency, and the average result is the one reported. 125 was selected as the stopping point as it has an extremely high number of resolved threads and high complexity level without being overly extreme to the point that averaging along with calculating the preceding numbers would become too time intensive. With all that being said, these tests took around 90 minutes to run.

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

As expected in the earlier stages, single-threaded performance had the lead in the earlier stages. This is due to the far higher time in initial setup and overhead. But as complexity increases, the multithreaded outperforms the single-threaded in performance, notably coming to a near tie at a complexity of 25.

The interesting thing that I didn't expect is that the multithreaded's improvement over the single-threaded doesn't level off at around 16x, which comes from the dividing of labor. Instead, it continues past that point, reaching an astonishing 70x improvement over base performance at a clause size of 125. This is likely due to a combination of things. This is likely primarily caused by the fact that `ProverThread`s go from the back of the pool to the front. So in addition to the performance benefit of splitting up the labor, they start their deductions focusing on resolving with previous resolvents. These are far more valuable calculations; as the complexity of a set grows, the likelihood that the solution is going to come from deeper resolvents also increases. It's also likely partially due to the tests themselves. The benchmark I created is unwittingly very much focused on deeper levels of resolution.

#figure(
  image("performance_improvement.png", width: 75%),
  caption: [Performance Improvement of Multi Threaded Resolver over Single Threaded Resolver],
)

== Future work

The manual testing of different combinations of "hyperparameters" was extremely slow and manual, and I really feel like there is some real room for improvement. I also don't feel entirely confident in my final result; there are also a variety of other parameters I didn't explore, such as number of threads. This decision making under uncertainty combined with a definite target (runtime) seems like a strong candidate for a reinforcement learning model to optimize this process so that the resolver performs optimally under any set of clauses.

My benchmark wasn't as exhaustive as I'd like, and the worth of a system is dependent on its real world performance, so it would be extremely useful to expand my benchmarks to include some real world problems in addition to the `nSizedExample` approach.

Finally, the use of a database for a locally computed resolution theorem prover might seem a little strange, and there's good reason for that. There aren't major reasons to do this when we are dealing with disjoint pools that are measured in the thousands. However, one purpose that it does provide is in building the framework for a much larger resolution theorem prover for more complex discoveries. This whole system essentially works as a prototype for a distributed-compute-based resolution theorem prover.

== Conclusions
