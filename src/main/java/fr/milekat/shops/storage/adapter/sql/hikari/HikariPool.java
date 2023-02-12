package fr.milekat.shops.storage.adapter.sql.hikari;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.milekat.shops.storage.adapter.sql.SQLDataBaseConnection;
import fr.milekat.utils.Configs;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract {@link SQLDataBaseConnection} using a {@link HikariDataSource}.
 */
public abstract class HikariPool implements SQLDataBaseConnection {
    private HikariDataSource hikari;

    @Override
    public void init(@NotNull Configs config) {
        HikariConfig hikariConfig = new HikariConfig();

        // set pool name so the logging output can be linked back to us
        hikariConfig.setPoolName("infra-hikari");

        // allow the implementation to configure the HikariConfig appropriately with these values
        configureDatabase(hikariConfig,
                config.getString("storage.sql.hostname"),
                config.getString("storage.sql.port"),
                config.getString("storage.sql.database"),
                config.getString("storage.sql.username"),
                config.getString("storage.sql.password"));

        // configure the connection pool
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(10);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setKeepaliveTime(0);
        hikariConfig.setConnectionTimeout(5000);

        hikariConfig.setConnectionTestQuery("SELECT 1");

        this.hikari = new HikariDataSource(hikariConfig);
    }

    @Override
    public void close() {
        if (this.hikari != null) {
            this.hikari.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (this.hikari == null) {
            throw new SQLException("Unable to get a connection from the pool. (hikari is null)");
        }

        Connection connection = this.hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Unable to get a connection from the pool. (getConnection returned null)");
        }

        return connection;
    }

    /**
     * Configures the {@link HikariConfig} with the relevant database properties.
     *
     * <p>Each driver does this slightly differently...</p>
     *
     * @param config the hikari config
     * @param hostname the database hostname
     * @param port the database port
     * @param databaseName the database name
     * @param username the database username
     * @param password the database password
     */
    protected abstract void configureDatabase(@NotNull HikariConfig config, String hostname, String port,
                                              String databaseName, String username, String password);
}
