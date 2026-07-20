package cz.mcsworld.eroded.client.death;

import com.mojang.serialization.MapCodec;
import cz.mcsworld.eroded.client.data.ErodedCompassClientData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ErodedDeathCompassAngle implements RangeSelectItemModelProperty {

    public static final MapCodec<ErodedDeathCompassAngle> MAP_CODEC = MapCodec.unit(new ErodedDeathCompassAngle());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
        if (entity == null || world == null) return 0.0F;

        if (!ErodedCompassClientData.isActive()) {
            return 0.0F;
        }

        BlockPos target = ErodedCompassClientData.getTarget();
        if (target == null) return 0.0F;

        double dx = (target.getX() + 0.5) - entity.getX();
        double dz = (target.getZ() + 0.5) - entity.getZ();

        double angleToTarget = (Math.atan2(dz, dx) / (Math.PI * 2.0));
        double playerYaw = Mth.positiveModulo(entity.getYRot() / 360.0, 1.0);

        return (float) Mth.positiveModulo(0.5 - (playerYaw - 0.25 - angleToTarget) + 0.5f, 1.0);
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return MAP_CODEC;
    }
}