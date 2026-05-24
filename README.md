# MileShops - Minecraft Trading Shop Plugin

[![GitHub Release](https://img.shields.io/github/v/release/tutur1004/MileShops?style=flat-square)](https://github.com/tutur1004/MileShops/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/fr.milekat/mile-shops-api?style=flat-square&label=Maven%20Central)](https://central.sonatype.com/artifact/fr.milekat/mile-shops-api)
[![Javadoc](https://img.shields.io/badge/javadoc.io-mile--shops--api-blue?style=flat-square)](https://javadoc.io/doc/fr.milekat/mile-shops-api)
[![GitHub Issues](https://img.shields.io/github/issues/tutur1004/MileShops?style=flat-square)](https://github.com/tutur1004/MileShops/issues)

MileShops is a Minecraft plugin developed by Milekat, designed to bring a flexible and feature-rich trading shop system to your server.
It supports multiple shop layouts, complex item and money trades, per-player modifiers, and dual-backend storage (SQL or Elasticsearch).

## Features

- **Command: /shop** — requires `shops.command` permission
  - Create, edit, open, list, and delete shops in-game
  - Set per-player trade result multipliers

- **Multiple Shop Types**
  - `VANILLA` — uses the native Minecraft villager trading UI
  - `BASIC_FOUR`, `DOUBLE_SIX`, `SINGLE_ONE`, `SINGLE_SIX` — chest-based layouts of varying sizes
  - `COMPACT_ONE`, `COMPACT_FIVE` — compact centered layouts for minimal UI footprint

- **Advanced Trade System**
  - Item-to-item trades with optional second ingredient
  - Item-to-money trades (requires MileBanks)
  - Wildcard item matching via Minecraft item tags (e.g. all wool types matched by `wool_carpet`)
  - Per-trade usage limits tracked per player through the tag system
  - Per-player result multipliers (e.g. VIP discount)

- **Trade Modes**
  - Players can trade from `INVENTORY`, `ENDER_CHEST`, `SHULKER` boxes, or `END_SHULKER` (all combined)
  - Default mode is configurable; players can toggle between modes in the shop UI

- **NPC Integration**
  - Create NPC vendors via the MileNpc plugin
  - Shops can be bound to NPCs with custom textures and nameplates

- **Timed Shops**
  - Shops with configurable spawn-in / spawn-out dates
  - NPCs appear and disappear automatically when the shop is active or inactive

- **Inventory-Based Admin Editor**
  - Create and edit shops entirely through an in-game GUI
  - No manual config file editing required

- **Dual Storage Backend**
  - ElasticSearch ≥ 8 or SQL (MySQL, MariaDB, PostgreSQL)
  - Configurable in-memory cache with TTL for performance

- **Events API**
  - `PlayerOpenShopEvent` — fired (and cancellable) when a player opens a shop
  - `TradeCompleteEvent` — fired (and cancellable) when a player completes a trade

## Requirements

- **Java** 21
- **Paper** 1.21+
- **MileBanks** (optional) — required for money-based trades
- **MileNpc** (optional) — required for NPC vendor support
- **Elasticsearch** ≥ 8 **or** MySQL / MariaDB / PostgreSQL — for storage

## Installation

1. Download the plugin jar from the [MileShops GitHub repository](https://github.com/tutur1004/MileShops).
2. Start your server once to generate `config.yml`, then stop it.
3. Fill in the storage connection details in `config.yml`.
4. Restart the server.

## Configuration

```yaml
debug: false

storage:
  type: ElasticSearch  # ElasticSearch | sql
  sql:
    prefix: "shop_"
    hostname: "localhost"
    port: "3306"
    database: "minecraft"
    username: "user"
    password: "pass"
  elasticsearch:
    prefix: "shop-"
    hostname: "elasticsearch"
    port: "9200"
    username: "user"
    password: "pass"
  cache:
    enabled: true
    time: 300  # Cache TTL in seconds

settings:
  default-trade-mode: "INVENTORY"  # INVENTORY | ENDER_CHEST | SHULKER | END_SHULKER

tags:
  enable_builtin_tags: true  # Adds player-uuid and player-name automatically
  custom:
    string: ["player-name", "player-uuid"]
    integer: []
```

## Commands

All subcommands require the base `shops.command` permission. Administrative subcommands additionally require their own node (default: op).

| Command | Permission | Description |
|---|---|---|
| `/shop list [page]` | `shops.list` | List all shops (8 per page) |
| `/shop open <name>` | `shops.open` | Open a shop |
| `/shop create <type> <name>` | `shops.create` | Create a new shop |
| `/shop edit <name>` | `shops.edit` | Open the admin editor for a shop |
| `/shop type <name> <newType>` | `shops.type` | Change a shop's layout type |
| `/shop remove <name>` | `shops.remove` | Delete a shop |
| `/shop modifier <player> <value>` | `shops.modifier` | Set a player's trade result multiplier |
| `/shop reload` | `shops.reload` | Reload config and all shops |

**Available shop types:** `VANILLA`, `BASIC_FOUR`, `DOUBLE_SIX`, `SINGLE_ONE`, `SINGLE_SIX`, `COMPACT_ONE`, `COMPACT_FIVE`

## API

Add `mile-shops-api` to your project via [MavenCentral](https://central.sonatype.com/artifact/fr.milekat/mile-shops-api).

```java
RegisteredServiceProvider<MileShopsIAPI> provider = Bukkit.getServicesManager().getRegistration(MileShopsIAPI.class);
if (provider == null) return; // Plugin not loaded

MileShopsIAPI api = provider.getProvider();

// --- Retrieve shops and trades ---
List<Shop> shops = api.getShops();
List<Trade> trades = api.getShopTrades(shop);

// --- Open a shop for a player ---
api.openShop(player.getUniqueId(), shop);

// --- Execute a trade programmatically ---
int count = api.processedTrades(player, TradeMode.INVENTORY, shop, trade, false);

// --- Player trade modifiers ---
api.setPlayerModifier(player.getUniqueId(), 1.5); // 50 % bonus
double modifier = api.getPlayerModifier(player.getUniqueId());
api.resetPlayerModifier(player.getUniqueId());

// --- Custom player tags ---
Map<String, Object> tags = new HashMap<>();
tags.put("player-uuid", player.getUniqueId().toString());
tags.put("player-name", player.getName());
api.setPlayerTags(player.getUniqueId(), tags);

Map<String, Object> stored = api.getPlayerTags(player.getUniqueId());
api.removePlayerTags(player.getUniqueId());
```

### Key API methods

| Method | Description |
|---|---|
| `getShops()` | All registered shops |
| `getShopTrades(Shop)` | All trades for a given shop |
| `getShopTrades(UUID)` | All trades for a shop by UUID |
| `getShopTrades(String)` | All trades for a shop by name |
| `openShop(UUID, Shop)` | Open a shop inventory for a player |
| `openAdminShop(UUID, Shop)` | Open the admin editor for a player |
| `processedTrade(Player, Shop, Trade)` | Execute a single trade from inventory |
| `processedTrades(Player, TradeMode, Shop, Trade, boolean)` | Execute trade(s) with mode and multi-trade support |
| `getPlayerModifier(UUID)` | Get a player's result multiplier |
| `setPlayerModifier(UUID, double)` | Set a player's result multiplier |
| `resetPlayerModifier(UUID)` | Reset a player's modifier to default |
| `getPlayerTags(UUID)` | Retrieve a player's registered tags |
| `setPlayerTags(UUID, Map)` | Register or update a player's tags |
| `removePlayerTags(UUID)` | Remove all tags for a player |

### Events

```java
@EventHandler
public void onShopOpen(PlayerOpenShopEvent event) {
    event.getPlayer();
    event.getShop();
    event.setCancelled(true); // Prevent opening
}

@EventHandler
public void onTradeComplete(TradeCompleteEvent event) {
    event.getPlayer();
    event.getShop();
    event.getTrade();
    event.setCancelled(true); // Cancel the trade
}
```

## Credits

- **Developer:** Milekat — [GitHub](https://github.com/tutur1004)

## Support

Report issues at [MileShops GitHub Issues](https://github.com/tutur1004/MileShops/issues).
