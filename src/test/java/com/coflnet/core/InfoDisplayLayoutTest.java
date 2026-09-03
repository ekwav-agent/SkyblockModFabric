package com.coflnet.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoDisplayLayoutTest {
    private static final int REPRESENTATIVE_DISPLAY_WIDTH = 112;

    @Test
    void defaultBazaarInfoDisplayStaysInBoundsAndOutsideCenteredContainer() {
        for (String title : new String[]{"Co-op Bazaar Orders", "Bazaar ➜ Products"}) {
            assertSafeDefault(title, 1280, 720);
            assertSafeDefault(title, 640, 360);
            assertSafeDefault(title, 426, 240);
        }
    }

    private static void assertSafeDefault(String title, int viewportWidth, int viewportHeight) {
        int containerWidth = 176;
        int containerHeight = 222;
        var container = new InfoDisplayLayout.Rect(
                (viewportWidth - containerWidth) / 2,
                (viewportHeight - containerHeight) / 2,
                containerWidth,
                containerHeight);
        var display = InfoDisplayLayout.placeDefault(title, viewportWidth, viewportHeight,
                container, REPRESENTATIVE_DISPLAY_WIDTH, 45);

        assertTrue(display.isInside(viewportWidth, viewportHeight), title + " display must be in viewport");
        assertFalse(display.intersects(container), title + " display must not overlap container");
    }
}
