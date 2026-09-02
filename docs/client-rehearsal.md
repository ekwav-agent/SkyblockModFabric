# Client rehearsal

Inside the DevServer sandbox, build the client mod and scenario server:

```sh
./gradlew build
```

Start the scenario server in manual mode:

```sh
coflnet-minecraft-test-server local \
  --jar testserver/build/libs/skycofl-scenario-server.jar \
  --mode manual \
  --port 25565
```

While the server is running, launch the client and capture screenshots after 20 and 40 seconds:

```sh
coflnet-minecraft-client run \
  --mod testserver/build/libs/skycofl-scenario-server.jar \
  --server-port 25565 \
  --screenshots /workspace/.coflnet-client-run \
  --mode debug \
  --capture-after 20,40
```

Inspect the PNG files and `client.log` under `/workspace/.coflnet-client-run`.

## Last rehearsal

The client runner completed with `status: "captured"`. The join marker in `client.log` was:

```text
[17:14:09] [Render thread/INFO]: Loaded 3 advancements
```

The manual server's `server.log` reached `[17:13:46] [Server thread/INFO]: Done (7.832s)! For
help, type "help"` before the client was launched.

It wrote two PNGs, each 1280 by 720 pixels. Inspection of both captures at full resolution showed
the live `ender-chest-sequence` scenario: the Ender Chest menu was open with its scenario item,
the player inventory and hotbar were visible, and the enclosed room floor and glass boundary were
visible behind the menu. Neither capture showed the death screen.

The debug-run `manifest.json` did not contain a `mod_sha256` field; its `artifact_sha256` was the
all-zero debug placeholder. The independently computed SHA-256 for the scenario mod was:

```text
skycofl-scenario-server.jar: e0c637a0f031cac1001880cb1ce133d0c8512270db694cac93f30517afcc70a8
```

## Last automated rehearsal

The automated runner printed this `result.json` summary:

```json
{
  "passed": true,
  "scenarios": [
    {
      "id": "ender-chest-sequence",
      "passed": true,
      "summary": "labels=room.rebuilt:true,menu.slot-8.skyblock-menu:true,menu.initial-contents:true,room.structure:true,fixture.transition:true,menu.slot-update-newer:true,menu.slot-update-older:true,menu.close:true,menu.reopen:true,menu.packet-order:true,network.clientbound-packets:true"
    },
    {
      "id": "bazaar-menu",
      "passed": true,
      "summary": "labels=room.rebuilt:true,menu.slot-8.skyblock-menu:true,menu.initial-contents:true,room.structure:true,fixture.transition:true,menu.slot-update-newer:true,menu.slot-update-older:true,menu.close:true,menu.reopen:true,menu.packet-order:true,network.clientbound-packets:true"
    },
    {
      "id": "auction-house-menu",
      "passed": true,
      "summary": "labels=room.rebuilt:true,menu.slot-8.skyblock-menu:true,menu.initial-contents:true,room.structure:true,fixture.transition:true,menu.slot-update-newer:true,menu.slot-update-older:true,menu.close:true,menu.reopen:true,menu.packet-order:true,network.clientbound-packets:true"
    },
    {
      "id": "trade-divider",
      "passed": true,
      "summary": "labels=room.rebuilt:true,menu.slot-8.skyblock-menu:true,menu.initial-contents:true,room.structure:true,fixture.transition:true,trade.dividers:true,menu.slot-update-newer:true,menu.slot-update-older:true,menu.close:true,menu.reopen:true,menu.packet-order:true,network.clientbound-packets:true"
    },
    {
      "id": "world-signals",
      "passed": true,
      "summary": "labels=room.rebuilt:true,scoreboard.objective-team-score:true,entity.named:true,hud.chat-actionbar-title-tab-sound:true,room.structure:true,fixture.transition:true,world.entities:true,scoreboard.sidebar:true,entity.metadata-update:true,chunk.unload:true,chunk.reload:true,world.packet-order:true,network.clientbound-packets:true"
    }
  ]
}
```

The final server log lines were:

```text
[17:13:19] [Server thread/INFO]: ThreadedAnvilChunkStorage (world): All chunks are saved
[17:13:19] [Server thread/INFO]: ThreadedAnvilChunkStorage (DIM-1): All chunks are saved
[17:13:19] [Server thread/INFO]: ThreadedAnvilChunkStorage (DIM1): All chunks are saved
[17:13:19] [Server thread/INFO]: ThreadedAnvilChunkStorage: All dimensions are saved
```
