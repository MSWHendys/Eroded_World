package cz.mcsworld.eroded.client.death;

import com.mojang.serialization.MapCodec;
import cz.mcsworld.eroded.client.data.ErodedCompassClientData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.numeric.NumericProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class ErodedDeathCompassAngle implements NumericProperty {

    public static final MapCodec<ErodedDeathCompassAngle> MAP_CODEC = MapCodec.unit(new ErodedDeathCompassAngle());

    @Override
    public float getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed) {
        if (entity == null || world == null) return 0.0F;

        if (!ErodedCompassClientData.isActive()) {
            return 0.0F;
        }

        BlockPos target = ErodedCompassClientData.getTarget();
        if (target == null) return 0.0F;

        double dx = (target.getX() + 0.5) - entity.getX();
        double dz = (target.getZ() + 0.5) - entity.getZ();

        double angleToTarget = (Math.atan2(dz, dx) / (Math.PI * 2.0));
        double playerYaw = MathHelper.floorMod(entity.getYaw() / 360.0, 1.0);

        return (float) MathHelper.floorMod(0.5 - (playerYaw - 0.25 - angleToTarget) + 0.5f, 1.0);
    }

    @Override
    public MapCodec<? extends NumericProperty> getCodec() {
        return MAP_CODEC;
    }
}