package com.coflnet.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuClassifierTest {
    @Test void pinsHypixelStorageTitles() {
        assertTrue(MenuClassifier.isStorageChest("Ender Chest (2/9)"));
        assertTrue(MenuClassifier.isStorageChest("Large Backpack (Slot #3)"));
        assertTrue(MenuClassifier.isStorageChest("Hunting Toolkit"));
        assertFalse(MenuClassifier.isStorageChest("Bazaar ➜ Oddities"));
    }

    @Test void requiresHypixelTradeShapeAndDivider() {
        assertTrue(MenuClassifier.isTradeTitle(45, "You     VerticleFr"));
        assertTrue(MenuClassifier.isTradeMenu(45, "You     VerticleFr",
                new boolean[]{true, true, true, true, true}));
        assertFalse(MenuClassifier.isTradeMenu(45, "You     VerticleFr",
                new boolean[]{true, true, false, true, true}));
        assertFalse(MenuClassifier.isTradeTitle(54, "You     VerticleFr"));
    }
}
