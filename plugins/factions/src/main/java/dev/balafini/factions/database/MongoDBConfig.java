package dev.balafini.factions.database;

public class MongoDBConfig {
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean authEnabled;
    private final boolean isAtlas;

    public MongoDBConfig(String host, int port, String database, String username, String password, boolean authEnabled,
                         boolean isAtlas) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.authEnabled = authEnabled;
        this.isAtlas = isAtlas;
    }

    public String getConnectionString() {
        if (isAtlas) {
            return String.format("mongodb+srv://%s:%s@%s/%s?retryWrites=true&w=majority",
                    username, password, host, database);
        } else if (authEnabled) {
            return String.format("mongodb://%s:%s@%s:%d/%s", username, password, host, port, database);
        } else {
            return String.format("mongodb://%s:%d/%s", host, port, database);
        }
    }

    public String getDatabase() {
        return database;
    }
}
