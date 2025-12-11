import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Database {

    private int lastRetrieved;
    private String DB_PATH;
    private ReentrantLock lock = new ReentrantLock();
    private Connection conn;

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
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS clauses (id INTEGER PRIMARY KEY AUTOINCREMENT, clause TEXT UNIQUE, starting_set BOOLEAN DEFAULT FALSE,resolved BOOLEAN DEFAULT FALSE)");
                // Enable WAL mode
                stmt.executeUpdate("PRAGMA journal_mode=WAL");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to the database", e);
        }

        clearClauses();

        // fill clauses table with clauses
        String[] clauseStrings = new String[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            clauseStrings[i] = clauses.get(i).toString();
        }
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO clauses (clause, starting_set) VALUES (?, ?)")) {
            for (String clauseString : clauseStrings) {
                stmt.setString(1, clauseString);
                stmt.setBoolean(2, true);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        lastRetrieved = getFirstId();
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
        String clauseString = clause.toString();

        try (PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO clauses (clause) VALUES (?)")) {
            stmt.setString(1, clauseString);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addClauses(List<Clause> clauses) {
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT OR IGNORE INTO clauses (clause) VALUES (?)")) {
            conn.setAutoCommit(false);
            for (Clause clause : clauses) {
                pstmt.setString(1, clause.toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
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

    public ArrayList<Clause> getUnresolvedClauses(int amount) {
        ArrayList<Clause> clauses = new ArrayList<>();
        lock.lock();
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id, clause FROM clauses WHERE resolved is FALSE AND id >= ? LIMIT ?")) {
            pstmt.setInt(1, lastRetrieved);
            pstmt.setInt(2, amount);
            try (ResultSet results = pstmt.executeQuery()) {
                while (results.next()) {
                    System.out.println("Retrieved clause:" + results.getString("clause"));
                    Clause new_clause = ClauseParser.parseClause(results.getString("clause"));
                    new_clause.setId(results.getInt("id"));
                    // update the lastRetrieved to reflect the last clause id
                    if (lastRetrieved < new_clause.getId())
                        lastRetrieved = new_clause.getId();

                    clauses.add(new_clause);
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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

    public void flushResolvents() {
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
    }

    public void clearClauses() {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM clauses");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
