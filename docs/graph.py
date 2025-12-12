import pandas as pd
import matplotlib.pyplot as plt

# Load the CSV data
df = pd.read_csv("docs/results.csv")

# Plot Single Threaded and Multi Threaded times
plt.figure(figsize=(10, 6))
plt.plot(df["n"], df["Single Threaded(ms)"], marker="o", label="Single Threaded (ms)")
plt.plot(df["n"], df["Multi Threaded (ms)"], marker="o", label="Multi Threaded (ms)")
plt.xlabel("Number of Clauses")
plt.ylabel("Time (ms)")
plt.yscale("log", base=2)
plt.legend()
plt.grid(True)
plt.tight_layout()
plt.savefig("docs/performance_graph.png")


# plot the performacne imrovement over single threaded
df["improvement"] = df["Single Threaded(ms)"] / df["Multi Threaded (ms)"]
plt.figure(figsize=(10, 6))
plt.plot(df["n"], df["improvement"], marker="o")
plt.xlabel("Number of Clauses")
plt.ylabel("Improvement")
plt.grid(True)
plt.tight_layout()
plt.savefig("docs/performance_improvement.png")
