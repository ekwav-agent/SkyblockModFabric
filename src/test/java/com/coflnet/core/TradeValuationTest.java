package com.coflnet.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TradeValuationTest {
    @Test void readsAuctionAndBazaarPerItemValues() {
        assertEquals(12_500_000L, TradeValuation.parseWorthFromTips(
                new String[]{"§7Lowest BIN: §612.5m", "§7Med: §6611.8m"}, TradeValuation.WorthBasis.LBIN));
        String bazaar = "Buy: 37.49K (585.8 each)Sell: 33.38K (521.6 each)";
        assertEquals(585L, TradeValuation.parseWorthFromTips(new String[]{bazaar}, TradeValuation.WorthBasis.LBIN));
        assertEquals(521L, TradeValuation.parseWorthFromTips(new String[]{bazaar}, TradeValuation.WorthBasis.MEDIAN));
    }

    @Test void readsFormattedCoinOffer() {
        assertEquals(1_500_000L, TradeValuation.parseCoinOffer("§61.5m coins"));
        assertNull(TradeValuation.parseCoinOffer("§x1.5m coins"));
        assertNull(TradeValuation.parseCoinOffer("§6Golden Dragon"));
    }

    @Test void totalsPricedAndUnpricedTradeItems() {
        var side = TradeValuation.sum(List.of(1_500_000L, 611_800_000L));
        assertEquals(613_300_000L, side.total());
        assertEquals(0, side.unpriced());
        var withUnpriced = TradeValuation.sum(java.util.Arrays.asList(67L, null));
        assertEquals(67L, withUnpriced.total());
        assertEquals(1, withUnpriced.unpriced());
    }
}
