package fr.milekat.shops.storage.adapter.sql;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.Configs;
import fr.milekat.utils.storage.StorageConnection;
import fr.milekat.utils.storage.adapter.sql.connection.SQLConnection;
import fr.milekat.utils.storage.adapter.sql.connection.SQLDataBaseClient;
import fr.milekat.utils.storage.adapter.sql.utils.Schema;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.milekat.utils.storage.exceptions.StorageLoadException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class SQLStorage implements StorageImplementation {
    private final Configs config;
    private final String SCHEMA_FILE = "shop_schema.sql";
    private final String DatabaseName;
    private final String PREFIX = Main.getConfigs().getString("storage.sql.prefix");
    private final List<String> TABLES = List.of("TBD");
    private final StorageConnection connection;

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
    public SQLStorage(@NotNull Configs config) throws StorageLoadException {
        this.config = config;
        DatabaseName = config.getString("storage.sql.database");
        connection = new SQLConnection(config);
        ensureSchema();
    }

    private void ensureSchema() throws StorageLoadException {
        try (SQLDataBaseClient client = this.connection.getSQLClient()) {
            if (!checkStorages()) {
                try (InputStream schemaFile = this.getClass().getResourceAsStream(SCHEMA_FILE)) {
                    if (schemaFile == null) {
                        throw new StorageLoadException("Missing schema file");
                    } else {
                        new Schema(client, schemaFile, PREFIX);
                    }
                }
            }
        } catch (StorageExecuteException exception) {
            throw new StorageLoadException("Unsupported database type");
        } catch (IOException exception) {
            throw new StorageLoadException("Error while loading schema file");
        }
    }

    /**
     * Format query by replacing {prefix} with {@link SQLStorage#PREFIX}
     */
    @Contract(pure = true)
    private @NotNull String formatQuery(@NotNull String query) {
        return query.replaceAll("\\{prefix}", PREFIX);
    }

    /**
     * Disconnect from HikariCP pool
     */
    @Override
    public void disconnect() {
        connection.close();
    }

    /**
     * Check if all tables are created
     *
     * @return true if all tables are created
     */
    @Override
    public boolean checkStorages() throws StorageExecuteException {
        try (SQLDataBaseClient client = this.connection.getSQLClient();
             Connection connection = client.getConnection()) {
            for (String table : TABLES) {
                try (PreparedStatement q = connection.prepareStatement(formatQuery(CHECK_TABLE))) {
                    q.setString(1, DatabaseName);
                    q.setString(2, table);
                    q.execute();
                    if (!q.getResultSet().next()) {
                        if (Main.DEBUG) {
                            Main.getMileLogger().warning("Table: " + table + " not found in " + DatabaseName);
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
    public @NotNull List<Shop> getAllShops() {
        return null;
    }

    @Override
    public void asyncDeleteShop(@NotNull Shop shop, CommandSender sender) {

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
    public int getTradeUses(@NotNull Map<String, Object> tags, @NotNull Trade trade) {
        return 0;
    }

    @Override
    public void logTrade(@Nullable Map<String, Object> tags, @NotNull Trade trade) {

    }

    /*
        Class shortcuts
     */

}