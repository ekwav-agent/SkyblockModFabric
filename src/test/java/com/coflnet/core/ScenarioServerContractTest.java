package com.coflnet.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioServerContractTest {
    @Test
    void rootCheckoutExposesScenarioServerProjectAndIndex() throws IOException {
        Path settings = Path.of("settings.gradle");
        Path scenarioIndex = Path.of(
                "testserver/src/main/resources/data/coflnet_scenarios/scenarios/index.json");

        assertAll(
                () -> assertTrue(Files.readString(settings).contains("include(\"testserver\")"),
                        "root settings must include the scenario-server project"),
                () -> assertTrue(Files.isRegularFile(scenarioIndex),
                        "scenario-server resources must expose the scenario index"));
    }
}
