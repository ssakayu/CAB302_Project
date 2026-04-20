PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username TEXT NOT NULL UNIQUE,
                                     email TEXT NOT NULL UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     created_at TEXT DEFAULT (datetime('now'))
    );

CREATE TABLE IF NOT EXISTS preferences (
                                           user_id INTEGER PRIMARY KEY,
                                           theme TEXT DEFAULT 'dark',
                                           enabled_widgets TEXT DEFAULT 'timer,todo,notes,calendar,music',
                                           focus_minutes INTEGER DEFAULT 25,
                                           short_break_minutes INTEGER DEFAULT 5,
                                           long_break_minutes INTEGER DEFAULT 15,
                                           widget_x REAL DEFAULT 100,
                                           widget_y REAL DEFAULT 100,
                                           widget_opacity REAL DEFAULT 1.0,
                                           FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );