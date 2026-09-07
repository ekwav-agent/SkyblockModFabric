package com.coflnet.mixin;

import com.coflnet.CoflModClient;
import com.coflnet.gui.trade.TradePriceCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;

@Mixin(ClientPacketListener.class)
public class NewItemInChestMixin {

    @Inject(method = "handleContainerSetSlot", at = @At("HEAD"))
    private void onSlotUpdateHead(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        // Track UUID changes before the slot is updated
        try {
            if (Minecraft.getInstance().player == null || Minecraft.getInstance().player.containerMenu == null)
                return;
            
            int slot = packet.getSlot();
            if (slot < 0 || slot >= Minecraft.getInstance().player.containerMenu.slots.size())
                return;
                
            ItemStack previousStack = Minecraft.getInstance().player.containerMenu.getSlot(slot).getItem();
            ItemStack newStack = packet.getItem();
            
            if (previousStack.isEmpty() || newStack.isEmpty())
                return;
            
            Component prevName = previousStack.getCustomName();
            Component newName = newStack.getCustomName();
            
            // If item name is the same (including style/color) but UUIDs differ, map new UUID to original.
            // Using Component.equals() which compares contents, style, and siblings —
            // this prevents remapping between items that share the same plain text name
            // but differ in color (e.g. pets of different tiers like RARE vs MYTHIC).
            if (prevName != null && prevName.equals(newName)) {
                String prevUuid = CoflModClient.getUuidFromStack(previousStack);
                String newUuid = CoflModClient.getUuidFromStack(newStack);
                
                if (prevUuid != null && newUuid != null && !prevUuid.equals(newUuid)) {
                    // Find the original UUID (follow chain if exists)
                    String originalUuid = CoflModClient.uuidToOriginalUuid.getOrDefault(prevUuid, prevUuid);
                    CoflModClient.uuidToOriginalUuid.put(newUuid, originalUuid);
                }
            }
        } catch (Exception e) {
            // Silently ignore errors in UUID tracking
        }
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void onPacketReceive(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        try {
            CoflModClient.onContainerSlotApplied(packet.getContainerId(), packet.getSlot());
            String itemTitle = packet.getItem().getCustomName() == null
                    ? "" : packet.getItem().getCustomName().getString();
            CoflModClient.onDescriptionResultSlotApplied(
                    packet.getContainerId(), packet.getSlot(), itemTitle);
            int slot = packet.getSlot();
            // Offer slots are 0-35; slot 40 may be the final divider update
            // that makes the full trade layout verifiable.
            if ((slot >= 0 && slot < 36) || slot == 40) {
                TradePriceCache.requestCurrentTrade(packet.getContainerId());
                CoflModClient.openTradeOverlayIfReady(packet.getContainerId());
            }

            if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu != null) {
                if (slot < 0 || slot >= Minecraft.getInstance().player.containerMenu.slots.size())
                    return;
                    
                ItemStack previousStack = Minecraft.getInstance().player.containerMenu.getSlot(slot).getItem();
                if(previousStack.get(DataComponents.LORE) == null)
                    return;
                for (Component line : previousStack.get(DataComponents.LORE).lines()) {
                    if(line.getString().contains("Refreshing"))
                    {
                        // TODO: try batching this to refresh lore sooner than current waittime
                    }
                }
            }
        } catch (Exception e) {
            // If it fails, it might be a custom packet or a different type.
            // You can log the exception or handle it as needed.
            System.out.println("[NewItemInChestMixin] Failed to process packet: " + e.getMessage());
        }
    }

    /** Price the initial offer as soon as Minecraft has applied its complete contents. */
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void onContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        CoflModClient.onContainerContentApplied(packet.containerId());
        TradePriceCache.requestCurrentTrade(packet.containerId());
        CoflModClient.openTradeOverlayIfReady(packet.containerId());
    }
}
