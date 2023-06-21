package fr.milekat.shops.storage.utils;

import fr.milekat.shops.workers.utils.TradeMode;

import java.util.UUID;

public record PlayerTradeMode(UUID playerUuid, TradeMode tradeMode) {}
