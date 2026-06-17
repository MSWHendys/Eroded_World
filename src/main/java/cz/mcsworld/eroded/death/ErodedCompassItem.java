package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.network.CompassDarknessBreakPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ErodedCompassItem extends Item {

    private static final Map<UUID, Long> DARKNESS_BREAK_COOLDOWNS = new ConcurrentHashMap<>();

    public ErodedCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayerEntity sp)) {
            return ActionResult.PASS;
        }

        ErodedDeathMemory mem = ErodedDeathStorage.get(sp.getUuid());

        boolean sneaking = player.isSneaking() || player.isInSneakingPose();

        if (sneaking) {
            return tryBreakDarkness(sp, mem);
        }

        if (mem == null || mem.isExpired(sp.getServer().getTicks())) {
            sp.sendMessage(
                    Text.translatable("eroded.compass.empty"),
                    true
            );
            return ActionResult.CONSUME;
        }

        long ticks = mem.getRemainingTicks(sp.getServer().getTicks());
        long seconds = ticks / 20;
        long min = seconds / 60;
        long sec = seconds % 60;

        String timeString = String.format("%02d:%02d", min, sec);

        sp.sendMessage(
                Text.translatable(
                        "eroded.compass.whisper",
                        timeString
                ),
                false
        );

        BlockPos pos = mem.getDeathPos();

        sp.sendMessage(
                Text.translatable(
                        "eroded.compass.whisper.coords",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                ).formatted(Formatting.DARK_GRAY),
                false
        );

        return ActionResult.CONSUME;
    }

    private ActionResult tryBreakDarkness(
            ServerPlayerEntity player,
            ErodedDeathMemory memory
    ) {
        DeathConfig.Compass.DarknessBreak cfg =
                DeathConfig.get().compass.darknessBreak;

        if (!cfg.enabled) {
            player.sendMessage(
                    Text.translatable("eroded.compass.darkness_break.disabled")
                            .formatted(Formatting.GRAY),
                    true
            );
            return ActionResult.CONSUME;
        }

        if (cfg.requireValidTarget) {
            if (memory == null || memory.isExpired(player.getServer().getTicks())) {
                player.sendMessage(
                        Text.translatable("eroded.compass.empty"),
                        true
                );
                return ActionResult.CONSUME;
            }
        }

        long now = player.getServer().getTicks();
        long readyAt = DARKNESS_BREAK_COOLDOWNS.getOrDefault(player.getUuid(), 0L);

        if (now < readyAt) {
            long remainingSeconds = Math.max(1L, (readyAt - now + 19L) / 20L);

            player.sendMessage(
                    Text.translatable(
                            "eroded.compass.darkness_break.cooldown",
                            remainingSeconds
                    ).formatted(Formatting.GRAY),
                    true
            );

            return ActionResult.CONSUME;
        }

        int durationTicks = Math.max(1, cfg.durationTicks);
        int cooldownTicks = Math.max(0, cfg.cooldownTicks);
        float maxDarkness = Math.max(0.0F, Math.min(1.0F, cfg.maxDarkness));

        DARKNESS_BREAK_COOLDOWNS.put(
                player.getUuid(),
                now + cooldownTicks
        );

        ServerPlayNetworking.send(
                player,
                new CompassDarknessBreakPacket(
                        durationTicks,
                        maxDarkness
                )
        );

        player.sendMessage(
                Text.translatable("eroded.compass.darkness_break.use")
                        .formatted(Formatting.AQUA),
                true
        );

        return ActionResult.CONSUME;
    }
}