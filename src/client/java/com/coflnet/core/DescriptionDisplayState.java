package com.coflnet.core;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/** Session-scoped, bounded state for container-attached description displays. */
public final class DescriptionDisplayState {
    public record Line(String type, String value, int line) {}

    public record Request(long session, long sequence, long activation, String title, Object menuIdentity) {}

    public record Selection(List<Line> lines) {}

    public record Completion(boolean accepted, boolean appliesToActiveMenu, boolean changed, List<Line> lines) {
        private static Completion stale() {
            return new Completion(false, false, false, List.of());
        }
    }

    private final int capacity;
    private final LinkedHashMap<String, List<Line>> displays = new LinkedHashMap<>(16, 0.75f, true);
    private final LinkedHashMap<String, Long> latestRequests = new LinkedHashMap<>(16, 0.75f, true);
    private long session;
    private long nextSequence;
    private long nextActivation;
    private long verifiedActivation = -1;
    private Request activeMenu;
    private int activeContainerId = -1;
    private int expectedContainerSlots;
    private BitSet observedContainerSlots = new BitSet();
    private boolean containerReady;

    public DescriptionDisplayState(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized Selection activate(String title, Object menuIdentity) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(menuIdentity, "menuIdentity");
        if (activeMenu != null && activeMenu.title().equals(title)
                && activeMenu.menuIdentity() == menuIdentity) {
            return new Selection(displays.getOrDefault(title, List.of()));
        }
        activeMenu = new Request(session, 0, ++nextActivation, title, menuIdentity);
        verifiedActivation = -1;
        activeContainerId = -1;
        expectedContainerSlots = 0;
        observedContainerSlots.clear();
        containerReady = false;
        List<Line> cached = displays.getOrDefault(title, List.of());
        return new Selection(cached);
    }

    public synchronized Request beginRequest(String title, Object menuIdentity) {
        Objects.requireNonNull(title, "title");
        long activation = activeMenu != null
                && activeMenu.title().equals(title)
                && activeMenu.menuIdentity() == menuIdentity
                ? activeMenu.activation() : -1;
        Request request = new Request(session, ++nextSequence, activation, title, menuIdentity);
        latestRequests.put(title, request.sequence());
        trim(latestRequests);
        return request;
    }

    public synchronized Completion complete(Request request, List<Line> response) {
        List<Line> snapshot = List.copyOf(response);
        Long latest = latestRequests.get(request.title());
        if (request.session() != session || latest == null || latest != request.sequence()) {
            return Completion.stale();
        }

        List<Line> previous = displays.get(request.title());
        boolean changed = !snapshot.equals(previous);
        if (changed) {
            displays.put(request.title(), snapshot);
            trim(displays);
        }
        boolean applies = activeMenu != null
                && activeMenu.session() == request.session()
                && activeMenu.activation() == request.activation()
                && activeMenu.title().equals(request.title())
                && activeMenu.menuIdentity() == request.menuIdentity();
        if (applies) {
            verifiedActivation = activeMenu.activation();
        }
        return new Completion(true, applies, changed, snapshot);
    }

    public synchronized boolean confirmCached(String title, Object menuIdentity) {
        if (activeMenu != null && activeMenu.title().equals(title)
                && activeMenu.menuIdentity() == menuIdentity && displays.containsKey(title)) {
            verifiedActivation = activeMenu.activation();
            return true;
        }
        return false;
    }

    public synchronized boolean isActiveAndVerified(Object menuIdentity) {
        return activeMenu != null && activeMenu.menuIdentity() == menuIdentity
                && verifiedActivation == activeMenu.activation();
    }

    public synchronized void watchContainer(Object menuIdentity, int containerId, int containerSlots) {
        if (activeMenu == null || activeMenu.menuIdentity() != menuIdentity || containerSlots < 0) {
            return;
        }
        if (activeContainerId == containerId && expectedContainerSlots == containerSlots) {
            return;
        }
        activeContainerId = containerId;
        expectedContainerSlots = containerSlots;
        observedContainerSlots = new BitSet(containerSlots);
        containerReady = false;
    }

    public synchronized boolean fullContentApplied(Object menuIdentity, int containerId) {
        if (!matchesContainer(menuIdentity, containerId)) {
            return false;
        }
        containerReady = true;
        return true;
    }

    public synchronized boolean slotApplied(Object menuIdentity, int containerId, int slot) {
        if (!matchesContainer(menuIdentity, containerId) || slot < 0 || slot >= expectedContainerSlots) {
            return false;
        }
        if (containerReady) {
            return true;
        }
        observedContainerSlots.set(slot);
        if (observedContainerSlots.cardinality() != expectedContainerSlots) {
            return false;
        }
        containerReady = true;
        return true;
    }

    public synchronized boolean resultSlotApplied(
            Object menuIdentity, int containerId, int slot, String itemTitle) {
        if (!matchesContainer(menuIdentity, containerId) || slot < 0
                || (expectedContainerSlots > 0 && slot >= expectedContainerSlots)) {
            return false;
        }
        return itemTitle.contains("Combine Items")
                || itemTitle.equals("§aFlip Order")
                || itemTitle.contains("AUCTION FOR");
    }

    public synchronized boolean isActive(Request request) {
        return activeMenu != null
                && activeMenu.session() == request.session()
                && activeMenu.activation() == request.activation()
                && activeMenu.title().equals(request.title())
                && activeMenu.menuIdentity() == request.menuIdentity();
    }

    public synchronized void clearSession() {
        session++;
        displays.clear();
        latestRequests.clear();
        activeMenu = null;
        verifiedActivation = -1;
        activeContainerId = -1;
        expectedContainerSlots = 0;
        observedContainerSlots.clear();
        containerReady = false;
    }

    public synchronized int size() {
        return displays.size();
    }

    private <V> void trim(LinkedHashMap<String, V> map) {
        while (map.size() > capacity) {
            String eldest = map.keySet().iterator().next();
            map.remove(eldest);
        }
    }

    private boolean matchesContainer(Object menuIdentity, int containerId) {
        return activeMenu != null && activeMenu.menuIdentity() == menuIdentity
                && activeContainerId == containerId;
    }
}
