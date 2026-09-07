package com.coflnet.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Reflection keeps the host's base comparison compilable before this production type exists. */
class DescriptionDisplayStateTest {
    @Test
    void cacheHitIsImmediateAndMissNeverUsesAnotherTitle() throws Exception {
        var state = new State(4);
        Object firstA = new Object();
        state.activate("A", firstA);
        state.complete(state.begin("A", firstA), state.lines("A"));
        assertTrue(state.selectedLines(state.activate("B", new Object())).isEmpty());
        assertEquals(state.lines("A"), state.selectedLines(state.activate("A", new Object())));
    }

    @Test
    void reopenedPreviewIsNotActionableUntilItsExactContentsAreConfirmed() throws Exception {
        var state = new State(4);
        Object first = new Object();
        state.activate("A", first);
        state.complete(state.begin("A", first), state.lines("A"));
        Object reopened = new Object();
        state.activate("A", reopened);
        assertFalse(state.verified(reopened));
        state.confirm("A", reopened);
        assertTrue(state.verified(reopened));
    }

    @Test
    void packetReadinessUsesFullContentOrAllChestSlotsInAnyOrder() throws Exception {
        var state = new State(4);
        Object menu = new Object();
        state.activate("A", menu);
        state.watch(menu, 7, 3);
        assertFalse(state.slot(menu, 8, 0), "obsolete container id");
        assertFalse(state.slot(menu, 7, 2));
        assertFalse(state.slot(menu, 7, 0));
        assertFalse(state.slot(menu, 7, 2), "duplicate slot");
        assertFalse(state.slot(menu, 7, 3), "player inventory slot");
        assertTrue(state.slot(menu, 7, 1), "all chest slots observed");

        Object replacement = new Object();
        state.activate("B", replacement);
        state.watch(replacement, 9, 6);
        assertFalse(state.full(menu, 7), "replaced menu");
        assertTrue(state.full(replacement, 9));
    }

    @Test
    void fullContentReadinessAlsoSupportsNonChestMenus() throws Exception {
        var state = new State(4);
        Object menu = new Object();
        state.activate("Anvil", menu);
        state.watch(menu, 11, 0);
        assertFalse(state.slot(menu, 11, 0));
        assertTrue(state.full(menu, 11));
        assertTrue(state.resultSlot(menu, 11, 2, "Combine Items"));
        assertTrue(state.resultSlot(menu, 11, 2, "§aFlip Order"));
        assertTrue(state.resultSlot(menu, 11, 2, "AUCTION FOR Golden Dragon"));
        assertFalse(state.resultSlot(menu, 12, 2, "Combine Items"), "obsolete container id");
        assertFalse(state.resultSlot(menu, 11, 2, "Player Inventory Item"));

        Object chest = new Object();
        state.activate("Chest", chest);
        state.watch(chest, 12, 3);
        assertFalse(state.resultSlot(chest, 12, 3, "Combine Items"), "player inventory slot");
    }

    @Test
    void sameMenuReinitPreservesReadinessActivationAndVerification() throws Exception {
        var state = new State(4);
        Object menu = new Object();
        state.activate("A", menu);
        state.watch(menu, 7, 3);
        assertTrue(state.full(menu, 7));
        Object request = state.begin("A", menu);

        state.activate("A", menu);
        state.watch(menu, 7, 3);
        assertTrue(state.applies(state.complete(request, state.lines("A"))));
        assertTrue(state.verified(menu));
        assertTrue(state.slot(menu, 7, 0), "re-init must not reset ready state");
    }

    @Test
    void unchangedRefreshPreservesSnapshotWhileChangedAndEmptyResponsesReplaceIt() throws Exception {
        var state = new State(4);
        Object menu = new Object();
        state.activate("A", menu);
        assertTrue(state.changed(state.complete(state.begin("A", menu), state.lines("A"))));
        assertFalse(state.changed(state.complete(state.begin("A", menu), state.lines("A"))));
        assertTrue(state.changed(state.complete(state.begin("A", menu), state.lines("B"))));
        assertTrue(state.changed(state.complete(state.begin("A", menu), List.of())));
        assertTrue(state.selectedLines(state.activate("A", new Object())).isEmpty());
    }

    @Test
    void completionOrderCannotCrossTitlesOrOverwriteNewerSameTitle() throws Exception {
        var state = new State(4);
        Object firstA = new Object();
        state.activate("A", firstA);
        Object oldA = state.begin("A", firstA);
        Object menuB = new Object();
        state.activate("B", menuB);
        Object requestB = state.begin("B", menuB);
        Object reopenedA = new Object();
        state.activate("A", reopenedA);
        Object newA = state.begin("A", reopenedA);
        assertFalse(state.applies(state.complete(requestB, state.lines("B"))));
        assertFalse(state.accepted(state.complete(oldA, state.lines("old A"))));
        assertTrue(state.applies(state.complete(newA, state.lines("A"))));
        assertEquals(state.lines("A"), state.selectedLines(state.activate("A", new Object())));
    }

    @Test
    void cacheIsBoundedAndSessionClearRejectsOutstandingResponses() throws Exception {
        var state = new State(2);
        Object outstanding = null;
        for (String title : List.of("A", "B", "C")) {
            Object menu = new Object();
            state.activate(title, menu);
            outstanding = state.begin(title, menu);
            state.complete(outstanding, state.lines(title));
        }
        assertEquals(2, state.size());
        assertTrue(state.selectedLines(state.activate("A", new Object())).isEmpty());
        state.clearSession();
        assertEquals(0, state.size());
        assertFalse(state.accepted(state.complete(outstanding, state.lines("late"))));
    }

    @Test
    void evictedDisplayCannotBeConfirmedAsCached() throws Exception {
        var state = new State(1);
        Object first = new Object();
        state.activate("A", first);
        state.complete(state.begin("A", first), state.lines("A"));
        Object second = new Object();
        state.activate("B", second);
        state.complete(state.begin("B", second), state.lines("B"));
        Object reopened = new Object();
        state.activate("A", reopened);
        assertFalse(state.confirm("A", reopened));
    }

    private static final class State {
        private final Object instance;
        private final Class<?> type;
        private final Class<?> requestType;
        private final Class<?> lineType;

        State(int capacity) throws Exception {
            type = Class.forName("com.coflnet.core.DescriptionDisplayState");
            requestType = Class.forName("com.coflnet.core.DescriptionDisplayState$Request");
            lineType = Class.forName("com.coflnet.core.DescriptionDisplayState$Line");
            instance = type.getConstructor(int.class).newInstance(capacity);
        }

        Object activate(String title, Object menu) throws Exception {
            return invoke(type.getMethod("activate", String.class, Object.class), title, menu);
        }

        Object begin(String title, Object menu) throws Exception {
            return invoke(type.getMethod("beginRequest", String.class, Object.class), title, menu);
        }

        Object complete(Object request, List<?> lines) throws Exception {
            return invoke(type.getMethod("complete", requestType, List.class), request, lines);
        }

        List<?> lines(String value) throws Exception {
            return List.of(lineType.getConstructor(String.class, String.class, int.class)
                    .newInstance("APPEND", value, 0));
        }

        List<?> selectedLines(Object selection) throws Exception {
            return (List<?>) selection.getClass().getMethod("lines").invoke(selection);
        }

        boolean accepted(Object completion) throws Exception {
            return booleanValue(completion, "accepted");
        }

        boolean applies(Object completion) throws Exception {
            return booleanValue(completion, "appliesToActiveMenu");
        }

        boolean changed(Object completion) throws Exception {
            return booleanValue(completion, "changed");
        }

        int size() throws Exception {
            return (int) invoke(type.getMethod("size"));
        }

        void clearSession() throws Exception {
            invoke(type.getMethod("clearSession"));
        }

        boolean confirm(String title, Object menu) throws Exception {
            return (boolean) invoke(type.getMethod("confirmCached", String.class, Object.class), title, menu);
        }

        boolean verified(Object menu) throws Exception {
            return (boolean) invoke(type.getMethod("isActiveAndVerified", Object.class), menu);
        }

        void watch(Object menu, int containerId, int slots) throws Exception {
            invoke(type.getMethod("watchContainer", Object.class, int.class, int.class),
                    menu, containerId, slots);
        }

        boolean slot(Object menu, int containerId, int slot) throws Exception {
            return (boolean) invoke(type.getMethod("slotApplied", Object.class, int.class, int.class),
                    menu, containerId, slot);
        }

        boolean full(Object menu, int containerId) throws Exception {
            return (boolean) invoke(type.getMethod("fullContentApplied", Object.class, int.class),
                    menu, containerId);
        }

        boolean resultSlot(Object menu, int containerId, int slot, String title) throws Exception {
            return (boolean) invoke(type.getMethod(
                    "resultSlotApplied", Object.class, int.class, int.class, String.class),
                    menu, containerId, slot, title);
        }

        private boolean booleanValue(Object target, String method) throws Exception {
            return (boolean) target.getClass().getMethod(method).invoke(target);
        }

        private Object invoke(Method method, Object... arguments) throws Exception {
            return method.invoke(instance, arguments);
        }
    }
}
