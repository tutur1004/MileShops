package fr.milekat.shops.storage.adapter.sql;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.Configs;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class SQLStorage implements StorageImplementation {
    private final String SCHEMA_FILE = "shop_schema.sql";
    private final SQLDataBaseConnection DB;
    private final String DatabaseName;
    private final String PREFIX = Main.getConfigs().getString("storage.sql.prefix");
    private final List<String> TABLES = List.of("TBD");

    /*
        SQL Queries definition
     */
    private final String CHECK_TABLE = "SELECT TABLE_NAME " +
            "FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA=? AND TABLE_NAME = ?;";

    //  Tables

    //  Queries

    /*
        Main DB
     */
    public SQLStorage(@NotNull Configs config) throws StorageLoaderException {
        DatabaseName = config.getString("storage.sql.database");
        DB = new SQLConnection(config).getSqlDataBaseConnection();
        try {
            if (!checkStorages()) {
                applySchema();
            }
        } catch (StorageExecuteException | IOException exception) {
            throw new StorageLoaderException("Unsupported database type");
        }
    }

    /**
     * Format query by replacing {prefix} with {@link SQLStorage#PREFIX}
     */
    @Contract(pure = true)
    private @NotNull String formatQuery(@NotNull String query) {
        return query.replaceAll("\\{prefix}", PREFIX);
    }

    @Override
    public String getImplementationName() {
        return DB.getImplementationName();
    }

    /**
     * Disconnect from HikariCP pool
     */
    @Override
    public void disconnect() {
        DB.close();
    }

    /**
     * Apply SQL Default schema with shop_schema.sql dump
     */
    private void applySchema() throws IOException, StorageLoaderException {
        List<String> statements;
        //  Read schema file
        try (InputStream schemaFileIS = this.getClass().getResourceAsStream(SCHEMA_FILE)) {
            if (schemaFileIS == null) {
                throw new StorageLoaderException("Missing schema file");
            }
            statements = SQLUtils.getQueries(schemaFileIS).stream()
                    .map(this::formatQuery)
                    .collect(Collectors.toList());
        }
        //  Apply Schema
        try (Connection connection = DB.getConnection();
             Statement s = connection.createStatement()) {
            connection.setAutoCommit(false);
            for (String query : statements) {
                s.addBatch(query);
            }
            s.executeBatch();
        } catch (Exception exception) {
            if (!exception.getMessage().contains("already exists") && Main.DEBUG) {
                exception.printStackTrace();
            }
        }
    }

    /**
     * Check if all tables are created
     * @return true if all tables are created
     */
    @Override
    public boolean checkStorages() throws StorageExecuteException {
        try (Connection connection = DB.getConnection()) {
            for (String table : TABLES) {
                try (PreparedStatement q = connection.prepareStatement(formatQuery(CHECK_TABLE))) {
                    q.setString(1, DatabaseName);
                    q.setString(2, table);
                    q.execute();
                    if (!q.getResultSet().next()) {
                        if (Main.DEBUG) {
                            Main.warning("Table: " + table + " not found in " + DatabaseName);
                        }
                        return false;
                    }
                }
            }
            return true;
        } catch (SQLException exception) {
            throw new StorageExecuteException(exception, "Missing schema file");
        }
    }

    /*
        SQL Queries execution
     */
    @Override
    public void asyncSaveShop(@NotNull Shop shop, CommandSender sender, boolean createIfNotExist) {

    }

    @Override
    public Shop getShop(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public Shop getShop(@NotNull String name) {
        return null;
    }

    @Override
    public Shop getShopNpc(@NotNull UUID shopNpcUuid) throws StorageExecuteException {
        return null;
    }

    @Override
    public List<Shop> getAllShops() {
        return null;
    }

    @Override
    public void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender) {

    }

    @Override
    public List<Trade> getTrades(@NotNull UUID shopUuid) {
        return null;
    }

    @Override
    public List<Trade> getTrades(@NotNull String shopName) {
        return null;
    }

    @Override
    public void asyncSaveTradeMode(@NotNull UUID playerUuid, @NotNull TradeMode mode) {

    }

    @Override
    public TradeMode getTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        return null;
    }

    @Override
    public int getTradeUses(@NotNull UUID player, @NotNull UUID tradeUuid) {
        return 0;
    }

    @Override
    public void logTrade(@NotNull UUID player, @Nullable List<String> playerTags, @NotNull Trade trade) {

    }

    /*
        Class shortcuts
     */

}