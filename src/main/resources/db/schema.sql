PRAGMA foreign_keys = ON;

-- USERS
CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     username TEXT NOT NULL UNIQUE,
                                     email TEXT NOT NULL UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     created_at TEXT DEFAULT (datetime('now'))
    );

-- PREFERENCES (1 row per user)
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

-- TASKS
CREATE TABLE IF NOT EXISTS tasks (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     user_id INTEGER NOT NULL,
                                     title TEXT NOT NULL,
                                     is_done INTEGER DEFAULT 0,
                                     priority TEXT DEFAULT 'medium', -- low/medium/high
                                     created_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- NOTES
CREATE TABLE IF NOT EXISTS notes (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     user_id INTEGER NOT NULL,
                                     content TEXT NOT NULL,
                                     created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- OPTIONAL: link notes to tasks
CREATE TABLE IF NOT EXISTS task_notes (
                                          task_id INTEGER NOT NULL,
                                          note_id INTEGER NOT NULL,
                                          PRIMARY KEY (task_id, note_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
    );

-- POMODORO SESSIONS
CREATE TABLE IF NOT EXISTS pomodoro_sessions (
                                                 id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                 user_id INTEGER NOT NULL,
                                                 focus_minutes INTEGER NOT NULL,
                                                 completed_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- OAUTH TOKENS
CREATE TABLE IF NOT EXISTS oauth_tokens (
                                            user_id INTEGER NOT NULL,
                                            provider TEXT NOT NULL,
                                            access_token TEXT NOT NULL,
                                            refresh_token TEXT,
                                            token_type TEXT,
                                            scope TEXT,
                                            expires_at TEXT,
                                            PRIMARY KEY (user_id, provider),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_tasks_user ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_user ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_pomodoro_user ON pomodoro_sessions(user_id);
