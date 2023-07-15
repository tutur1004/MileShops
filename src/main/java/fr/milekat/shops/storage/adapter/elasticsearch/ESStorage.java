package fr.milekat.shops.storage.adapter.elasticsearch;

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
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.Configs;
import fr.milekat.utils.DateMileKat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ESStorage implements StorageImplementation {
    private final String PREFIX;
    private final ESConnection DB;
    private final List<BulkOperation> logToProcess = new ArrayList<>();

    /*
        Main DB
    */
    public ESStorage(@NotNull Configs config) throws StorageLoaderException {
        this.PREFIX = config.getString("storage.elasticsearch.prefix");
        try {
            DB = new ESConnection(config);
            Main.debug(DB.getClient().cluster().health().toString());
            logPool();
        } catch (IOException e) {
            throw new StorageLoaderException("Error while trying to load ElasticSearch cluster");
        }
    }

    @Override
    public boolean checkStorages() throws StorageExecuteException {
        List<String> indices = new ArrayList<>();
        indices.add("trades");
        indices.add("shops");
        indices.add("history");
        indices.add("users-mode");
        try {
            for (String index : indices) {
                //  Check if index exist, otherwise create it
                if (!DB.getClient()
                        .indices()
                        .exists(ExistsRequest.of(builder -> builder.index(PREFIX + index)))
                        .value()) {
                    DB.getClient().indices().create(c -> c.index(PREFIX + index));
                }

            }
            Main.debug("Indices loaded");
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to load ElasticSearch indices");
        }
        return true;
    }

    @Override
    public String getImplementationName() {
        return null;
    }

    @Override
    public void disconnect() {
        try {
            DB.close();
            Main.debug("ElasticSearch connection closed.");
        } catch (IOException e) {
            Main.warning("Error while trying to close RestClient for Elasticsearch connection.");
        }
    }

    /*
        ES Queries execution
     */
    @Override
    public void asyncSaveShop(@NotNull Shop shop, CommandSender sender, boolean createIfNotExist) {
        prepareShopAsync("name", shop.getName()).whenComplete((response, exception) -> {
            if (exception!=null) {
                Main.message(sender, "&cError while trying to save shop " + shop.getName());
                Main.warning("Error while trying to fetch shop with uuid " + shop.getName());
                exception.printStackTrace();
                Main.bukkitSync(shop.getNpc()::destroy);
            }
            if (response.hits().total() != null && response.hits().total().value() > 0) {
                if (!createIfNotExist) {
                    Main.message(sender, "&cShop with name '" + shop.getName() + "' already exist.");
                    Main.bukkitSync(shop.getNpc()::destroy);
                    return;
                }
                //  Update existing shop
                Main.debug("[ES-aSync] asyncSaveShop - update shop " + shop.getUuid() + ".");
                DB.getAsyncClient().update(u -> u
                                .index(PREFIX + "shops")
                                .id(response.hits().hits().get(0).id())
                                .doc(shop)
                                .docAsUpsert(true),
                        Shop.class
                ).whenComplete((updateResponse, updateException) -> {
                    if (updateException != null) {
                        Main.message(sender, "&cError while trying to update shop " + shop.getName());
                        Main.warning("Error while trying to update shop with uuid " + shop.getUuid());
                        Main.stack(updateException.getStackTrace());
                        Main.bukkitSync(shop.getNpc()::destroy);
                    } else {
                        Main.info("Shop '" + shop.getName() + "' has been updated");
                        Main.message(sender, "&2Shop updated !");
                    }
                });
            } else {
                //  Save new shop
                Main.debug("[ES-aSync] asyncSaveShop - index new shop " + shop.getUuid() + ".");
                DB.getAsyncClient().index(i -> i
                        .index(PREFIX + "shops")
                        .document(shop)
                ).whenComplete((indexResponse, throwable) -> {
                    if (throwable!=null) {
                        Main.message(sender, "&cError while trying to create shop " + shop.getName());
                        Main.warning("Error while trying to index shop with uuid " + shop.getUuid());
                        Main.stack(throwable.getStackTrace());
                        Main.bukkitSync(shop.getNpc()::destroy);
                    } else {
                        Main.info("New shop created with name " + shop.getName());
                        Main.message(sender, "&2Shop created !");
                    }
                });
            }
        });
    }

    @SuppressWarnings("SameParameterValue")
    private CompletableFuture<SearchResponse<Shop>> prepareShopAsync(@NotNull String field, @NotNull String value) {
        Main.debug("[ES-aSync] prepareShopAsync - search shop with field '" + field + "' and value '" + value + "'.");
        return DB.getAsyncClient().search(
               new SearchRequest.Builder()
                       .index(PREFIX + "shops")
                       .query(q -> q.match(m -> m.field(field).query(value)))
                       .size(1)
                       .build(),
               Shop.class
        );
    }

    private @Nullable Shop getShop(@NotNull String field, @NotNull String value) throws IOException {
        Main.debug("[ES-Sync] getShop - search shop with field '" + field + "' and value '" + value + "'.");
        SearchResponse<Shop> response = DB.getClient().search(
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
        try {
            Main.debug("[ES-Sync] getAllShops - Fetch all shops.");
            SearchResponse<Shop> response = DB.getClient().search(
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
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to fetch all shops");
        }
    }

    @Override
    public void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender) {
        List<BulkOperation> bulkDocs = new ArrayList<>();
        trades.forEach(trade -> bulkDocs.add(new BulkOperation.Builder().create(c -> c.document(trade)).build()));
        Main.debug("[ES-aSync] asyncSaveShopTrades - delete all trades for shop '" + shop.getUuid() + "'.");
        DB.getAsyncClient().deleteByQuery(new DeleteByQueryRequest.Builder()
                .index(PREFIX + "trades")
                .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shop.getUuid()))))
                .build()).whenComplete((deleteResponse, deleteException) -> {
                    if (deleteException!=null) {
                        Main.warning("Error while trying to remove trades from shop with uuid " + shop.getUuid());
                        Main.message(sender, "&cError while trying to update trades");
                        return;
                    }
                    Main.debug("[ES-aSync] asyncSaveShopTrades - index all trades for shop '" + shop.getUuid() + "'.");
                    DB.getAsyncClient().bulk(new BulkRequest.Builder()
                            .index(PREFIX + "trades")
                            .operations(bulkDocs)
                            .build()).whenComplete((bulkResponse, bulkException) -> {
                                if (bulkException!=null) {
                                    Main.warning("Error while trying to save trades for shop with uuid " + shop.getUuid());
                                    Main.message(sender, "&cError while trying to update trades");
                                    return;
                                }
                                Main.message(sender, "&2Trades saved, &6Updating trades..");
                                CacheManager.addCache(Storage.TRADE_CACHE, new ShopTrades(shop.getUuid(), trades));
                                Main.message(sender, "&2Trades updated !");
                                Main.info("Trades from shop " + shop.getName() + " updated");
                            });
                });
    }

    @Override
    public List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        try {
            Main.debug("[ES-Sync] getTrades - Get all trades of shop '" + shopUuid + "'.");
            SearchResponse<Trade> response =  DB.getClient().search(
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
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find trades for shop uuid " + shopUuid);
        }
    }

    @Override
    public List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = getShop(shopName).getUuid();
        return getTrades(shopUuid);
    }

    @Override
    public void asyncSaveTradeMode(@NotNull UUID playerUuid, @NotNull TradeMode mode) {
        // TODO: 22/06/2023 Duplicated values here ?
        PlayerTradeMode playerMode = new PlayerTradeMode(playerUuid, mode);
        Main.debug("[ES-aSync] asyncSaveTradeMode - search users-mode");
        DB.getAsyncClient().search(new SearchRequest.Builder()
                        .index(PREFIX + "users-mode")
                        .query(q -> q.match(m -> m.field("playerUuid").query(playerUuid.toString())))
                        .size(1)
                        .build(),
                PlayerTradeMode.class
        ).whenComplete(((searchResponse, searchException) -> {
            if (searchException!=null) {
                Main.warning("Error while searching TradeMode for " + playerUuid);
                Main.stack(searchException.getStackTrace());
            }
            int results = searchResponse.hits().hits().size();
            if (results==0) {
                Main.debug("[ES-aSync] asyncSaveTradeMode - index users-mode");
                DB.getAsyncClient().index(c -> c
                        .index(PREFIX + "users-mode")
                        .document(playerMode)
                ).whenComplete((createResponse, createException) ->
                        CacheManager.addCache(Storage.TRADE_MODE_CACHE, playerMode));
            } else {
                Main.debug("[ES-aSync] asyncSaveTradeMode - update users-mode");
                DB.getAsyncClient().update(u -> u
                                .index(PREFIX + "users-mode")
                                .id(searchResponse.hits().hits().get(0).id())
                                .doc(playerMode)
                                .docAsUpsert(true),
                        PlayerTradeMode.class
                ).whenComplete((updateResponse, updateException) ->
                        CacheManager.addCache(Storage.TRADE_MODE_CACHE, playerMode));
            }
        }));
    }

    @Override
    public TradeMode getTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        try {
            Main.debug("[ES-Sync] getTradeMode - search users-mode of '" + playerUuid + "'.");
            SearchResponse<PlayerTradeMode> response = DB.getClient().search(
                    new SearchRequest.Builder()
                            .index(PREFIX + "users-mode")
                            .query(q -> q.match(m -> m.field("playerUuid").query(playerUuid.toString())))
                            .size(1)
                            .build(),
                    PlayerTradeMode.class);
            TradeMode tradeMode = TradeMode.INVENTORY;
            if (response.hits().hits().size() > 0 && response.hits().hits().get(0).source()!=null) {
                PlayerTradeMode playerTradeMode = response.hits().hits().get(0).source();
                if (playerTradeMode!=null) {
                    tradeMode = playerTradeMode.tradeMode();
                }
            }
            if (tradeMode == null) {
                tradeMode = TradeMode.INVENTORY;
            }
            CacheManager.addCache(Storage.TRADE_MODE_CACHE, new PlayerTradeMode(playerUuid, tradeMode));
            return tradeMode;
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to fetch trade mode of player " + playerUuid);
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
                )
                .build()
        );
    }

    private void logPool() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), ()-> {
            List<BulkOperation> processing = new ArrayList<>(logToProcess);
            logToProcess.clear();
            if (processing.size() > 0) {
                DB.getAsyncClient().bulk(
                        new BulkRequest.Builder()
                                .operations(processing)
                                .build()
                ).whenComplete((bulkResponse, bulkException) -> {
                    if (bulkException!=null) {
                        logToProcess.addAll(processing);
                        Main.warning("Error while trying to save Trade Logs.");
                        Main.stack(bulkException.getStackTrace());
                    } else {
                        Main.debug("'" + processing.size() + "' log trades saved.");
                    }
                });
            }
        }, 50, 100);
    }
}
