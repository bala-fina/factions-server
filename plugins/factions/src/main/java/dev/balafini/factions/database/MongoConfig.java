package dev.balafini.factions.database;

import org.bukkit.configuration.file.FileConfiguration;

public record MongoConfig(
        String connectionString,
        String databaseName
) {

    public static MongoConfig load(FileConfiguration config) {
        return new MongoConfig(
                config.getString("database.connection-string", "mongodb://localhost:27017" ),
                config.getString("database.database-name", "factionsdev" )
        );
    }

}


