package fr.milekat.shops.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.Storage;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import fr.milekat.shops.workers.utils.PlayerTradeMode;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.Configs;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ESStorage implements StorageImplementation {
    private final String PREFIX;
    private final ESConnection DB;

    /*
        Main DB
    */
    public ESStorage(@NotNull Configs config) throws StorageLoaderException {
        this.PREFIX = config.getString("storage.elasticsearch.prefix");
        try {
            DB = new ESConnection(config);
            Main.debug(DB.getClient().cluster().health().toString());
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
    public void asyncSaveShop(@NotNull Shop shop, CommandSender sender) {
        prepareShopAsync(shop.getUuid()).whenComplete(((response, exception) -> {
            if (exception!=null) {
                Main.message(sender, "&cError while trying to save shop " + shop.getName());
                Main.warning("Error while trying to fetch shop with uuid " + shop.getUuid());
                Main.stack(exception.getStackTrace());
            }
            Optional<Hit<Shop>> shopHit = response.hits().hits().stream().findFirst();
            //  Ensure shop name not exist
            // TODO : Test this check
            DB.getAsyncClient().count(new CountRequest.Builder()
                    .index(PREFIX + "shops")
                    .query(q -> q.bool(b -> b
                            .mustNot(mn -> mn.term(t -> t.field("uuid").value(shop.getUuid().toString())))
                            .must(m -> m.term(t -> t.field("name").value(shop.getName())))))
                    .build()
            ).whenComplete((countResponse, countException) -> {
                if (countException != null) {
                    Main.message(sender, "&cError while trying to save shop " + shop.getName());
                    Main.warning("Error while trying to check if shop name exist: " + shop.getName());
                    Main.stack(countException.getStackTrace());
                }
                if (countResponse.count() != 0) {
                    Main.message(sender, "&cShop name already exist " + shop.getName());
                    Main.warning("Can't save " + shop.getName());
                    return;
                }
                //  Name is not used
                if (shopHit.isEmpty()) {
                    DB.getAsyncClient().index(i -> i
                                    .index(PREFIX + "shops")
                                    .document(shop)
                    ).whenComplete(((indexResponse, throwable) -> {
                        if (throwable!=null) {
                            Main.message(sender, "&cError while trying to save shop " + shop.getName());
                            Main.warning("Error while trying to index shop with uuid " + shop.getUuid());
                            Main.stack(throwable.getStackTrace());
                        } else {
                            Main.info("New shop created with name " + shop.getName());
                            Main.message(sender, "&2Shop created !");
                        }
                    }));
                } else {
                        String id = shopHit.get().id();
                        DB.getAsyncClient().update(u -> u
                                        .index(PREFIX + "shops")
                                        .id(id)
                                        .doc(shop)
                                        .docAsUpsert(true),
                                Shop.class
                        ).whenComplete(((updateResponse, updateException) -> {
                            if (updateException != null) {
                                Main.message(sender, "&cError while trying to save shop" + shop.getName());
                                Main.warning("Error while trying to save shop with uuid " + shop.getUuid());
                                Main.stack(updateException.getStackTrace());
                            } else {
                                Main.message(sender, "&2Shop saved !");
                            }
                        }));
                    }
                });
        }));
    }

    private CompletableFuture<SearchResponse<Shop>> prepareShopAsync(@NotNull UUID uuid) {
        return DB.getAsyncClient().search(
               new SearchRequest.Builder()
                       .index(PREFIX + "shops")
                       .query(q -> q.match(m -> m.field("uuid").query(String.valueOf(uuid))))
                       .size(1)
                       .build(),
               Shop.class);
    }

    @Override
    public Shop getShop(@NotNull UUID uuid) throws StorageExecuteException {
        try {
            return getShop("uuid", uuid.toString());
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find shop with uuid " + uuid);
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

    private @Nullable Shop getShop(@NotNull String field, @NotNull String value) throws IOException {
        SearchResponse<Shop> response =  DB.getClient().search(
                new SearchRequest.Builder()
                        .index(PREFIX + "shops")
                        .query(q -> q.match(m -> m.field(field).query(value)))
                        .size(1)
                        .build(),
                Shop.class);
        Optional<Hit<Shop>> shop = response.hits().hits().stream().findFirst();
        if (shop.isPresent() && shop.get().source() != null) {
            Storage.addCache(Storage.SHOP_CACHE, shop.get().source());
            return shop.get().source();
        }
        return null;
    }

    @Override
    public Shop getCacheShop(@NotNull UUID shopUuid) throws StorageExecuteException {
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            return optionalShop.get().getKey();
        } else  {
            return getShop(shopUuid);
        }
    }

    @Override
    public Shop getCacheShop(@NotNull String shopName) throws StorageExecuteException {
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getName().equals(shopName))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            return optionalShop.get().getKey();
        } else  {
            return getShop(shopName);
        }
    }

    @Override
    public List<Shop> getAllShops() throws StorageExecuteException {
        try {
            SearchResponse<Shop> response =  DB.getClient().search(
                    new SearchRequest.Builder()
                            .index(PREFIX + "shops")
                            .size(1024)
                            .build(),
                    Shop.class);
            List<Shop> shops = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                shops.add(hit.source());
            });
            Storage.SHOP_CACHE = shops.stream().collect(HashMap::new,
                    ((map, shop) -> map.put(shop, new Date())), Map::putAll);
            return shops;
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to fetch all shops");
        }
    }

    @Override
    public List<Shop> getCacheAllShops() throws StorageExecuteException {
        if (Storage.SHOP_CACHE.size() == 0 || Storage.SHOP_CACHE.values().stream()
                .anyMatch(date -> date.getTime() + Storage.SHOP_DELAY < new Date().getTime())) {
            return getAllShops();
        } else {
            return new ArrayList<>(Storage.SHOP_CACHE.keySet());
        }
    }

    @Override
    public void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender) {
        List<BulkOperation> bulkDocs = new ArrayList<>();
        trades.forEach(trade -> bulkDocs.add(new BulkOperation.Builder().create(c -> c.document(trade)).build()));
        DB.getAsyncClient().deleteByQuery(new DeleteByQueryRequest.Builder()
                .index(PREFIX + "trades")
                .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shop.getUuid()))))
                .build()).whenComplete((deleteResponse, deleteException) -> {
                    if (deleteException!=null) {
                        Main.warning("Error while trying to remove trades from shop with uuid " + shop.getUuid());
                        Main.message(sender, "&cError while trying to update trades");
                        return;
                    }
                    DB.getAsyncClient().bulk(new BulkRequest.Builder()
                    .index(PREFIX + "trades")
                    .operations(bulkDocs)
                    .build()).whenComplete(((bulkResponse, bulkException) -> {
                        if (bulkException!=null) {
                            Main.warning("Error while trying to save trades for shop with uuid " + shop.getUuid());
                            Main.message(sender, "&cError while trying to update trades");
                            return;
                        }
                        Main.message(sender, "&2Trades saved, &6Updating trades..");
                        List<Trade> cacheTrade = Storage.TRADE_CACHE.keySet().stream().toList();
                        cacheTrade.stream()
                                .filter(trade -> trade.getShopUuid().equals(shop.getUuid()))
                                .forEach(trade -> Storage.TRADE_CACHE.remove(trade));
                        trades.forEach(trade -> Storage.TRADE_CACHE.put(trade, new Date()));
                        Main.message(sender, "&2Trades updated !");
                        Main.info("Trades from shop " + shop.getName() + " updated");
                    }));
        });
    }

    @Override
    public List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        try {
            SearchResponse<Trade> response =  DB.getClient().search(
                    new SearchRequest.Builder()
                            .index(PREFIX + "trades")
                            .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shopUuid))))
                            .size(64)
                            .build(),
                    Trade.class);
            List<Trade> trades = new ArrayList<>();
            response.hits().hits().forEach(hit -> trades.add(hit.source()));
            List<Trade> cacheTrade = Storage.TRADE_CACHE.keySet().stream().toList();
            cacheTrade.stream()
                    .filter(trade -> trade.getShopUuid().equals(shopUuid))
                    .forEach(trade -> Storage.TRADE_CACHE.remove(trade));
            trades.forEach(trade -> Storage.TRADE_CACHE.put(trade, new Date()));
            return trades;
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find trades for shop uuid " + shopUuid);
        }
    }

    @Override
    public List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = this.getShop(shopName).getUuid();
        return this.getTrades(shopUuid);
    }

    @Override
    public List<Trade> getCacheTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        if (Storage.TRADE_CACHE.size() == 0 ||
                Storage.TRADE_CACHE.entrySet().stream().filter(entry -> entry.getKey().getShopUuid().equals(shopUuid))
                        .anyMatch(entry -> entry.getValue().getTime() + Storage.TRADE_DELAY < new Date().getTime())) {
            return getTrades(shopUuid);
        } else {
            return Storage.TRADE_CACHE.keySet().stream()
                    .filter(trade -> trade.getShopUuid().equals(shopUuid))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Trade> getCacheTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = getCacheShop(shopName).getUuid();
        return this.getCacheTrades(shopUuid);
    }

    @Override
    public void asyncSaveTradeMode(@NotNull String uuid, @NotNull TradeMode mode) {
        DB.getAsyncClient().search(new SearchRequest.Builder()
                        .index(PREFIX + "users-mode")
                        .query(q -> q.match(m -> m.field("uuid").query(uuid)))
                        .size(1)
                        .build(),
                PlayerTradeMode.class
        ).whenComplete(((searchResponse, searchException) -> {
            if (searchException!=null) {
                Main.warning("Error while searching TradeMode for " + uuid);
                Main.stack(searchException.getStackTrace());
            }
            int results = searchResponse.hits().hits().size();
            if (results==0) {
                DB.getAsyncClient().index(c -> c
                        .index(PREFIX + "users-mode")
                        .document(new PlayerTradeMode(uuid, mode))
                ).whenComplete((createResponse, createException) ->
                        Storage.addCache(Storage.TRADE_MODE_CACHE, uuid, mode));
            } else {
                DB.getAsyncClient().update(u -> u
                                .index(PREFIX + "users-mode")
                                .id(searchResponse.hits().hits().get(0).id())
                                .doc(new PlayerTradeMode(uuid, mode))
                                .docAsUpsert(true),
                        PlayerTradeMode.class
                ).whenComplete((updateResponse, updateException) ->
                        Storage.addCache(Storage.TRADE_MODE_CACHE, uuid, mode));
            }
        }));
    }

    public TradeMode getTradeMode(@NotNull String playerUuid) throws StorageExecuteException {
        try {
            SearchResponse<PlayerTradeMode> response =  DB.getClient().search(
                    new SearchRequest.Builder()
                            .index(PREFIX + "users-mode")
                            .query(q -> q.match(m -> m.field("uuid").query(playerUuid)))
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
            Storage.addCache(Storage.TRADE_MODE_CACHE, playerUuid, tradeMode);
            return tradeMode;
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to fetch trade mode of player " + playerUuid);
        }
    }

    @Override
    public TradeMode getCacheTradeMode(@NotNull String playerUuid) throws StorageExecuteException {
        if (Storage.TRADE_MODE_CACHE.size() == 0 || Storage.TRADE_MODE_CACHE.entrySet().stream()
                .filter(entry -> {
                    if (entry.getKey()==null) return false;
                    String entryUuid = entry.getKey().getKey();
                    if (entryUuid==null) return false;
                    return entryUuid.equals(playerUuid);
                })
                .anyMatch(entry -> entry.getValue().getTime() + Storage.TRADE_MODE_DELAY < new Date().getTime())) {
            return getTradeMode(playerUuid);
        } else {
            Optional<Map.Entry<String, TradeMode>> optionalEntry = Storage.TRADE_MODE_CACHE.keySet().stream()
                    .filter(entry -> entry.getKey().equals(playerUuid))
                    .findFirst();
            if (optionalEntry.isPresent()) {
                return optionalEntry.get().getValue();
            } else {
                return getTradeMode(playerUuid);
            }
        }
    }

    @Override
    public int getTradeUses(@NotNull UUID player, @NotNull UUID tradeUuid) {
        return 0;
    }
}
