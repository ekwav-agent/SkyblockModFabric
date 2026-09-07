package com.coflnet.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Deterministic bounded state for per-title request coalescing and retry. */
public final class DescriptionRequestTracker {
    public enum Result { ACCEPTED, COALESCED_SUCCESS, COALESCED_PENDING }
    public record Offer(Result result, List<String> evictedTitles) {}
    public record Fingerprint(String inventoryNbt, String storagePosition) {}

    private static final class Slot {
        private Fingerprint queued;
        private Object queuedOrigin;
        private Fingerprint running;
        private Object runningOrigin;
        private Fingerprint successful;
    }

    private final int capacity;
    private final LinkedHashMap<String, Slot> slots = new LinkedHashMap<>(16, 0.75f, true);

    public DescriptionRequestTracker(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public synchronized Offer offer(String title, Fingerprint fingerprint, Object origin) {
        Slot slot = slots.computeIfAbsent(title, ignored -> new Slot());
        if (slot.queued == null && fingerprint.equals(slot.running) && origin == slot.runningOrigin
                || fingerprint.equals(slot.queued) && origin == slot.queuedOrigin) {
            return new Offer(Result.COALESCED_PENDING, List.of());
        }
        if (slot.running == null && slot.queued == null && fingerprint.equals(slot.successful)) {
            return new Offer(Result.COALESCED_SUCCESS, List.of());
        }
        slot.queued = fingerprint;
        slot.queuedOrigin = origin;
        return new Offer(Result.ACCEPTED, trim());
    }

    public synchronized boolean start(String title, Fingerprint fingerprint, Object origin) {
        Slot slot = slots.get(title);
        if (slot == null || !fingerprint.equals(slot.queued) || origin != slot.queuedOrigin) {
            return false;
        }
        slot.queued = null;
        slot.queuedOrigin = null;
        slot.running = fingerprint;
        slot.runningOrigin = origin;
        return true;
    }

    public synchronized void finish(String title, Fingerprint fingerprint, boolean succeeded) {
        Slot slot = slots.get(title);
        if (slot == null || !fingerprint.equals(slot.running)) {
            return;
        }
        slot.running = null;
        slot.runningOrigin = null;
        if (succeeded) {
            slot.successful = fingerprint;
        }
    }

    public synchronized void clear() {
        slots.clear();
    }

    public synchronized void forgetSuccessful(String title, Fingerprint fingerprint) {
        Slot slot = slots.get(title);
        if (slot != null && fingerprint.equals(slot.successful)) {
            slot.successful = null;
        }
    }

    public synchronized int size() {
        return slots.size();
    }

    private List<String> trim() {
        List<String> evicted = new ArrayList<>();
        var iterator = slots.entrySet().iterator();
        while (slots.size() > capacity && iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().running == null) {
                evicted.add(entry.getKey());
                iterator.remove();
            }
        }
        return List.copyOf(evicted);
    }
}
