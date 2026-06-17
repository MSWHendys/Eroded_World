package cz.mcsworld.eroded.item;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.VerticallyAttachableBlockItem;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class ErodedTorchItem extends VerticallyAttachableBlockItem {

    public ErodedTorchItem(
            Block standingBlock,
            Block wallBlock,
            Settings settings
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
                MathHelper.ceil(
                        DarknessConfigs.get()
                                .server
                                .erodedTorch
                                .maxChargeTicks / 20.0F
                )
        );
    }

    public static boolean hasCharge(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageable()
                && stack.getDamage() < stack.getMaxDamage();
    }

    public static boolean isFullyCharged(ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageable()
                && stack.getDamage() <= 0;
    }

    public static void drain(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            return;
        }

        stack.setDamage(
                MathHelper.clamp(
                        stack.getDamage() + 1,
                        0,
                        stack.getMaxDamage()
                )
        );
    }

    public static void recharge(ItemStack stack, int amount) {
        if (stack.isEmpty() || !stack.isDamageable() || amount <= 0) {
            return;
        }

        stack.setDamage(
                Math.max(
                        0,
                        stack.getDamage() - amount
                )
        );
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return DarknessConfigs.get().client.showTorchChargeHud
                && stack.isDamageable()
                && stack.getDamage() > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (!stack.isDamageable()) {
            return 13;
        }

        float ratio = 1.0F - (
                (float) stack.getDamage()
                        / (float) Math.max(1, stack.getMaxDamage())
        );

        return Math.round(13.0F * MathHelper.clamp(ratio, 0.0F, 1.0F));
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (!stack.isDamageable()) {
            return MathHelper.hsvToRgb(1.0F / 3.0F, 1.0F, 1.0F);
        }

        float ratio = 1.0F - (
                (float) stack.getDamage()
                        / (float) Math.max(1, stack.getMaxDamage())
        );

        return MathHelper.hsvToRgb(
                MathHelper.clamp(ratio, 0.0F, 1.0F) / 3.0F,
                1.0F,
                1.0F
        );
    }
}