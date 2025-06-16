package dev.balafini.factions.database;

public record MongoConfig(
        String host,
        int port,
        String database,
        String username,
        String password,
        boolean useAuthentication
) {
    public String connectionString() {
//        if (useAuthentication && !username.isBlank() && !password.isBlank()) {
//            return String.format("mongodb://%s:%s@%s:%d/%s", username, password, host, port, database);
//        }

        return String.format("mongodb+srv://%s:%s@%s/%s?retryWrites=true&w=majority",
                username, password, host, database);
    }
}


