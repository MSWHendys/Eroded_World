package cz.mcsworld.eroded.item;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

public final class ErodedTorchItem extends StandingAndWallBlockItem {

    public ErodedTorchItem(
            Block standingBlock,
            Block wallBlock,
            Properties settings
    ) {
        super(
                standingBlock,
                wallBlock,
                Direction.DOWN,
                settings
        );
    }

    public static int getTargetMaxDamage() {
        return Math.max(
                2,
                Mth.ceil(
                        DarknessConfigs.get()
                                .server
                                .erodedTorch
                                .maxChargeTicks / 20.0F
                )
        );
    }

    public static boolean hasCharge(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageableItem()
                && stack.getDamageValue() < stack.getMaxDamage();
    }

    public static boolean isFullyCharged(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageableItem()
                && stack.getDamageValue() <= 0;
    }

    public static void drain(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return;
        }

        stack.setDamageValue(
                Mth.clamp(
                        stack.getDamageValue() + 1,
                        0,
                        stack.getMaxDamage()
                )
        );
    }

    public static void recharge(ItemStack stack, int amount) {
        if (stack.isEmpty() || !stack.isDamageableItem() || amount <= 0) {
            return;
        }

        stack.setDamageValue(
                Math.max(
                        0,
                        stack.getDamageValue() - amount
                )
        );
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return DarknessConfigs.get().client.showTorchChargeHud
                && stack.isDamageableItem()
                && stack.getDamageValue() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 13;
        }

        float ratio = 1.0F - (
                (float) stack.getDamageValue()
                        / (float) Math.max(1, stack.getMaxDamage())
        );

        return Math.round(13.0F * Mth.clamp(ratio, 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return Mth.hsvToRgb(1.0F / 3.0F, 1.0F, 1.0F);
        }

        float ratio = 1.0F - (
                (float) stack.getDamageValue()
                        / (float) Math.max(1, stack.getMaxDamage())
        );

        return Mth.hsvToRgb(
                Mth.clamp(ratio, 0.0F, 1.0F) / 3.0F,
                1.0F,
                1.0F
        );
    }
}