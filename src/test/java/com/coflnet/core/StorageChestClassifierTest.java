package com.coflnet.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageChestClassifierTest {
    @Test void pinsHypixelStorageTitles() {
        assertTrue(MenuClassifier.isStorageChest("Ender Chest (2/9)"));
        assertTrue(MenuClassifier.isStorageChest("Large Backpack (Slot #3)"));
        assertTrue(MenuClassifier.isStorageChest("Hunting Toolkit"));
        assertFalse(MenuClassifier.isStorageChest("Bazaar ➜ Oddities"));
    }
}
