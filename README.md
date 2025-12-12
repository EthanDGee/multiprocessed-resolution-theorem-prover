# Multi-Threaded Resolution Theorem Prover

This project provides a Java-based implementation of a resolution theorem prover that can be run in either a single-threaded or multi-threaded mode. It is designed for scalability, using a central SQLite database to manage a large knowledge base of logical clauses and to coordinate work between multiple prover threads.

This approach demonstrates how resolution theorem provers can be scaled to handle problems that are too large to fit into memory and how their performance can be significantly improved by leveraging multi-core processors.

## Architecture and Scalability

The prover's scalability is achieved through two key architectural features:

1. **Database-Centric Design**: Instead of storing the entire knowledge base in memory, the prover uses an SQLite database. Clauses are read from and written to the database in batches. This allows the system to process a set of clauses much larger than the available RAM.

2. **Concurrent Processing**: The multi-threaded implementation (`MultiThreadedResolver`) uses a coordinator-worker pattern. A main thread spawns a pool of worker threads (`ProverThread`), which run in parallel. Each worker independently:
    - Fetches a set of unresolved clauses from the database.
    - Performs resolution steps on them.
    - Adds newly derived clauses back to the database.

The `Database` class is designed for concurrent access, using locks and condition variables to ensure that threads can safely and efficiently query and update the shared knowledge base. This design transforms the search for a proof into a parallel task, where multiple threads explore different parts of the search space simultaneously.

## Requirements

- **Java Development Kit (JDK) 21** or later.
- **Apache Maven** 3.6.0 or later.

## How to Run the Benchmark

The main entry point for this project is the `Benchmark` class, which runs a performance comparison between the single-threaded and multi-threaded provers on a sample problem.

To run the benchmark, execute the following Maven command from the project root directory:

```sh
mvn compile exec:java -Dexec.mainClass="Benchmark"
```

This will run both provers and print the time taken by each, clearly demonstrating the performance improvement of the multi-threaded approach.

## How to Run Each Prover Individually

You can also run the single-threaded and multi-threaded provers on their own.

### Single-Threaded Prover

The `ResolutionTheoremProver` class contains the single-threaded implementation. To run it, use the following command:

```sh
mvn compile exec:java -Dexec.mainClass="ResolutionTheoremProver"
```

### Multi-Threaded Prover

The `MultiThreadedResolver` class contains the multi-threaded implementation. To run it, use the following command:

```sh
mvn compile exec:java -Dexec.mainClass="MultiThreadedResolver"
```

