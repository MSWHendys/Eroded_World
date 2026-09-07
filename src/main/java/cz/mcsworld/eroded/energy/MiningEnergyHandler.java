package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Energy cost of mining, intentionally independent of Territory/Cave Collapse.
 * Disabling cave collapses must never silently disable the Energy system.
 */
public final class MiningEnergyHandler {

    // Fixed-point precision for fractional Energy costs. One million work
    // units equal exactly one visible Energy point.
    private static final int WORK_UNITS_PER_ENERGY = 1_000_000;

    private MiningEnergyHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (player.isCreative() || player.isSpectator()) return;

            // Ignore instant/zero-hardness utility blocks (torches, flowers,
            // crops, etc.). Mining Energy is intended for actual block work.
            if (state.getDestroySpeed(world, pos) <= 0.0f) return;

            handleMiningEnergy(serverPlayer, state);
        });
    }

    private static void handleMiningEnergy(ServerPlayer player, BlockState state) {
        var energyCfg = EnergyConfig.get().server;
        if (!energyCfg.enabled || !energyCfg.mining.enabled) return;

        SkillData energyData = SkillManager.get(player);

        // A completed block is mining activity too. Reset the passive-regen
        // clock before getEnergy()/consumeEnergy() can materialize a regen
        // interval. Continuous mining is additionally covered every server
        // tick by PlayerBlockBreakMixin.
        if (energyCfg.mining.pausePassiveRegenWhileMining) {
            energyData.pauseRegenerationClock();
        }

        if (energyData.getEnergy() <= 0) {
            // With speed scaling enabled, zero-Energy mining behavior is
            // handled directly by getDestroySpeed() on both client and server
            // (critical-speed continuation or a configured hard stop). Avoid
            // temporary Mining Fatigue, which used to cause a visible hitch
            // and would compound the configured percentage.
            if (!energyCfg.mining.speedScalingEnabled && energyCfg.core.blockWorkAtZero) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.MINING_FATIGUE,
                        80,
                        4,
                        true,
                        false
                ));
            }
            return;
        }

        // Adrenaline immunity means Energy work is free; do not bank hidden
        // mining progress that would be charged immediately after immunity.
        if (energyData.isImmune()) return;

        int workUnits = calculateWorkUnits(player, state, energyCfg.mining);
        // One common fixed-point denominator lets hard/soft profiles mix
        // without carrying a remainder calculated against another profile.
        int energyCost = energyData.addMiningWork(workUnits, WORK_UNITS_PER_ENERGY);

        if (energyCost > 0) {
            energyData.consumeEnergy(energyCost);
        }

        if (energyCfg.fatigueWhenExhausted && !energyCfg.mining.speedScalingEnabled) {
            SkillData.EnergyState currentState = energyData.getEnergyState();
            if (SkillData.severity(currentState)
                    >= SkillData.severity(SkillData.EnergyState.EXHAUSTED)) {
                player.addEffect(new MobEffectInstance(
                        MobEffects.MINING_FATIGUE,
                        100,
                        2,
                        true,
                        false
                ));
            }
        }
    }

    private static int calculateWorkUnits(
            ServerPlayer player,
            BlockState state,
            EnergyConfig.Server.Mining cfg
    ) {
        MiningBlockRules.Category category = MiningBlockRules.category(state);
        int blocksPerEnergy;
        int slowMultiplier;
        int wrongMultiplier;

        if (category == MiningBlockRules.Category.HARD) {
            blocksPerEnergy = cfg.hardBlocks.blocksPerEnergy;
            slowMultiplier = cfg.hardBlocks.slowToolMultiplier;
            wrongMultiplier = cfg.hardBlocks.wrongToolMultiplier;
        } else {
            blocksPerEnergy = cfg.softBlocks.blocksPerEnergy;
            slowMultiplier = cfg.softBlocks.slowToolMultiplier;
            wrongMultiplier = cfg.softBlocks.wrongToolMultiplier;
        }

        // Fractional Energy is accumulated deterministically even though the
        // HUD/persistent Energy value itself remains an integer.
        int baseUnits = Math.max(
                1,
                (int) Math.ceil((double) WORK_UNITS_PER_ENERGY / blocksPerEnergy)
        );

        ItemStack stack = player.getMainHandItem();
        MiningBlockRules.ToolUse toolUse = MiningBlockRules.toolUse(stack, state);

        int multiplier = switch (toolUse) {
            case FAST, NATURAL_HAND_WORK -> 1;
            case SLOW -> slowMultiplier;
            case WRONG -> wrongMultiplier;
        };

        long weighted = (long) baseUnits * multiplier;
        return (int) Math.min(Integer.MAX_VALUE, weighted);
    }
}

