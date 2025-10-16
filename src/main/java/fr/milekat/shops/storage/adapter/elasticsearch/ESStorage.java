package fr.milekat.shops.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.CacheManager;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.milekat.utils.Configs;
import fr.milekat.utils.DateMileKat;
import fr.milekat.utils.storage.adapter.elasticsearch.connection.ESConnection;
import fr.milekat.utils.storage.adapter.elasticsearch.features.Index;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.milekat.utils.storage.exceptions.StorageLoadException;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class ESStorage implements StorageImplementation {
    // Elastic settings
    private final ESConnection connection;
    private final String numberOfReplicas;

    // Indexes settings
    private final String INDEX_TRADES;
    private final String INDEX_SHOPS;
    private final String INDEX_USER_MODES;
    private final String INDEX_TRADE_LOGS;
    private final Map<String, Class<?>> trade_logs_fields = new HashMap<>();
    private final List<BulkOperation> logToProcess = new ArrayList<>();

    /*
        Main DB
    */
    public ESStorage(@NotNull ESConnection connection, @NotNull Configs config) throws StorageLoadException {
        this.connection = connection;
        String prefix = config.getString("storage.elasticsearch.prefix", "shop-");
        if (!prefix.matches("[a-z0-9][a-z0-9-]{0,19}")) {
            throw new StorageLoadException("Elasticsearch prefix wrong, please only lower cases (a-z), " +
                    "digits (0-9) and dashes '-', also you can't start with a dash '-'.");
        }
        this.INDEX_TRADES = prefix + "trades";
        this.INDEX_SHOPS = prefix + "shops";
        this.INDEX_USER_MODES = prefix + "user-modes";
        this.INDEX_TRADE_LOGS = prefix + "trade-logs";
        this.numberOfReplicas = config.getString("storage.elasticsearch.replicas", "0");
        trade_logs_fields.put("trade", Trade.class);
        trade_logs_fields.put("@timestamp", Date.class);
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            Main.getMileLogger().debug(esClient.cluster().health().toString());
            logPool();
        } catch (IOException exception) {
            throw new StorageLoadException("Error while trying to load ElasticSearch cluster");
        }
    }

    private @NotNull JacksonJsonpMapper getMapper() {
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        mapper.objectMapper().registerModule(new CustomMappers().getModule());
        return mapper;
    }

    @Override
    public boolean checkStorages() {
        Main.getMileLogger().debug("Check if storage is ready...");
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            Main.getMileLogger().debug("Check indices...");
            for (String index : List.of(INDEX_TRADES, INDEX_SHOPS, INDEX_USER_MODES)) {
                //  Check if index exist, otherwise create it
                if (!esClient
                        .indices()
                        .exists(ExistsRequest.of(builder -> builder.index(index)))
                        .value()) {
                    esClient.indices().create(c ->
                            c.index(index).settings(s -> s.numberOfReplicas("0")));
                } else {
                    Main.getMileLogger().debug("Index '" + index + "' found !");
                }
            }
            new Index(esClient, INDEX_TRADE_LOGS, numberOfReplicas,
                    trade_logs_fields, Main.TAGS, "tags");
            Main.getMileLogger().debug("Storage is ready.");
            return true;
        } catch (StorageLoadException | IOException exception) {
            Main.getMileLogger().warning("Error while trying to load ElasticSearch indices.");
            Main.getMileLogger().stack(exception.getStackTrace());
        }
        return false;
    }

    @Override
    public void disconnect() {
        Main.getMileLogger().debug("ElasticSearch automatically close connections after execution, using try-with-resources.");
    }

    /*
        ES Queries execution
     */
    @Override
    public void asyncSaveShop(@NotNull Shop shop, boolean createIfNotExist, CommandSender sender) {
        //  Open Bukkit async task
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            //  Open Elasticsearch connection
            try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
                Main.getMileLogger().debug("[ES-aSync] prepareShopAsync - search shop with field 'name' and " +
                        "value '" + shop.getName() + "'.");
                try {
                    SearchResponse<Shop> searchResponse = esClient.search(
                            new SearchRequest.Builder()
                                    .index(INDEX_SHOPS)
                                    .query(q -> q.term(t -> t
                                            .field("name.keyword")
                                            .value(shop.getName())
                                    ))
                                    .size(1)
                                    .build(),
                            Shop.class
                    );
                    // Check if shop exist
                    if (searchResponse.hits().total() != null && searchResponse.hits().total().value() > 0) {
                        //  Update existing shop
                        Main.getMileLogger().debug("[ES-aSync] asyncSaveShop - update shop " + shop.getUuid() + ".");
                        try {
                            esClient.update(u -> u
                                            .index(INDEX_SHOPS)
                                            .id(searchResponse.hits().hits().getFirst().id())
                                            .doc(shop)
                                            .docAsUpsert(true),
                                    Shop.class);
                            Main.getMileLogger().info("Shop '" + shop.getName() + "' has been updated");
                            Main.message(sender, "&2Shop updated !");
                        } catch (ElasticsearchException | IOException exception) {
                            Main.message(sender, "&cError while trying to update shop " + shop.getName());
                            Main.getMileLogger().warning("Error while trying to update shop with uuid " + shop.getUuid());
                            Main.getMileLogger().stack(exception.getStackTrace());
                            shop.removeNpc();
                        }
                    } else {
                        if (!createIfNotExist) {
                            Main.message(sender, "&cShop with name '" + shop.getName() + "' do not exist.");
                            shop.removeNpc();
                            return;
                        }
                        //  Save new shop
                        Main.getMileLogger().debug("[ES-aSync] asyncSaveShop - index new shop " + shop.getUuid() + ".");
                        try {
                            esClient.index(i -> i
                                    .index(INDEX_SHOPS)
                                    .document(shop));
                            Main.getMileLogger().info("New shop created with name " + shop.getName());
                            Main.message(sender, "&2Shop created !");
                        } catch (ElasticsearchException | IOException exception) {
                            Main.message(sender, "&cError while trying to create shop " + shop.getName());
                            Main.getMileLogger().warning("Error while trying to index shop with uuid " + shop.getUuid());
                            Main.getMileLogger().stack(exception.getStackTrace());
                            shop.removeNpc();
                        }
                    }
                    //  Update cache
                    this.getShop(shop.getName());
                } catch (ElasticsearchException | IOException | StorageExecuteException exception) {
                    Main.message(sender, "&cError while trying to save shop " + shop.getName());
                    Main.getMileLogger().warning("Error while trying to fetch shop with uuid " + shop.getName());
                    Main.getMileLogger().stack(exception.getStackTrace());
                    shop.removeNpc();
                }
            } catch (IOException exception) {
                Main.message(sender, "&cError while trying to connect to storage.");
                Main.getMileLogger().warning("Error while trying to connect to ElasticSearch.");
                Main.getMileLogger().stack(exception.getStackTrace());
                shop.removeNpc();
            }
        });
    }

    @Override
    public Shop getShop(@NotNull UUID uuid) throws StorageExecuteException {
        try {
            return getShop("uuid.keyword", uuid.toString());
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find shop with playerUuid " + uuid);
        }
    }

    @Override
    public Shop getShop(@NotNull String name) throws StorageExecuteException {
        try {
            return getShop("name.keyword", name);
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to find shop with name " + name);
        }
    }

    private @Nullable Shop getShop(@NotNull String field, @NotNull String value) throws IOException {
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            Main.getMileLogger().debug("[ES-Sync] getShop - search shop with field '" + field +
                    "' and value '" + value + "'.");
            SearchResponse<Shop> response = esClient.search(
                    new SearchRequest.Builder()
                            .index(INDEX_SHOPS)
                            .query(q -> q.term(t -> t
                                    .field(field)
                                    .value(value)
                            ))
                            .size(1)
                            .build(),
                    Shop.class
            );
            Optional<Hit<Shop>> shop = response.hits().hits().stream().findFirst();
            if (shop.isPresent() && shop.get().source() != null) {
                CacheManager.addCache(Main.SHOP_CACHE, shop.get().source());
                return shop.get().source();
            }
            return null;
        }
    }

    @Override
    public @NotNull List<Shop> getAllShops() throws StorageExecuteException {
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            try {
                Main.getMileLogger().debug("[ES-Sync] getAllShops - Fetch all shops.");
                SearchResponse<Shop> response = esClient.search(
                        new SearchRequest.Builder()
                                .index(INDEX_SHOPS)
                                .size(1024)
                                .build(),
                        Shop.class);
                List<Shop> shops = new ArrayList<>();
                response.hits().hits().forEach(hit -> shops.add(hit.source()));
                shops.removeIf(Objects::isNull);
                //  Order by name
                shops.sort(Comparator.comparing(Shop::getName, String.CASE_INSENSITIVE_ORDER));
                Main.SHOP_CACHE = shops.stream().collect(HashMap::new,
                        ((map, shop) -> map.put(shop, new Date())), Map::putAll);
                return shops;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception, "Error while trying to fetch all shops.");
            }
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to connect to ElasticSearch.");
        }
    }

    @Override
    public void asyncDeleteShop(@NotNull Shop shop, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
                esClient.deleteByQuery(new DeleteByQueryRequest.Builder()
                        .index(INDEX_TRADES)
                        .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shop.getUuid()))))
                        .build());
                esClient.deleteByQuery(new DeleteByQueryRequest.Builder()
                        .index(INDEX_SHOPS)
                        .query(q -> q.match(m -> m.field("uuid").query(String.valueOf(shop.getUuid()))))
                        .build());
                Main.getMileLogger().info("Shop '" + shop.getName() + "' has been deleted");
                if (sender != null) {
                    Main.message(sender, "&2Shop deleted !");
                }
            } catch (ElasticsearchException | IOException e) {
                Main.getMileLogger().warning("Error while trying to fetch trades for shop with uuid " + shop.getUuid());
                Main.getMileLogger().stack(e.getStackTrace());
            }
        });
    }

    @Override
    public void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
                List<BulkOperation> bulkDocs = new ArrayList<>();
                trades.forEach(trade -> bulkDocs
                        .add(new BulkOperation.Builder().create(c -> c.document(trade)).build()));
                Main.getMileLogger().debug("[ES-aSync] asyncSaveShopTrades - delete all trades for shop '" + shop.getUuid() + "'.");
                try {
                    esClient.deleteByQuery(new DeleteByQueryRequest.Builder()
                            .index(INDEX_TRADES)
                            .query(q -> q.match(m -> m.field("shopUuid").query(String.valueOf(shop.getUuid()))))
                            .build());
                } catch (ElasticsearchException | IOException exception) {
                    Main.message(sender, "&cError while trying to update trades");
                    Main.getMileLogger().warning("Error while trying to remove trades from shop with uuid " + shop.getUuid());
                    Main.getMileLogger().stack(exception.getStackTrace());
                    return;
                }
                Main.getMileLogger().debug("[ES-aSync] asyncSaveShopTrades - index all trades for shop '" + shop.getUuid() + "'.");
                try {
                    esClient.bulk(new BulkRequest.Builder()
                            .index(INDEX_TRADES)
                            .operations(bulkDocs)
                            .build());
                } catch (ElasticsearchException | IOException exception) {
                    Main.message(sender, "&cError while trying to update trades");
                    Main.getMileLogger().warning("Error while trying to save trades from shop with uuid " + shop.getUuid());
                    Main.getMileLogger().stack(exception.getStackTrace());
                    return;
                }
                Main.message(sender, "&2Trades saved, &6Updating trades..");
                CacheManager.addCache(Main.TRADE_CACHE, new ShopTrades(shop.getUuid(), trades));
                Main.message(sender, "&2Trades updated !");
                Main.getMileLogger().info("Trades from shop " + shop.getName() + " updated");
            } catch (IOException exception) {
                Main.message(sender, "&cError while trying to connect to storage.");
                Main.getMileLogger().warning("Error while trying to connect to ElasticSearch.");
                Main.getMileLogger().stack(exception.getStackTrace());
            }
        });
    }

    @Override
    public List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            try {
                Main.getMileLogger().debug("[ES-Sync] getTrades - Get all trades of shop '" + shopUuid + "'.");
                SearchResponse<Trade> response = esClient.search(
                        new SearchRequest.Builder()
                                .index(INDEX_TRADES)
                                .query(q -> q.term(t -> t
                                        .field("shopUuid.keyword")
                                        .value(shopUuid.toString())
                                ))
                                .size(64)
                                .build(),
                        Trade.class);
                List<Trade> trades = new ArrayList<>();
                response.hits().hits().forEach(hit -> trades.add(hit.source()));
                CacheManager.addCache(Main.TRADE_CACHE, new ShopTrades(shopUuid, trades));
                Main.getMileLogger().debug("Found '" + trades.size() + "' trades for shop '" + shopUuid + "'.");
                return trades;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception,
                        "Error while trying to find trades for shop uuid " + shopUuid);
            }
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to connect to ElasticSearch.");
        }
    }

    @Override
    public List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = getShop(shopName).getUuid();
        return getTrades(shopUuid);
    }

    @Override
    public void saveTradeMode(@NotNull UUID playerUuid, @NotNull TradeMode mode) {
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            PlayerTradeMode playerMode = new PlayerTradeMode(playerUuid, mode);
            Main.getMileLogger().debug("[ES-aSync] saveTradeMode - search users-mode");
            try {
                SearchResponse<PlayerTradeMode> searchResponse = esClient.search(
                        new SearchRequest.Builder()
                                .index(INDEX_USER_MODES)
                                .query(q -> q.term(t -> t
                                        .field("playerUuid.keyword")
                                        .value(playerUuid.toString())
                                ))
                                .size(1)
                                .build(),
                        PlayerTradeMode.class
                );
                int results = searchResponse.hits().hits().size();
                try {
                    if (results == 0) {
                        Main.getMileLogger().debug("[ES-aSync] saveTradeMode - index users-mode");

                        esClient.index(c -> c
                                .index(INDEX_USER_MODES)
                                .document(playerMode)
                        );
                    } else {
                        Main.getMileLogger().debug("[ES-aSync] saveTradeMode - update users-mode");
                        esClient.update(u -> u
                                        .index(INDEX_USER_MODES)
                                        .id(searchResponse.hits().hits().getFirst().id())
                                        .doc(playerMode)
                                        .docAsUpsert(true),
                                PlayerTradeMode.class
                        );
                    }
                    CacheManager.addCache(Main.TRADE_MODE_CACHE, playerMode);
                } catch (ElasticsearchException | IOException exception) {
                    Main.getMileLogger().warning("Error while saving TradeMode for " + playerUuid);
                    Main.getMileLogger().stack(exception.getStackTrace());
                }
            } catch (ElasticsearchException | IOException exception) {
                Main.getMileLogger().warning("Error while searching TradeMode for " + playerUuid);
                Main.getMileLogger().stack(exception.getStackTrace());
            }
        } catch (IOException exception) {
            Main.getMileLogger().warning("Error while trying to connect to ElasticSearch.");
            Main.getMileLogger().stack(exception.getStackTrace());
        }
    }

    @NotNull
    private static TradeMode getTradeMode(@NotNull SearchResponse<PlayerTradeMode> searchResponse) {
        TradeMode tradeMode = TradeUtils.getDefaultTradeMode();
        if (!searchResponse.hits().hits().isEmpty() && searchResponse.hits().hits().getFirst().source() != null) {
            PlayerTradeMode playerTradeMode = searchResponse.hits().hits().getFirst().source();
            if (playerTradeMode != null) {
                tradeMode = playerTradeMode.tradeMode();
            }
        }
        if (tradeMode == null) {
            tradeMode = TradeUtils.getDefaultTradeMode();
        }
        return tradeMode;
    }

    @Override
    public TradeMode getTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            Main.getMileLogger().debug("[ES-Sync] getTradeMode - search users-mode of '" + playerUuid + "'.");
            try {
                SearchResponse<PlayerTradeMode> searchResponse = esClient.search(
                        new SearchRequest.Builder()
                                .index(INDEX_USER_MODES)
                                .query(q -> q.term(t -> t
                                        .field("playerUuid.keyword")
                                        .value(playerUuid.toString())
                                ))
                                .size(1)
                                .build(),
                        PlayerTradeMode.class);
                TradeMode tradeMode = getTradeMode(searchResponse);
                CacheManager.addCache(Main.TRADE_MODE_CACHE, new PlayerTradeMode(playerUuid, tradeMode));
                return tradeMode;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception,
                        "Error while trying to fetch trade mode of player " + playerUuid);
            }
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to connect to ElasticSearch.");
        }
    }

    @Override
    public int getTradeUses(@NotNull Map<String, Object> tags, @NotNull Trade trade) {
        if (!trade.isUsageLimited()) return 0;
        try {
            try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
                Main.getMileLogger().debug("[ES-Sync] getTradeUses - search trades uses for trade '" +
                        trade.getShopUuid().toString() + "-" + trade.getTradePosition() +
                        "' with tags '" + tags + "'.");
                CountRequest countRequest = new CountRequest.Builder()
                    .index(INDEX_TRADE_LOGS)
                    .query(q -> {
                        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
                        boolQueryBuilder.filter(f -> f.term(t -> t
                                .field("trade.shopUuid.keyword")
                                .value(trade.getShopUuid().toString())));
                        boolQueryBuilder.filter(f -> f.term(t -> t
                                .field("trade.position")
                                .value(FieldValue.of(trade.getTradePosition()))));
                        tags.forEach((tag, value) -> boolQueryBuilder.must(m -> m.match(ma -> ma
                                .field("tags." + tag + ".keyword")
                                .query(FieldValue.of(value))
                        )));
                        return q.bool(boolQueryBuilder.build());
                    })
                    .build();
                CountResponse response = esClient.count(countRequest);
                return Math.round(response.count());
            }
        } catch (ElasticsearchException | IOException exception) {
            Main.getMileLogger().warning("Error while trying to fetch trade uses with tags " + tags);
            Main.getMileLogger().stack(exception.getStackTrace());
        }
        return 0;
    }

    @Override
    public void logTrade(@Nullable Map<String, Object> tags, @NotNull Trade trade) {
        Map<String, Object> log = new HashMap<>();
        log.put("tags", tags);
        log.put("trade", trade);
        log.put("@timestamp", DateMileKat.getDateEs());
        logToProcess.add(
                new BulkOperation.Builder().create(
                        new CreateOperation.Builder<>()
                                .index(INDEX_TRADE_LOGS)
                                .document(log)
                                .build()
                ).build()
        );
    }

    private void logPool() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            List<BulkOperation> processing = new ArrayList<>(logToProcess);
            logToProcess.clear();
            if (!processing.isEmpty()) {
                try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
                    esClient.bulk(new BulkRequest.Builder().operations(processing).build());
                    Main.getMileLogger().debug("'" + processing.size() + "' log trades saved.");
                } catch (ElasticsearchException | IOException exception) {
                    logToProcess.addAll(processing);
                    Main.getMileLogger().warning("Error while trying to save Trade Logs.");
                    Main.getMileLogger().stack(exception.getStackTrace());
                }
            }
        }, 50, 100);
    }
}
