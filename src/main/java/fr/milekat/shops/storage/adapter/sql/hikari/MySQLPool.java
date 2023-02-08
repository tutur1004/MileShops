package fr.milekat.shops.storage.adapter.sql.hikari;

import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;

public class MySQLPool extends HikariPool {
    @Override
    public String getImplementationName() {
        return "MySQL";
    }

    @Override
    protected void configureDatabase(@NotNull HikariConfig config, String address, String port,
                                     String databaseName, String username, String password) {
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + address + ":" + port + "/" + databaseName);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }
}