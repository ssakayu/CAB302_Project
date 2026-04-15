import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbTest {
    public static void main(String[] args) throws Exception {
        // This will CREATE the file if it doesn't exist:
        String url = "jdbc:sqlite:data/focusdesk.db";

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {

            st.execute("PRAGMA foreign_keys = ON;");

            // Create 1 table to prove DB works
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    created_at TEXT DEFAULT (datetime('now'))
                );
            """);
        }

        System.out.println("Database created/updated at data/focusdesk.db");
    }
}