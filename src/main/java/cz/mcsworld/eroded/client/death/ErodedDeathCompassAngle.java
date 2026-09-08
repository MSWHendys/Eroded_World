package cz.mcsworld.eroded.client.death;

import com.mojang.serialization.MapCodec;
import cz.mcsworld.eroded.client.data.ErodedCompassClientData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public final class ErodedDeathCompassAngle implements RangeSelectItemModelProperty {

    public static final MapCodec<ErodedDeathCompassAngle> MAP_CODEC =
            MapCodec.unit(new ErodedDeathCompassAngle());

    @Override
    public float get(@NonNull ItemStack stack, @Nullable ClientLevel world, @Nullable ItemOwner context, int seed) {
        if (world == null || context == null) {
            return 0.0F;
        }

        if (!ErodedCompassClientData.isActive()) {
            return 0.0F;
        }

        BlockPos target = ErodedCompassClientData.getTarget();
        if (target == null) {
            return 0.0F;
        }

        Vec3 entityPos = context.position();

        double dx = (target.getX() + 0.5) - entityPos.x;
        double dz = (target.getZ() + 0.5) - entityPos.z;

        double angleToTarget = Math.atan2(dz, dx) / (Math.PI * 2.0);
        double playerYaw = Mth.positiveModulo(context.getVisualRotationYInDegrees() / 360.0, 1.0);

        return (float) Mth.positiveModulo(
                0.5 - (playerYaw - 0.25 - angleToTarget) + 0.5F,
                1.0
        );
    }

    @Override
    public @NonNull MapCodec<? extends RangeSelectItemModelProperty> type() {
        return MAP_CODEC;
    }
}