
PRAGMA foreign_keys;

DROP TABLE IF EXISTS user_treasure;
DROP TABLE IF EXISTS treasure;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id INTEGER PRIMARY KEY,    
    email TEXT NOT NULL UNIQUE,
    token TEXT NOT NULL,
    admin INTEGER CHECK (admin == 0 || admin == 1) DEFAULT 0
);

CREATE TABLE treasure (
    id INTEGER PRIMARY KEY AUTOINCREMENT,    
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    description TEXT,
    userCreatorId INTEGER, 
    FOREIGN KEY(userCreatorId) REFERENCES user(id)
);

CREATE TABLE user_treasure (
    userId INTEGER NOT NULL,
    treasureId INTEGER NOT NULL,
    FOREIGN KEY(userId) REFERENCES user(id),
    FOREIGN KEY(treasureId) REFERENCES treasure(id)
);
