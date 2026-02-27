package cz.mcsworld.eroded;

import cz.mcsworld.eroded.client.ClientSystems;
import cz.mcsworld.eroded.client.ErodedCompassClientTicker;
import cz.mcsworld.eroded.client.audio.CalmDownEffect;
import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.gui.EnergyScreenOverlay;
import cz.mcsworld.eroded.client.hud.EnergyHud;
import cz.mcsworld.eroded.client.input.DodgeInputHandler;
import cz.mcsworld.eroded.client.ui.EnergyWarningClientHandler;
import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.gui.ErodedCompassTooltip;
import cz.mcsworld.eroded.gui.ErodedTooltip;
import cz.mcsworld.eroded.network.*;
import cz.mcsworld.eroded.visuals.darkness.DarknessDebugOverlay;
import cz.mcsworld.eroded.client.data.DarknessClientData;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import cz.mcsworld.eroded.client.data.ErodedCompassClientData;


import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.math.BlockPos;

import static cz.mcsworld.eroded.death.block.ErodedBlocks.DEATH_ENDER_CHEST;


public class ErodedModClient implements ClientModInitializer {
    private static long lastConfigSave = 0;
    private static final long SAVE_COOLDOWN_MS = 2000;

    @Override
    public void onInitializeClient() {

        HudRenderCallback.EVENT.register(new EnergyHud());
        HudRenderCallback.EVENT.register(new DarknessDebugOverlay());
        EnergyScreenOverlay.register();

        ErodedTooltip.register();
        ErodedCompassTooltip.register();
        EnergyWarningClientHandler.register();

        DodgeInputHandler.register();

        ClientPlayNetworking.registerGlobalReceiver(
                CraftingFailPacket.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        EnergyScreenOverlay.onCraftingFail();
                    });
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                EnergySyncPacket.ID,
                (payload, context) -> {
                    ClientEnergyData.update(
                            payload.energy(),
                            ClientEnergyData.getMaxEnergy()
                    );
                }
        );


        ClientPlayNetworking.registerGlobalReceiver(
                ErodedCompassSyncPacket.ID,
                (payload, context) -> {
                    ErodedCompassClientData.updateTarget(
                            payload.active(),
                            payload.active()
                                    ? BlockPos.fromLong(payload.deathPosLong())
                                    : null,
                            payload.remainingTicks()
                    );
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                DarknessStatePacket.ID,
                (payload, context) -> {
                    DarknessClientData.update(payload.inDarkness());
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientSystems.clientTick();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ErodedCompassClientTicker.tick();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (DarknessClientData.consumeDarknessExit()) {
                CalmDownEffect.trigger();
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(
                SoundTuningSyncPacket.ID,
                (payload, context) -> {
                    context.client().execute(() -> {

                        var audio = DarknessConfigs.get().client.audio;

                        if (payload.volumeMul() != null)
                            audio.volumeMultiplier = payload.volumeMul();

                        if (payload.delayMul() != null)
                            audio.delayMultiplier = payload.delayMul();

                        boolean changed = false;

                        if (payload.volumeMul() != null &&
                                audio.volumeMultiplier != payload.volumeMul()) {
                            audio.volumeMultiplier = payload.volumeMul();
                            changed = true;
                        }

                        if (payload.delayMul() != null &&
                                audio.delayMultiplier != payload.delayMul()) {
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
                    });
                }
        );
    }

}
