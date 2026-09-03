package com.coflnet.core;

import java.util.Objects;

/** Pure default placement for the container-side InfoDisplay. */
public final class InfoDisplayLayout {
    private static final int GAP = 5;

    private InfoDisplayLayout() {
    }

    public static Rect placeDefault(String menuTitle, int viewportWidth, int viewportHeight,
                                    Rect container, int displayWidth, int displayHeight) {
        Objects.requireNonNull(menuTitle, "menuTitle");
        if (viewportWidth <= 0 || viewportHeight <= 0 || displayWidth <= 0 || displayHeight <= 0) {
            throw new IllegalArgumentException("viewport and display dimensions must be positive");
        }

        int y = clamp(container.y() + GAP, 0, viewportHeight - displayHeight);
        int leftX = container.x() - GAP - displayWidth;
        if (leftX >= 0) {
            return new Rect(leftX, y, displayWidth, displayHeight);
        }

        int rightX = container.right() + GAP;
        if (rightX + displayWidth <= viewportWidth) {
            return new Rect(rightX, y, displayWidth, displayHeight);
        }

        int x = clamp(container.x(), 0, viewportWidth - displayWidth);
        int aboveY = container.y() - GAP - displayHeight;
        if (aboveY >= 0) {
            return new Rect(x, aboveY, displayWidth, displayHeight);
        }
        return new Rect(x, clamp(container.bottom() + GAP, 0, viewportHeight - displayHeight),
                displayWidth, displayHeight);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean intersects(Rect other) {
            return x < other.right() && right() > other.x()
                    && y < other.bottom() && bottom() > other.y();
        }

        public boolean isInside(int viewportWidth, int viewportHeight) {
            return x >= 0 && y >= 0 && right() <= viewportWidth && bottom() <= viewportHeight;
        }
    }
}
