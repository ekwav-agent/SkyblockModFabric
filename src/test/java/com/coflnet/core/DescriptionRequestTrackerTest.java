package com.coflnet.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Reflection keeps this behavioral regression runnable against the pinned base. */
class DescriptionRequestTrackerTest {
    @Test
    void coalescesOnlySameMenuWorkAndDoesNotSuppressAnotherTitle() throws Exception {
        var tracker = new Tracker(4);
        Object menuA = new Object();
        assertEquals("ACCEPTED", tracker.offer("A", "same-nbt", "", menuA));
        assertEquals("COALESCED_PENDING", tracker.offer("A", "same-nbt", "", menuA));
        assertEquals("ACCEPTED", tracker.offer("B", "same-nbt", "", new Object()));
        assertEquals("ACCEPTED", tracker.offer("A", "same-nbt", "", new Object()));
    }

    @Test
    void fullContentAndIncrementalSignalsCoalesceWhileFirstSnapshotIsPending() throws Exception {
        var tracker = new Tracker(4);
        Object menu = new Object();
        assertEquals("ACCEPTED", tracker.offer("A", "complete-snapshot", "", menu));
        assertEquals("COALESCED_PENDING", tracker.offer("A", "complete-snapshot", "", menu));
    }

    @Test
    void returningToRunningInventoryReplacesSupersededQueuedWork() throws Exception {
        var tracker = new Tracker(4);
        var displays = new DescriptionDisplayState(4);
        Object menu = new Object();
        displays.activate("A", menu);
        tracker.offer("A", "F1", "", menu);
        var first = displays.beginRequest("A", menu);
        assertTrue(tracker.start("A", "F1", "", menu));
        tracker.offer("A", "F2", "", menu);
        displays.beginRequest("A", menu);

        assertEquals("ACCEPTED", tracker.offer("A", "F1", "", menu));
        var latest = displays.beginRequest("A", menu);
        assertFalse(displays.complete(first, List.of()).accepted());
        tracker.finish("A", "F1", "", false);
        assertFalse(tracker.start("A", "F2", "", menu));
        assertTrue(tracker.start("A", "F1", "", menu));
        assertTrue(displays.complete(latest, List.of()).appliesToActiveMenu());
    }

    @Test
    void failedRequestCanRetryAndOnlySuccessSuppressesAnotherUpload() throws Exception {
        var tracker = new Tracker(4);
        Object menu = new Object();
        assertEquals("ACCEPTED", tracker.offer("A", "nbt", "", menu));
        assertTrue(tracker.start("A", "nbt", "", menu));
        tracker.finish("A", "nbt", "", false);
        assertEquals("ACCEPTED", tracker.offer("A", "nbt", "", menu));
        assertTrue(tracker.start("A", "nbt", "", menu));
        tracker.finish("A", "nbt", "", true);
        assertEquals("COALESCED_SUCCESS", tracker.offer("A", "nbt", "", new Object()));
    }

    @Test
    void successfulDedupIncludesStoragePosition() throws Exception {
        var tracker = new Tracker(4);
        Object menu = new Object();
        assertEquals("ACCEPTED", tracker.offer("Island Chest", "same-nbt", "1,2,3", menu));
        assertTrue(tracker.start("Island Chest", "same-nbt", "1,2,3", menu));
        tracker.finish("Island Chest", "same-nbt", "1,2,3", true);
        assertEquals("ACCEPTED", tracker.offer(
                "Island Chest", "same-nbt", "40,2,3", new Object()));
    }

    @Test
    void successfulDedupCanBeInvalidatedAfterDisplayEviction() throws Exception {
        var tracker = new Tracker(4);
        Object first = new Object();
        tracker.offer("A", "nbt", "", first);
        tracker.start("A", "nbt", "", first);
        tracker.finish("A", "nbt", "", true);
        assertEquals("COALESCED_SUCCESS", tracker.offer("A", "nbt", "", new Object()));
        tracker.forgetSuccessful("A", "nbt", "");
        assertEquals("ACCEPTED", tracker.offer("A", "nbt", "", new Object()));
    }

    @Test
    void newerReopenReplacesQueuedIdentityEvenAfterOlderSuccess() throws Exception {
        var tracker = new Tracker(4);
        Object first = new Object();
        Object second = new Object();
        Object third = new Object();
        tracker.offer("A", "nbt", "", first);
        tracker.start("A", "nbt", "", first);
        assertEquals("ACCEPTED", tracker.offer("A", "nbt", "", second));
        tracker.finish("A", "nbt", "", true);
        assertEquals("ACCEPTED", tracker.offer("A", "nbt", "", third));
        assertFalse(tracker.start("A", "nbt", "", second));
        assertTrue(tracker.start("A", "nbt", "", third));
    }

    @Test
    void titleStateIsBoundedAndReportsWorkThatMustBeCancelled() throws Exception {
        var tracker = new Tracker(2);
        tracker.offer("A", "1", "", new Object());
        tracker.offer("B", "2", "", new Object());
        Object offer = tracker.rawOffer("C", "3", "", new Object());
        assertEquals(2, tracker.size());
        assertEquals(List.of("A"), offer.getClass().getMethod("evictedTitles").invoke(offer));
    }

    private static final class Tracker {
        private final Object instance;
        private final Class<?> type;
        private final Class<?> fingerprintType;
        private final Method offer;

        Tracker(int capacity) throws Exception {
            type = Class.forName("com.coflnet.core.DescriptionRequestTracker");
            fingerprintType = Class.forName("com.coflnet.core.DescriptionRequestTracker$Fingerprint");
            instance = type.getConstructor(int.class).newInstance(capacity);
            offer = type.getMethod("offer", String.class, fingerprintType, Object.class);
        }

        String offer(String title, String nbt, String position, Object menu) throws Exception {
            Object offered = rawOffer(title, nbt, position, menu);
            Object result = offered.getClass().getMethod("result").invoke(offered);
            return result.toString();
        }

        Object rawOffer(String title, String nbt, String position, Object menu) throws Exception {
            return offer.invoke(instance, title, fingerprint(nbt, position), menu);
        }

        boolean start(String title, String nbt, String position, Object menu) throws Exception {
            return (boolean) type.getMethod("start", String.class, fingerprintType, Object.class)
                    .invoke(instance, title, fingerprint(nbt, position), menu);
        }

        void finish(String title, String nbt, String position, boolean succeeded) throws Exception {
            type.getMethod("finish", String.class, fingerprintType, boolean.class)
                    .invoke(instance, title, fingerprint(nbt, position), succeeded);
        }

        void forgetSuccessful(String title, String nbt, String position) throws Exception {
            type.getMethod("forgetSuccessful", String.class, fingerprintType)
                    .invoke(instance, title, fingerprint(nbt, position));
        }

        int size() throws Exception {
            return (int) type.getMethod("size").invoke(instance);
        }

        private Object fingerprint(String nbt, String position) throws Exception {
            return fingerprintType.getConstructor(String.class, String.class).newInstance(nbt, position);
        }
    }
}
