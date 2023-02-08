package fr.milekat.shops.storage.adapter.sql;

import fr.milekat.shops.storage.adapter.sql.hikari.HikariPool;
import fr.milekat.shops.storage.adapter.sql.hikari.MariaDBPool;
import fr.milekat.shops.storage.adapter.sql.hikari.MySQLPool;
import fr.milekat.shops.storage.adapter.sql.hikari.PostgresPool;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

// TODO: Implement SQL methods !
public class SQLConnection {
    private final SQLDataBaseConnection sqlDataBaseConnection;

    public SQLConnection(@NotNull FileConfiguration config) throws StorageLoaderException {
        HikariPool hikariPool;
        switch (config.getString("storage.type").toLowerCase()) {
            case "mysql": {
                hikariPool = new MySQLPool();
                break;
            }
            case "mariadb": {
                hikariPool = new MariaDBPool();
                break;
            }
            case "postgres": {
                hikariPool = new PostgresPool();
                break;
            }
            default: {
                throw new StorageLoaderException("Unknown SQL type");
            }
        }
        hikariPool.init(config);
        sqlDataBaseConnection = hikariPool;
    }

    public SQLDataBaseConnection getSqlDataBaseConnection() {
        return sqlDataBaseConnection;
    }
}
