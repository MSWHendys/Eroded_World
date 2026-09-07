package cz.mcsworld.eroded;

import cz.mcsworld.eroded.client.ClientSystems;
import cz.mcsworld.eroded.client.ErodedCompassClientTicker;
import cz.mcsworld.eroded.client.ErodedKeybinds;
import cz.mcsworld.eroded.client.TerritoryPlacementHintClient;
import cz.mcsworld.eroded.client.audio.CalmDownEffect;
import cz.mcsworld.eroded.client.data.*;
import cz.mcsworld.eroded.client.debug.TerritoryDebugOverlay;
import cz.mcsworld.eroded.client.gui.EnergyScreenOverlay;
import cz.mcsworld.eroded.client.hud.DarknessHudOverlay;
import cz.mcsworld.eroded.client.hud.EnergyHud;
import cz.mcsworld.eroded.client.hud.EnergyHudLogic;
import cz.mcsworld.eroded.client.input.DodgeInputHandler;
import cz.mcsworld.eroded.client.screen.TerritoryModuleScreen;
import cz.mcsworld.eroded.client.ui.EnergyWarningClientHandler;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.config.ErodedConfigs;
import cz.mcsworld.eroded.core.ErodedScreenHandlers;
import cz.mcsworld.eroded.gui.ErodedCompassTooltip;
import cz.mcsworld.eroded.gui.ErodedTooltip;
import cz.mcsworld.eroded.network.*;
import cz.mcsworld.eroded.visuals.darkness.DarknessDebugOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import cz.mcsworld.eroded.gui.ErodedSpecialItemTooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import cz.mcsworld.eroded.core.ErodedEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;


public class ErodedModClient implements ClientModInitializer {
    private static final long SOUND_CONFIG_SAVE_DEBOUNCE_MS = 2000L;
    private static boolean soundConfigSavePending = false;
    private static long soundConfigSaveDueAtMs = 0L;

    private static void flushPendingSoundConfigSave() {
        if (!soundConfigSavePending) return;

        ErodedConfigs.saveDarkness();
        soundConfigSavePending = false;
        soundConfigSaveDueAtMs = 0L;
    }

    @Override
    public void onInitializeClient() {

        EnergyHud.register();
        DarknessDebugOverlay.register();
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
        DarknessHudOverlay.register();

        DodgeInputHandler.register();

        ErodedSpecialItemTooltip.register();
        TerritoryPlacementHintClient.register();

        ClientPlayNetworking.registerGlobalReceiver(
                CraftingFailPacket.ID,
                (payload, context) -> context.client().execute(EnergyScreenOverlay::onCraftingFail)
        );

        ClientPlayNetworking.registerGlobalReceiver(
                EnergySyncPacket.ID,
                (payload, context) -> context.client().execute(() -> {
                    boolean hadEnergyState = ClientEnergyData.isInitialized();
                    int previousEnergy = ClientEnergyData.getEnergy();

                    ClientEnergyData.update(
                            payload.enabled(),
                            payload.energy(),
                            payload.maxEnergy(),
                            payload.immunitySeconds(),
                            payload.miningEnabled(),
                            payload.miningSpeedScalingEnabled(),
                            payload.miningAllowAtZero(),
                            payload.miningFullSpeedFromPercent(),
                            payload.miningReducedSpeedFromPercent(),
                            payload.miningReducedSpeedPercent(),
                            payload.miningCriticalSpeedPercent()
                    );

                    if (!payload.enabled()) {
                        EnergyHud.resetWarning();
                        EnergyScreenOverlay.resetEnergyState();
                    } else if (hadEnergyState && payload.energy() > previousEnergy) {
                        // Energy warnings describe a worsening transition.
                        // Once Energy rises again, an old warning is no longer valid.
                        EnergyHud.resetWarning();
                        EnergyScreenOverlay.resetWarning();
                    }
                })
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
                HudPositionSyncPacket.ID,
                (payload, context) -> context.client().execute(() -> {
                    var hud = cz.mcsworld.eroded.config.energy.EnergyConfig.get().client.hud;

                    if (hud.hudPosition != payload.position()) {
                        hud.hudPosition = payload.position();
                        ErodedConfigs.saveEnergy();
                    }

                    // Always preview the requested position, even when Energy is full
                    // or the player selected the position that was already active.
                    EnergyHud.showPositionPreview();
                })
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
                        // True debounce: always persist the latest values after
                        // the burst quiets down. The old cooldown could leave a
                        // change only in RAM forever when it arrived inside the
                        // 2-second window and no later packet followed.
                        soundConfigSavePending = true;
                        soundConfigSaveDueAtMs = System.currentTimeMillis()
                                + SOUND_CONFIG_SAVE_DEBOUNCE_MS;
                    }
                })
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.level != null) {
                cz.mcsworld.eroded.client.data.DarknessClientData.updateLightLevel(client);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (soundConfigSavePending
                    && System.currentTimeMillis() >= soundConfigSaveDueAtMs) {
                flushPendingSoundConfigSave();
            }
        });

        // Do not lose a command-driven sound preference if the client closes
        // during the debounce window.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                flushPendingSoundConfigSave()
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TerritoryModuleSyncPayload.ID,
                (payload, context) -> context.client().execute(() -> ClientTerritoryModuleData.update(payload))
        );

    }

}
