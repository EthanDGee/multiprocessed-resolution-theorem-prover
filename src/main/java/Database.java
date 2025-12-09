import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Database {
    private String DB_PATH;

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

        // create the clauses table
        String sql = "CREATE TABLE IF NOT EXISTS clauses (id INTEGER PRIMARY KEY AUTOINCREMENT, clause TEXT UNIQUE, starting_set BOOLEAN DEFAULT FALSE,resolved BOOLEAN DEFAULT FALSE)";
        clearClauses();
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        // fill clauses table with clauses
        String[] clauseStrings = new String[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            clauseStrings[i] = clauses.get(i).toString();
        }
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO clauses (clause, starting_set) VALUES (?, ?)");
            for (String clauseString : clauseStrings) {
                stmt.setString(1, clauseString);
                stmt.setBoolean(2, true);
                stmt.addBatch();
            }
            stmt.executeBatch();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void addClause(Clause clause) {
        String clauseString = clause.toString();

        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO clauses (clause) VALUES (?)");
            stmt.setString(1, clauseString);
            stmt.executeUpdate();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addClauses(List<Clause> clauses) {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO clauses (clause) VALUES (?)");

            for (Clause clause : clauses) {
                pstmt.setString(1, clause.toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<Clause> getClauses(int startingIndex, int amount) {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement pstmt = conn.prepareStatement("SELECT id, clause FROM clauses WHERE id >= ? LIMIT ?");
            pstmt.setInt(1, startingIndex);
            pstmt.setInt(2, amount);
            ResultSet results = pstmt.executeQuery();
            ArrayList<Clause> clauses = new ArrayList<>();

            while (results.next()) {
                Clause new_clause = ClauseParser.parseClause(results.getString("clause"));
                new_clause.setId(results.getInt("id"));
                clauses.add(new_clause);
            }
            pstmt.close();
            conn.close();

            return clauses;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public ArrayList<Clause> getUnresolvedClauses(int amount) {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement pstmt = conn
                    .prepareStatement("SELECT id, clause FROM clauses WHERE resolved is FALSE LIMIT ?");
            pstmt.setInt(1, amount);
            ResultSet results = pstmt.executeQuery();
            ArrayList<Clause> clauses = new ArrayList<>();
            while (results.next()) {
                Clause new_clause = ClauseParser.parseClause(results.getString("clause"));
                new_clause.setId(results.getInt("id"));
                clauses.add(new_clause);
            }
            pstmt.close();
            conn.close();

            return clauses;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
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

        try (Connection conn = DriverManager.getConnection(DB_PATH);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < clauseIds.length; i++) {
                pstmt.setInt(i + 1, clauseIds[i]);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean hasEmptyClause() {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery("SELECT id FROM clauses WHERE clause like 'nil' LIMIT 1");
            boolean hasEmpty = results.next(); // returns true if there is at least one result
            stmt.close();
            conn.close();
            return hasEmpty;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void flushResolvents() {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM clauses where starting_set = FALSE");
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void clearClauses() {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DELETE FROM clauses");
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public int countClauses() {
        int count = 0;
        try (Connection conn = DriverManager.getConnection(DB_PATH);
                Statement stmt = conn.createStatement();
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
