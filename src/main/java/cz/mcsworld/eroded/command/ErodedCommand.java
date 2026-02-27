package cz.mcsworld.eroded.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import cz.mcsworld.eroded.config.ErodedConfigs;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.energy.EnergyHudPosition;
import cz.mcsworld.eroded.network.SoundTuningSyncPacket;
import cz.mcsworld.eroded.world.territory.*;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public final class ErodedCommand {
    private static final java.util.Map<java.util.UUID, Long> SOUND_COOLDOWN = new java.util.HashMap<>();
    private static final long SOUND_COOLDOWN_MS = 1000;

    private ErodedCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        registerInternal(dispatcher)
        );
    }

    private static void registerInternal(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
                CommandManager.literal("eroded")

                        .then(CommandManager.literal("reload")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    try {
                                        ErodedConfigs.reload();
                                        ctx.getSource().sendFeedback(
                                                () -> Text.translatable("eroded.command.reload.success"),
                                                false
                                        );
                                        return 1;
                                    } catch (Exception e) {
                                        ctx.getSource().sendError(
                                                Text.translatable("eroded.command.reload.error")
                                        );
                                        e.printStackTrace();
                                        return 0;
                                    }
                                })
                        )

                        .then(CommandManager.literal("sound")

                                .then(CommandManager.literal("volume")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 10)
                                                        )
                                                        .executes(ctx -> {

                                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                                            ServerPlayerEntity player = ctx.getSource().getPlayer();

                                                            long now = System.currentTimeMillis();
                                                            Long last = SOUND_COOLDOWN.get(player.getUuid());

                                                            if (last != null && now - last < SOUND_COOLDOWN_MS) {
                                                                ctx.getSource().sendError(Text.translatable("eroded.command.sound.cooldown"));
                                                                return 0;
                                                            }

                                                            SOUND_COOLDOWN.put(player.getUuid(), now);
                                                            float normalized = 0.2f + ((value - 1) / 9.0f) * 0.8f;

                                                            SoundTuningSyncPacket.sendTo(player, normalized, null);

                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.translatable(
                                                                            "eroded.command.sound.volume.set",
                                                                            value
                                                                    ),
                                                                    false
                                                            );
                                                            return 1;
                                                        })

                                        )
                                )

                                .then(CommandManager.literal("delay")
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> {

                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();

                                                    long now = System.currentTimeMillis();
                                                    Long last = SOUND_COOLDOWN.get(player.getUuid());

                                                    if (last != null && now - last < SOUND_COOLDOWN_MS) {
                                                        ctx.getSource().sendError(Text.translatable("eroded.command.sound.cooldown"));
                                                        return 0;
                                                    }

                                                    SOUND_COOLDOWN.put(player.getUuid(), now);
                                                    float normalized = 0.5f + ((value - 1) / 9.0f) * 1.5f;

                                                    SoundTuningSyncPacket.sendTo(player, null, normalized);

                                                    ctx.getSource().sendFeedback(
                                                            () -> Text.translatable(
                                                                    "eroded.command.sound.delay.set",
                                                                    value
                                                            ),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                                .then(CommandManager.literal("info")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();

                                            long now = System.currentTimeMillis();
                                            Long last = SOUND_COOLDOWN.get(player.getUuid());

                                            if (last != null && now - last < SOUND_COOLDOWN_MS) {
                                                ctx.getSource().sendError(Text.translatable("eroded.command.sound.cooldown"));
                                                return 0;
                                            }

                                            SOUND_COOLDOWN.put(player.getUuid(), now);
                                            float volume = cz.mcsworld.eroded.config.darkness.DarknessConfigs
                                                    .get().client.audio.volumeMultiplier;

                                            float delay = cz.mcsworld.eroded.config.darkness.DarknessConfigs
                                                    .get().client.audio.delayMultiplier;

                                            int volumeUser = Math.round((volume - 0.2f) / 0.8f * 9f) + 1;
                                            int delayUser = Math.round((delay - 0.5f) / 1.5f * 9f) + 1;

                                            ctx.getSource().sendFeedback(
                                                    () -> Text.translatable("eroded.command.sound.info.title")
                                                            .append(Text.translatable(
                                                                    "eroded.command.sound.info.volume",
                                                                    volumeUser
                                                            ))
                                                            .append(Text.translatable(
                                                                    "eroded.command.sound.info.delay",
                                                                    delayUser
                                                            )),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                                .then(CommandManager.literal("reset")
                                        .executes(ctx -> {

                                            ServerPlayerEntity player = ctx.getSource().getPlayer();

                                            long now = System.currentTimeMillis();
                                            Long last = SOUND_COOLDOWN.get(player.getUuid());

                                            if (last != null && now - last < SOUND_COOLDOWN_MS) {
                                                ctx.getSource().sendError(Text.translatable("eroded.command.sound.cooldown"));
                                                return 0;
                                            }

                                            SOUND_COOLDOWN.put(player.getUuid(), now);

                                            SoundTuningSyncPacket.sendTo(player, 1.0f, 1.0f);

                                            ctx.getSource().sendFeedback(
                                                    () -> Text.translatable("eroded.command.sound.reset"),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )

                        .then(literal("icon")
                                .then(argument("position", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (var pos : EnergyHudPosition.values()) {
                                                builder.suggest(pos.name().toLowerCase());
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {

                                            String input = StringArgumentType.getString(ctx, "position").toUpperCase();

                                            try {
                                                EnergyHudPosition newPos = EnergyHudPosition.valueOf(input);

                                                var cfg = EnergyConfig.get().client.hud;
                                                cfg.hudPosition = newPos;

                                                AutoConfig.getConfigHolder(EnergyConfig.class).save();

                                                ctx.getSource().sendFeedback(
                                                        () -> Text.translatable(
                                                                "eroded.energy.hud.position.changed",
                                                                newPos.getTranslation()
                                                        ),
                                                        false
                                                );

                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().sendError(
                                                        Text.translatable("eroded.energy.hud.position.invalid")
                                                );
                                            }

                                            return 1;
                                        })
                                )
                        )
        );
    }
}