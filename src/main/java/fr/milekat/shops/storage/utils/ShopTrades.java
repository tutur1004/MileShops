package fr.milekat.shops.storage.utils;

import fr.milekat.shops.api.classes.Trade;

import java.util.List;
import java.util.UUID;

public record ShopTrades(UUID shopUuid, List<Trade> trades) {}
