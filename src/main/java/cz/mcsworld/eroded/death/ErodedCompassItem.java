package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.network.CompassDarknessBreakPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ErodedCompassItem extends Item {

    private static final Map<UUID, Long> DARKNESS_BREAK_COOLDOWNS = new ConcurrentHashMap<>();

    public ErodedCompassItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        ErodedDeathMemory mem = ErodedDeathStorage.get(sp.getUUID());

        boolean sneaking = player.isShiftKeyDown() || player.isCrouching();

        if (sneaking) {
            return tryBreakDarkness(sp, mem);
        }

        if (mem == null || mem.isExpired(sp.getServer().getTickCount())) {
            sp.displayClientMessage(
                    Component.translatable("eroded.compass.empty"),
                    true
            );
            return InteractionResult.CONSUME;
        }

        long ticks = mem.getRemainingTicks(sp.getServer().getTickCount());
        long seconds = ticks / 20;
        long min = seconds / 60;
        long sec = seconds % 60;

        String timeString = String.format("%02d:%02d", min, sec);

        sp.displayClientMessage(
                Component.translatable(
                        "eroded.compass.whisper",
                        timeString
                ),
                false
        );

        BlockPos pos = mem.getDeathPos();

        sp.displayClientMessage(
                Component.translatable(
                        "eroded.compass.whisper.coords",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                ).withStyle(ChatFormatting.DARK_GRAY),
                false
        );

        return InteractionResult.CONSUME;
    }

    private InteractionResult tryBreakDarkness(
            ServerPlayer player,
            ErodedDeathMemory memory
    ) {
        DeathConfig.Compass.DarknessBreak cfg =
                DeathConfig.get().compass.darknessBreak;

        if (!cfg.enabled) {
            player.displayClientMessage(
                    Component.translatable("eroded.compass.darkness_break.disabled")
                            .withStyle(ChatFormatting.GRAY),
                    true
            );
            return InteractionResult.CONSUME;
        }

        if (cfg.requireValidTarget) {
            if (memory == null || memory.isExpired(player.getServer().getTickCount())) {
                player.displayClientMessage(
                        Component.translatable("eroded.compass.empty"),
                        true
                );
                return InteractionResult.CONSUME;
            }
        }

        long now = player.getServer().getTickCount();
        long readyAt = DARKNESS_BREAK_COOLDOWNS.getOrDefault(player.getUUID(), 0L);

        if (now < readyAt) {
            long remainingSeconds = Math.max(1L, (readyAt - now + 19L) / 20L);

            player.displayClientMessage(
                    Component.translatable(
                            "eroded.compass.darkness_break.cooldown",
                            remainingSeconds
                    ).withStyle(ChatFormatting.GRAY),
                    true
            );

            return InteractionResult.CONSUME;
        }

        int durationTicks = Math.max(1, cfg.durationTicks);
        int cooldownTicks = Math.max(0, cfg.cooldownTicks);
        float maxDarkness = Math.max(0.0F, Math.min(1.0F, cfg.maxDarkness));

        DARKNESS_BREAK_COOLDOWNS.put(
                player.getUUID(),
                now + cooldownTicks
        );

        ServerPlayNetworking.send(
                player,
                new CompassDarknessBreakPacket(
                        durationTicks,
                        maxDarkness
                )
        );

        player.displayClientMessage(
                Component.translatable("eroded.compass.darkness_break.use")
                        .withStyle(ChatFormatting.AQUA),
                true
        );

        return InteractionResult.CONSUME;
    }
}