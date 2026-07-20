package cz.mcsworld.eroded;

import cz.mcsworld.eroded.client.ClientSystems;
import cz.mcsworld.eroded.client.ErodedCompassClientTicker;
import cz.mcsworld.eroded.client.ErodedKeybinds;
import cz.mcsworld.eroded.client.TerritoryPlacementHintClient;
import cz.mcsworld.eroded.client.audio.CalmDownEffect;
import cz.mcsworld.eroded.client.data.*;
import cz.mcsworld.eroded.client.debug.TerritoryDebugOverlay;
import cz.mcsworld.eroded.client.gui.EnergyScreenOverlay;
import cz.mcsworld.eroded.client.hud.EnergyHud;
import cz.mcsworld.eroded.client.hud.EnergyHudLogic;
import cz.mcsworld.eroded.client.input.DodgeInputHandler;
import cz.mcsworld.eroded.client.screen.TerritoryModuleScreen;
import cz.mcsworld.eroded.client.ui.EnergyWarningClientHandler;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.core.ErodedScreenHandlers;
import cz.mcsworld.eroded.gui.ErodedCompassTooltip;
import cz.mcsworld.eroded.gui.ErodedTooltip;
import cz.mcsworld.eroded.network.*;
import cz.mcsworld.eroded.visuals.darkness.DarknessDebugOverlay;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import cz.mcsworld.eroded.gui.ErodedSpecialItemTooltip;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import cz.mcsworld.eroded.core.ErodedEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;


public class ErodedModClient implements ClientModInitializer {
    private static long lastConfigSave = 0;
    private static final long SAVE_COOLDOWN_MS = 2000;

    @Override
    public void onInitializeClient() {

        HudRenderCallback.EVENT.register(new EnergyHud());
        HudRenderCallback.EVENT.register(new DarknessDebugOverlay());
        EnergyScreenOverlay.register();
        MenuScreens.register(
                ErodedScreenHandlers.TERRITORY_MODULE,
                TerritoryModuleScreen::new
        );

        EntityRendererRegistry.register(
                ErodedEntities.ERODED_SPECIAL_SKELETON,
                SkeletonRenderer::new
        );
        EntityRendererRegistry.register(
                ErodedEntities.ERODED_SPECIAL_ZOMBIE,
                ZombieRenderer::new
        );

        ClientPlayNetworking.registerGlobalReceiver(
                CompassDarknessBreakPacket.ID,
                (payload, context) -> context.client().execute(() -> DarknessClientData.activateCompassDarknessBreak(
                        payload.durationTicks(),
                        payload.maxDarkness()
                ))
        );

        ErodedTooltip.register();
        ErodedCompassTooltip.register();
        EnergyWarningClientHandler.register();
        ErodedKeybinds.register();
        TerritoryDebugOverlay.register();

        DodgeInputHandler.register();

        ErodedSpecialItemTooltip.register();
        TerritoryPlacementHintClient.register();

        ClientPlayNetworking.registerGlobalReceiver(
                CraftingFailPacket.ID,
                (payload, context) -> context.client().execute(EnergyScreenOverlay::onCraftingFail)
        );

        ClientPlayNetworking.registerGlobalReceiver(
                EnergySyncPacket.ID,
                (payload, context) -> ClientEnergyData.update(
                        payload.energy(),
                        payload.maxEnergy(),
                        payload.immunitySeconds()
                )
        );


        ClientPlayNetworking.registerGlobalReceiver(
                ErodedCompassSyncPacket.ID,
                (payload, context) -> ErodedCompassClientData.updateTarget(
                        payload.active(),
                        payload.active()
                                ? BlockPos.of(payload.deathPosLong())
                                : null,
                        payload.remainingTicks()
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SkillSyncPacket.ID,
                (payload, context) -> context.client().execute(() -> ClientSkillData.update(
                        payload.woodworking(),
                        payload.smelting()
                ))
        );

        ClientPlayNetworking.registerGlobalReceiver(
                AnvilFeedbackPacket.ID,
                (payload, context) -> context.client().execute(() -> EnergyScreenOverlay.showAnvilMessage(
                        Component.translatable(
                                payload.key(),
                                Component.translatable("eroded.crafting.quality." + payload.quality().toLowerCase())
                        ),
                        payload.quality()
                ))
        );



        ClientPlayNetworking.registerGlobalReceiver(
                DarknessStatePacket.ID,
                (payload, context) -> DarknessClientData.update(payload.inDarkness())
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientSystems.clientTick());

        ClientTickEvents.END_CLIENT_TICK.register(client -> ErodedCompassClientTicker.tick());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (DarknessClientData.consumeDarknessExit()) {
                CalmDownEffect.trigger();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(
                CraftingRequirementPacket.ID,
                (payload, context) -> context.client().execute(() -> EnergyScreenOverlay.showCustomMessage(
                        Component.translatable(payload.key()),
                        EnergyHudLogic.RED
                ))
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SoundTuningSyncPacket.ID,
                (payload, context) -> context.client().execute(() -> {

                    var audio = DarknessConfigs.get().client.audio;

                    boolean changed = false;

                    if (payload.volumeMul() != null
                            && audio.volumeMultiplier != payload.volumeMul()) {
                        audio.volumeMultiplier = payload.volumeMul();
                        changed = true;
                    }

                    if (payload.delayMul() != null
                            && audio.delayMultiplier != payload.delayMul()) {
                        audio.delayMultiplier = payload.delayMul();
                        changed = true;
                    }

                    if (changed) {
                        long now = System.currentTimeMillis();

                        if (now - lastConfigSave > SAVE_COOLDOWN_MS) {
                            AutoConfig.getConfigHolder(DarknessConfigs.class).save();
                            lastConfigSave = now;
                        }
                    }
                })
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.level != null) {
                cz.mcsworld.eroded.client.data.DarknessClientData.updateLightLevel(client);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(
                TerritoryModuleSyncPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientTerritoryModuleData.update(payload))
        );

    }

}
