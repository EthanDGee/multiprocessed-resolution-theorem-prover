import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Database {

    private int lastRetrieved;
    private String DB_PATH;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition hasNewClauses = lock.newCondition();
    private Connection conn;
    private int lastId;

    public Database(List<Clause> clauses) {
        this(clauses, "jdbc:sqlite:db.sqlite3");
    }

    public Database(List<Clause> clauses, String dbPath) {
        if (clauses == null || clauses.isEmpty()) {
            throw new IllegalArgumentException("clauses cannot be null or empty");
        }
        if (dbPath == null || dbPath.isEmpty()) {
            throw new IllegalArgumentException("dbPath cannot be null or empty");
        }

        this.DB_PATH = dbPath;

        try {
            this.conn = DriverManager.getConnection(DB_PATH);
            // create the clauses table
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS clauses" +
                                "(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "clause TEXT UNIQUE, " +
                                "starting_set BOOLEAN DEFAULT FALSE," +
                                "resolved BOOLEAN DEFAULT FALSE)");
                // Enable WAL mode
                stmt.executeUpdate("PRAGMA journal_mode=WAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to the database", e);
        }

        clearClauses();

        // fill clauses table with clauses
        addClauses(clauses, true);
        lastRetrieved = getFirstId();
        lastId = getLastId();
    }

    public void close() {
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void addClause(Clause clause) {
        lock.lock();
        try {
            String clauseString = clause.toString();
            try (PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO clauses (clause) VALUES (?)")) {
                stmt.setString(1, clauseString);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            lastId = getLastId(); // we can't use ++ as insertion may be ignored
            hasNewClauses.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void addClauses(List<Clause> clauses) {
        addClauses(clauses, false);
    }

    private void addClauses(List<Clause> clauses, boolean isStartingSet) {
        lock.lock();
        try (PreparedStatement pstmt = conn
                .prepareStatement("INSERT OR IGNORE INTO clauses (clause, starting_set) VALUES (?,?)")) {
            conn.setAutoCommit(false);
            for (Clause clause : clauses) {
                pstmt.setString(1, clause.toString());
                pstmt.setBoolean(2, isStartingSet);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            lastId = getLastId();// we can't use simple + as some insertions may be ignored
            conn.commit();
            hasNewClauses.signalAll();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            }
            lock.unlock();
        }
    }

    public ArrayList<Clause> getClauses(int startingIndex, int amount) {
        ArrayList<Clause> clauses = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id, clause FROM clauses WHERE id >= ? LIMIT ?")) {
            pstmt.setInt(1, startingIndex);
            pstmt.setInt(2, amount);
            try (ResultSet results = pstmt.executeQuery()) {
                while (results.next()) {
                    Clause new_clause = ClauseParser.parseClause(results.getString("clause"));
                    new_clause.setId(results.getInt("id"));
                    clauses.add(new_clause);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return clauses;
    }

    public ArrayList<Clause> getUnresolvedClauses(int amount) throws InterruptedException {
        ArrayList<Clause> clauses = new ArrayList<>();
        try {
            lock.lock();
            while (lastRetrieved >= lastId) {
                hasNewClauses.await();
            }
            try (PreparedStatement pstmt = conn
                    .prepareStatement("SELECT id, clause FROM clauses WHERE resolved is FALSE AND id >= ? LIMIT ?")) {
                pstmt.setInt(1, lastRetrieved);
                pstmt.setInt(2, amount);
                try (ResultSet results = pstmt.executeQuery()) {
                    while (results.next()) {
                        Clause new_clause = ClauseParser.parseClause(results.getString("clause"));
                        new_clause.setId(results.getInt("id"));
                        clauses.add(new_clause);
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }

            if (!clauses.isEmpty()) {
                lastRetrieved = clauses.getLast().getId() + 1;
            }
        } finally {
            lock.unlock();
        }
        return clauses;
    }

    public void setResolved(List<Clause> clauses) {
        if (clauses == null || clauses.isEmpty()) {
            return;
        }

        int[] clauseIds = new int[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            clauseIds[i] = clauses.get(i).getId();
        }

        String sql = "UPDATE clauses SET resolved = TRUE WHERE id IN ("
                + String.join(",", Collections.nCopies(clauseIds.length, "?")) + ")";

        lock.lock();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < clauseIds.length; i++) {
                pstmt.setInt(i + 1, clauseIds[i]);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public boolean hasEmptyClause() {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM clauses WHERE clause like ? LIMIT 1")) {
            stmt.setString(1, Constants.EMPTY_CLAUSE);
            try (ResultSet results = stmt.executeQuery()) {
                return results.next(); // returns true if there is at least one result
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public boolean hasUnresolvedClauses() {
        lock.lock();
        try {
            return lastRetrieved <= lastId;
        } finally {
            lock.unlock();
        }
    }

    public void flushResolvents() {
        lock.lock();
        try {
            // clear all clauses not in the starting set;
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM clauses where starting_set = FALSE");

                // reset starting set resolved to false
                stmt.executeUpdate("UPDATE clauses SET resolved = FALSE WHERE starting_set = TRUE");

                // Optimize and vacuum database
                stmt.executeUpdate("VACUUM");
                stmt.executeUpdate("PRAGMA optimize");
                stmt.executeUpdate("PRAGMA wal_checkpoint(TRUNCATE)");
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
            // reset lastRetrieved index to first index in the database;
            lastRetrieved = getFirstId();
            lastId = getLastId();
            hasNewClauses.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void clearClauses() {
        lock.lock();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM clauses");
            // reset autoincrement
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='clauses'");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private int getFirstId() {
        try (Statement stmt = conn.createStatement();
             ResultSet result = stmt.executeQuery("SELECT id FROM clauses ORDER BY id LIMIT 1")) {
            if (result.next()) {
                return result.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    private int getLastId() {
        try (Statement stmt = conn.createStatement();
             ResultSet result = stmt.executeQuery("SELECT id FROM clauses ORDER BY id DESC  LIMIT 1")) {
            if (result.next()) {
                return result.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }

    public int countClauses() {
        int count = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM clauses")) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return count;
    }
}
