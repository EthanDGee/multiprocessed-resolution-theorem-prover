import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Database {
    private final String DB_PATH = "jdbc:sqlite:db.sqlite3";

    public Database(List<Clause> clauses) {

        // create the clauses table
        String sql = "CREATE TABLE IF NOT EXISTS clauses (id INTEGER PRIMARY KEY AUTOINCREMENT, clause TEXT, starting_set BOOLEAN DEFAULT FALSE,resolved BOOLEAN DEFAULT FALSE)";

        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        //fill clauses table with clauses
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
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<Clause> get_clauses(int starting_index, int amount) {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            ResultSet results = stmt
                    .executeQuery("SELECT clause FROM clauses WHERE id >= " + starting_index + "LIMIT " + amount);
            ArrayList<Clause> clauses = new ArrayList<>();

            while (results.next()) {
                clauses.add(ClauseParser.parseClause(results.getString("clause")));
            }
            stmt.close();
            conn.close();

            return clauses;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public ArrayList<Clause> get_unresolved_clauses(int amount) {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery("SELECT clause FROM clauses WHERE resolved is FALSE LIMIT " + amount);
            ArrayList<Clause> clauses = new ArrayList<>();
            while (results.next()) {
                clauses.add(ClauseParser.parseClause(results.getString("clause")));
            }
            stmt.close();
            conn.close();

            return clauses;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void set_resolved(int[] clause_ids) {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            PreparedStatement pstmt = conn.prepareStatement("UPDATE clauses SET resolved = TRUE WHERE id IN (?)");
            pstmt.setString(1, Arrays.toString(clause_ids));
            pstmt.executeUpdate();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean has_empty_clause() {
        try {
            Connection conn = DriverManager.getConnection(DB_PATH);
            Statement stmt = conn.createStatement();
            ResultSet results = stmt.executeQuery("SELECT id FROM clauses WHERE clause like 'nil' LIMIT 1");
            stmt.close();
            conn.close();
            return results.next(); // returns true if there is at least one result
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
}

