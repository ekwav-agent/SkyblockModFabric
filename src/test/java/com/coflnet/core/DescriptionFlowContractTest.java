package com.coflnet.core;

import CoflCore.handlers.DescriptionHandler;
import com.coflnet.CoflModClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DescriptionFlowContractTest {
    @AfterEach
    void resetEndpoint() {
        System.clearProperty(DescriptionEndpointOverride.PROPERTY);
        CoflCore.configuration.Config.BaseUrl = "https://sky.coflnet.com";
        DescriptionHandler.emptyTooltipData();
    }

    @Test
    void containerSideDisplaySelectsItsTitleBeforeRefresh() throws Exception {
        String mixin = Files.readString(Path.of(
                "src/client/java/com/coflnet/mixin/HandledScreenMixin.java"));
        assertEquals(true, mixin.contains("selectInfoDisplay(currentTitle, screen.getMenu())"));
        assertEquals(false, mixin.contains("sideTextWidget.setAlpha(0.3f)"));
    }

    @Test
    void containerDescriptionsUseAppliedPacketReadinessWithoutPopulationSleeps() throws Exception {
        String client = Files.readString(Path.of(
                "src/client/java/com/coflnet/CoflModClient.java"));
        String packetMixin = Files.readString(Path.of(
                "src/client/java/com/coflnet/mixin/NewItemInChestMixin.java"));
        assertEquals(false, client.contains("Waiting for item stacks to load"));
        assertEquals(true, packetMixin.contains("onContainerContentApplied(packet.containerId())"));
        assertEquals(true, packetMixin.contains(
                "onContainerSlotApplied(packet.getContainerId(), packet.getSlot())"));
        assertEquals(true, packetMixin.contains(
                "packet.getContainerId(), packet.getSlot(), itemTitle"));
    }

    @Test
    void selectingANewTitleDoesNotReuseThePreviousGlobalDisplay(@TempDir Path sessionDirectory)
            throws Exception {
        try (var stub = new DescriptionBackendStub()) {
            stub.start();
            CoflCore.misc.SessionManager.setMainPath(sessionDirectory);
            System.setProperty(DescriptionEndpointOverride.PROPERTY, stub.baseUrl());
            DescriptionEndpointOverride.applySystemProperty();
            String[] visibleItems = new String[DescriptionBackendStub.CLIENT_ITEM_COUNT];
            Arrays.setAll(visibleItems, slot -> "EMPTY_SLOT_" + slot);

            DescriptionHandler.loadDescriptionForInventory(visibleItems, DescriptionBackendStub.CHEST_NAME,
                    DescriptionBackendStub.orderInventoryNbt(), "scenario-user");
            stub.awaitRequest();
            assertEquals(5, DescriptionHandler.getInfoDisplay().length);

            assertEquals(0, selectForTitle("Different Container", new Object()).length);
        }
    }

    private static DescriptionHandler.DescModification[] selectForTitle(String title, Object menu)
            throws IllegalAccessException, InvocationTargetException {
        try {
            return (DescriptionHandler.DescModification[]) CoflModClient.class
                    .getMethod("selectInfoDisplay", String.class, Object.class)
                    .invoke(null, title, menu);
        } catch (NoSuchMethodException ignored) {
            // Pinned base initializes a container through this global display getter.
            return CoflModClient.getExtraSlotDescMod();
        }
    }

    @Test
    void backendReturnWithoutTitleCacheCallbackIsNotSuccessful(@TempDir Path sessionDirectory)
            throws Exception {
        try (var stub = new DescriptionBackendStub()) {
            stub.start();
            CoflCore.misc.SessionManager.setMainPath(sessionDirectory);
            System.setProperty(DescriptionEndpointOverride.PROPERTY, stub.baseUrl());
            DescriptionEndpointOverride.applySystemProperty();
            DescriptionHandler.setRefreshCallback((lines, title) -> { });
            String[] visibleItems = new String[DescriptionBackendStub.CLIENT_ITEM_COUNT];
            Arrays.setAll(visibleItems, slot -> "EMPTY_SLOT_" + slot);

            Class<?> requestType = Class.forName("com.coflnet.CoflModClient$DescriptionRequest");
            Class<?> displayRequestType =
                    Class.forName("com.coflnet.core.DescriptionDisplayState$Request");
            var constructor = requestType.getDeclaredConstructor(String.class, String[].class,
                    String.class, String.class, Class.forName("CoflCore.classes.Position"),
                    displayRequestType);
            constructor.setAccessible(true);
            Object request = constructor.newInstance(DescriptionBackendStub.CHEST_NAME, visibleItems,
                    DescriptionBackendStub.orderInventoryNbt(), "scenario-user", null, null);
            Method fetch = CoflModClient.class.getDeclaredMethod("fetchDescriptionsForItems", requestType);
            fetch.setAccessible(true);

            assertEquals(false, fetch.invoke(null, request));
            stub.awaitRequest();
        } finally {
            DescriptionHandler.setRefreshCallback(null);
        }
    }

    @Test
    void endpointOverrideIsExplicitAndLoopbackOnly() {
        CoflCore.configuration.Config.BaseUrl = "https://production-default.invalid";
        DescriptionEndpointOverride.applySystemProperty();
        assertEquals("https://production-default.invalid", CoflCore.configuration.Config.BaseUrl);

        System.setProperty(DescriptionEndpointOverride.PROPERTY, "https://example.com:443");
        assertThrows(IllegalArgumentException.class, DescriptionEndpointOverride::applySystemProperty);
        assertEquals("https://production-default.invalid", CoflCore.configuration.Config.BaseUrl);
    }

    @Test
    void realDescriptionRequestMapsOrderDescriptionAndTrailingInfoDisplay(@TempDir Path sessionDirectory)
            throws Exception {
        try (var stub = new DescriptionBackendStub()) {
            stub.start();
            CoflCore.misc.SessionManager.setMainPath(sessionDirectory);
            System.setProperty(DescriptionEndpointOverride.PROPERTY, stub.baseUrl());
            DescriptionEndpointOverride.applySystemProperty();
            String[] visibleItems = new String[DescriptionBackendStub.CLIENT_ITEM_COUNT];
            Arrays.setAll(visibleItems, slot -> "EMPTY_SLOT_" + slot);
            String orderId = "BUY AGATHA COUPON OrderPrice per unit: 1,250 coins";
            visibleItems[DescriptionBackendStub.ORDER_SLOT] = orderId;
            String fullInventoryNbt = DescriptionBackendStub.orderInventoryNbt();

            DescriptionHandler.loadDescriptionForInventory(visibleItems, DescriptionBackendStub.CHEST_NAME,
                    fullInventoryNbt, "scenario-user");

            var request = stub.awaitRequest();
            assertEquals(DescriptionBackendStub.CHEST_NAME, request.get("chestName").getAsString());
            assertEquals(3, request.get("version").getAsInt());
            assertEquals(fullInventoryNbt, request.get("fullInventoryNbt").getAsString());
            assertEquals(DescriptionBackendStub.ITEM_DESCRIPTION,
                    DescriptionHandler.getTooltipData(orderId)[0].value);
            assertEquals(5, DescriptionHandler.getInfoDisplay().length);
            assertEquals(DescriptionBackendStub.TOTAL_BUY, DescriptionHandler.getInfoDisplay()[0].value);
            assertEquals("", DescriptionHandler.getInfoDisplay()[1].value);
            assertEquals(DescriptionBackendStub.TOTAL_SELL, DescriptionHandler.getInfoDisplay()[2].value);
            assertEquals("", DescriptionHandler.getInfoDisplay()[3].value);
            assertEquals(DescriptionBackendStub.ITEM_DESCRIPTION, DescriptionHandler.getInfoDisplay()[4].value);
        }
    }
}
