package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Server-authoritative movement penalty for a real Energy collapse.
 *
 * Uses a dedicated transient movement-speed modifier instead of Slowness so
 * recovery cannot accidentally remove potion/trauma effects from other
 * systems. The modifier is never persisted in player.dat and is reconciled
 * every server tick, so reloads, relogs and master-switch changes are safe.
 */
public final class EnergyMovementHandler {

    private static final ResourceLocation COLLAPSE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath("eroded", "energy_collapse_speed");

    private EnergyMovementHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(EnergyMovementHandler::tick);
    }

    private static void tick(MinecraftServer server) {
        var root = EnergyConfig.get();
        boolean enabled = root.server.enabled;
        double multiplier = root.server.collapse.movementSpeedMultiplier;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AttributeInstance movement = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement == null) continue;

            SkillData data = SkillManager.get(player);
            boolean exempt = player.getAbilities().instabuild
                    || player.getAbilities().flying
                    || player.isSpectator()
                    || player.isFallFlying();

            boolean shouldSlow = enabled && !exempt && data.isCollapsed();

            if (!shouldSlow || multiplier >= 1.0) {
                movement.removeModifier(COLLAPSE_SPEED_ID);
                continue;
            }

            // Collapse never permits sprinting. Do this server-side even if a
            // client keeps the sprint key held down.
            if (player.isSprinting()) {
                player.setSprinting(false);
            }

            double amount = multiplier - 1.0; // e.g. 0.5 -> -0.5 (50 % speed)
            AttributeModifier current = movement.getModifier(COLLAPSE_SPEED_ID);

            if (current != null
                    && current.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    && Double.compare(current.amount(), amount) == 0) {
                continue;
            }

            movement.removeModifier(COLLAPSE_SPEED_ID);
            movement.addTransientModifier(new AttributeModifier(
                    COLLAPSE_SPEED_ID,
                    amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }
}
