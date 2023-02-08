package fr.milekat.shops.storage.adapter.sql;


import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLDataBaseConnection {
    String getImplementationName();

    void init(@NotNull FileConfiguration configs);

    void close();

    Connection getConnection() throws SQLException;
}
