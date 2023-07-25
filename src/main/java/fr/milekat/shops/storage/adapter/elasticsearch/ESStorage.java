package fr.milekat.shops.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.CacheManager;
import fr.milekat.shops.storage.Storage;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.storage.exceptions.StorageExecuteException;
import fr.milekat.shops.storage.exceptions.StorageLoaderException;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.workers.utils.NPCUtils;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.Configs;
import fr.milekat.utils.DateMileKat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class ESStorage implements StorageImplementation {
    private final String PREFIX;
    private final ESConnector DB;
    private final List<BulkOperation> logToProcess = new ArrayList<>();

    /*
        Main DB
    */
    public ESStorage(@NotNull Configs config) throws StorageLoaderException {
        this.PREFIX = config.getString("storage.elasticsearch.prefix");
        DB = new ESConnector(config);
        try (ESConnection connection = DB.getConnection()) {
            Main.debug(connection.getClient().cluster().health().toString());
            logPool();
        } catch (IOException exception) {
            throw new StorageLoaderException("Error while trying to load ElasticSearch cluster");
        }
    }

    @Override
    public boolean checkStorages() {
        List<String> indices = new ArrayList<>();
        indices.add("trades");
        indices.add("shops");
        indices.add("history");
        indices.add("users-mode");
        try (ESConnection connection = DB.getConnection()) {
            for (String index : indices) {
                //  Check if index exist, otherwise create it
                if (!connection.getClient()
                        .indices()
                        .exists(ExistsRequest.of(builder -> builder.index(PREFIX + index)))
                        .value()) {
                    connection.getClient().indices().create(c -> c.index(PREFIX + index));
                } else {
                    Main.debug("Index '" + PREFIX + index +"' found !");
                }
            }
            Main.debug("Indices loaded");
            return true;
        } catch (ElasticsearchException | IOException exception) {
            Main.warning("Error while trying to load ElasticSearch indices.");
            Main.stack(exception.getStackTrace());
        }
        return false;
    }

    @Override
    public String getImplementationName() {
        return null;
    }

    @Override
    public void disconnect() {
        Main.debug("ElasticSearch automatically close connections after execution, using try-with-resources.");
    }

    /*
        ES Queries execution
     */
    @Override
    public void asyncSaveShop(@NotNull Shop shop, CommandSender sender, boolean createIfNotExist) {
        //  Open Bukkit async task
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), ()-> {
            //  Open Elasticsearch connection
            try (ESConnection connection = DB.getConnection()) {
                Main.debug("[ES-aSync] prepareShopAsync - search shop with field 'name' and " +
                        "value '" + shop.getName() + "'.");
                try {
                    SearchResponse<Shop> searchResponse = connection.getClient().search(
                            new SearchRequest.Builder()
                                    .index(PREFIX + "shops")
                                    .query(q -> q.match(m -> m.field("name").query(shop.getName())))
                                    .size(1)
                                    .build(),
                            Shop.class
                    );
                    // Check if shop exist
                    if (searchResponse.hits().total() != null && searchResponse.hits().total().value() > 0) {
                        //  Update existing shop
                        Main.debug("[ES-aSync] asyncSaveShop - update shop " + shop.getUuid() + ".");
                        try {
                            connection.getClient().update(u -> u
                                            .index(PREFIX + "shops")
                                            .id(searchResponse.hits().hits().get(0).id())
                                            .doc(shop)
                                            .docAsUpsert(true),
                                    Shop.class);
                            Main.info("Shop '" + shop.getName() + "' has been updated");
                            Main.message(sender, "&2Shop updated !");
                        } catch (ElasticsearchException | IOException exception) {
                            Main.message(sender, "&cError while trying to update shop " + shop.getName());
                            Main.warning("Error while trying to update shop with uuid " + shop.getUuid());
                            Main.stack(exception.getStackTrace());
                            NPCUtils.syncDestroy(shop.getNpc());
                        }
                    } else {
                        if (!createIfNotExist) {
                            Main.message(sender, "&cShop with name '" + shop.getName() + "' do not exist.");
                            NPCUtils.syncDestroy(shop.getNpc());
                            return;
                        }
                        //  Save new shop
                        Main.debug("[ES-aSync] asyncSaveShop - index new shop " + shop.getUuid() + ".");
                        try {
                            connection.getClient().index(i -> i
                                    .index(PREFIX + "shops")
                                    .document(shop));
                            Main.info("New shop created with name " + shop.getName());
                            Main.message(sender, "&2Shop created !");
                        } catch (ElasticsearchException | IOException exception) {
                            Main.message(sender, "&cError while trying to create shop " + shop.getName());
                            Main.warning("Error while trying to index shop with uuid " + shop.getUuid());
                            Main.stack(exception.getStackTrace());
                            NPCUtils.syncDestroy(shop.getNpc());
                        }
                    }

                } catch (ElasticsearchException | IOException exception) {
                    Main.message(sender, "&cError while trying to save shop " + shop.getName());
                    Main.warning("Error while trying to fetch shop with uuid " + shop.getName());
                    exception.printStackTrace();
                    NPCUtils.syncDestroy(shop.getNpc());
                }

            } catch (IOException exception) {
                Main.warning("ElasticSearch client error.");
                Main.stack(exception.getStackTrace());
            }
        });
    }

    private @Nullable Shop getShop(@NotNull String field, @NotNull String value) throws IOException {
        try (ESConnection connection = DB.getConnection()) {
            Main.debug("[ES-Sync] getShop - search shop with field '" + field + "' and value '" + value + "'.");
            SearchResponse<Shop> response = connection.getClient().search(
                    new SearchRequest.Builder()
                            .index(PREFIX + "shops")
                            .query(q -> q.match(m -> m.field(field).query(value)))
                            .size(1)
                            .build(),
                    Shop.class
            );
            Optional<Hit<Shop>> shop = response.hits().hits().stream().findFirst();
            if (shop.isPresent() && shop.get().source() != null) {
                CacheManager.addCache(Storage.SHOP_CACHE, shop.get().source());
                return shop.get().source();
            }
            return null;
        }
    }

    @Override
    public Shop getShop(@NotNull UUID uuid) throws StorageExecuteException {
        try {
            return getShop("uuid", uuid.toString());
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find shop with playerUuid " + uuid);
        }
    }

    @Override
    public Shop getShop(@NotNull String name) throws StorageExecuteException {
        try {
            return getShop("name", name);
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find shop with name " + name);
        }
    }

    @Override
    public Shop getShopNpc(@NotNull UUID npcUuid) throws StorageExecuteException {
        try {
            return getShop("npc.uuid", npcUuid.toString());
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find shop with npc.uuid " + npcUuid);
        }
    }

    @Override
    public List<Shop> getAllShops() throws StorageExecuteException {
        try (ESConnection connection = DB.getConnection()) {
            try {
                Main.debug("[ES-Sync] getAllShops - Fetch all shops.");
                SearchResponse<Shop> response = connection.getClient().search(
                        new SearchRequest.Builder()
                                .index(PREFIX + "shops")
                                .size(1024)
                                .build(),
                        Shop.class);
                List<Shop> shops = new ArrayList<>();
                response.hits().hits().forEach(hit -> shops.add(hit.source()));
                Storage.SHOP_CACHE = shops.stream().collect(HashMap::new,
                        ((map, shop) -> map.put(shop, new Date())), Map::putAll);
                return shops;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception, "Error while trying to fetch all shops.");
            }
        } catch (IOException exception) {
            throw new StorageExecuteException(exception, "ElasticSearch client error.");
        }
    }

    @Override
    public void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), ()-> {
            try (ESConnection connection = DB.getConnection()) {
                List<BulkOperation> bulkDocs = new ArrayList<>();
                trades.forEach(trade -> bulkDocs
                        .add(new BulkOperation.Builder().create(c -> c.document(trade)).build()));
                Main.debug("[ES-aSync] asyncSaveShopTrades - delete all trades for shop '" + shop.getUuid() + "'.");
                try {
                    connection.getClient().deleteByQuery(new DeleteByQueryRequest.Builder()
                            .index(PREFIX + "trades")
                            .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shop.getUuid()))))
                            .build());
                } catch (ElasticsearchException | IOException exception) {
                    Main.message(sender, "&cError while trying to update trades");
                    Main.warning("Error while trying to remove trades from shop with uuid " + shop.getUuid());
                    Main.stack(exception.getStackTrace());
                    return;
                }
                Main.debug("[ES-aSync] asyncSaveShopTrades - index all trades for shop '" + shop.getUuid() + "'.");
                try {
                    connection.getClient().bulk(new BulkRequest.Builder()
                            .index(PREFIX + "trades")
                            .operations(bulkDocs)
                            .build());
                } catch (ElasticsearchException | IOException exception) {
                    Main.message(sender, "&cError while trying to update trades");
                    Main.warning("Error while trying to save trades from shop with uuid " + shop.getUuid());
                    Main.stack(exception.getStackTrace());
                    return;
                }
                Main.message(sender, "&2Trades saved, &6Updating trades..");
                CacheManager.addCache(Storage.TRADE_CACHE, new ShopTrades(shop.getUuid(), trades));
                Main.message(sender, "&2Trades updated !");
                Main.info("Trades from shop " + shop.getName() + " updated");
            } catch (IOException exception) {
                Main.warning("ElasticSearch client error.");
                Main.stack(exception.getStackTrace());
            }
        });
    }

    @Override
    public List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        try (ESConnection connection = DB.getConnection()) {
            try {
                Main.debug("[ES-Sync] getTrades - Get all trades of shop '" + shopUuid + "'.");
                SearchResponse<Trade> response = connection.getClient().search(
                        new SearchRequest.Builder()
                                .index(PREFIX + "trades")
                                .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shopUuid))))
                                .size(64)
                                .build(),
                        Trade.class);
                List<Trade> trades = new ArrayList<>();
                response.hits().hits().forEach(hit -> trades.add(hit.source()));
                CacheManager.addCache(Storage.TRADE_CACHE, new ShopTrades(shopUuid, trades));
                Main.debug("Found '" + trades.size() + "' trades for shop '" + shopUuid + "'.");
                return trades;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception,
                        "Error while trying to find trades for shop uuid " + shopUuid);
            }
        } catch (IOException exception) {
            throw new StorageExecuteException(exception, "ElasticSearch client error.");
        }
    }

    @Override
    public List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = getShop(shopName).getUuid();
        return getTrades(shopUuid);
    }

    @Override
    public void asyncSaveTradeMode(@NotNull UUID playerUuid, @NotNull TradeMode mode) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), ()-> {
            try (ESConnection connection = DB.getConnection()) {
                PlayerTradeMode playerMode = new PlayerTradeMode(playerUuid, mode);
                Main.debug("[ES-aSync] asyncSaveTradeMode - search users-mode");
                try {
                    SearchResponse<PlayerTradeMode> searchResponse = connection.getClient().search(
                            new SearchRequest.Builder()
                                    .index(PREFIX + "users-mode")
                                    .query(q -> q.match(m -> m.field("playerUuid").query(playerUuid.toString())))
                                    .size(1)
                                    .build(),
                            PlayerTradeMode.class
                    );
                    int results = searchResponse.hits().hits().size();
                    try {
                        if (results == 0) {
                            Main.debug("[ES-aSync] asyncSaveTradeMode - index users-mode");

                            connection.getClient().index(c -> c
                                    .index(PREFIX + "users-mode")
                                    .document(playerMode)
                            );
                        } else {
                            Main.debug("[ES-aSync] asyncSaveTradeMode - update users-mode");
                            connection.getClient().update(u -> u
                                            .index(PREFIX + "users-mode")
                                            .id(searchResponse.hits().hits().get(0).id())
                                            .doc(playerMode)
                                            .docAsUpsert(true),
                                    PlayerTradeMode.class
                            );
                        }
                        CacheManager.addCache(Storage.TRADE_MODE_CACHE, playerMode);
                    } catch (ElasticsearchException | IOException exception) {
                        Main.warning("Error while saving TradeMode for " + playerUuid);
                        Main.stack(exception.getStackTrace());
                    }
                } catch (ElasticsearchException | IOException exception) {
                    Main.warning("Error while searching TradeMode for " + playerUuid);
                    Main.stack(exception.getStackTrace());
                }
            } catch (IOException exception) {
                Main.warning("ElasticSearch client error.");
                Main.stack(exception.getStackTrace());
            }
        });
    }

    @Override
    public TradeMode getTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        try (ESConnection connection = DB.getConnection()) {
            Main.debug("[ES-Sync] getTradeMode - search users-mode of '" + playerUuid + "'.");
            try {
                SearchResponse<PlayerTradeMode> searchResponse = connection.getClient().search(
                        new SearchRequest.Builder()
                                .index(PREFIX + "users-mode")
                                .query(q -> q.match(m -> m.field("playerUuid").query(playerUuid.toString())))
                                .size(1)
                                .build(),
                        PlayerTradeMode.class);
                TradeMode tradeMode = TradeMode.INVENTORY;
                if (searchResponse.hits().hits().size() > 0 && searchResponse.hits().hits().get(0).source() != null) {
                    PlayerTradeMode playerTradeMode = searchResponse.hits().hits().get(0).source();
                    if (playerTradeMode != null) {
                        tradeMode = playerTradeMode.tradeMode();
                    }
                }
                if (tradeMode == null) {
                    tradeMode = TradeMode.INVENTORY;
                }
                CacheManager.addCache(Storage.TRADE_MODE_CACHE, new PlayerTradeMode(playerUuid, tradeMode));
                return tradeMode;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception,
                        "Error while trying to fetch trade mode of player " + playerUuid);
            }
        } catch (IOException exception) {
            throw new StorageExecuteException(exception, "ElasticSearch client error.");
        }
    }
    @Override
    public int getTradeUses(@NotNull UUID player, @NotNull UUID tradeUuid) {
        return 0;
    }

    @Override
    public void logTrade(@NotNull UUID player, @Nullable Map<String, Object> tags, @NotNull Trade trade) {
        Map<String, Object> log = new HashMap<>();
        log.put("uuid", player);
        log.put("tags", tags);
        log.put("trade", trade);
        log.put("@timestamp", DateMileKat.getDateEs());
        logToProcess.add(
                new BulkOperation.Builder().create(
                        new CreateOperation.Builder<>()
                                .index(PREFIX + "history")
                                .document(log)
                                .build()
                ).build()
        );
    }

    private void logPool() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), ()-> {
            List<BulkOperation> processing = new ArrayList<>(logToProcess);
            logToProcess.clear();
            if (processing.size() > 0) {
                try (ESConnection connection = DB.getConnection()) {
                    connection.getClient().bulk(new BulkRequest.Builder().operations(processing).build());
                    Main.debug("'" + processing.size() + "' log trades saved.");
                } catch (ElasticsearchException | IOException exception) {
                    logToProcess.addAll(processing);
                    Main.warning("Error while trying to save Trade Logs.");
                    Main.stack(exception.getStackTrace());
                }
            }
        }, 50, 100);
    }
}
