package cz.mcsworld.eroded.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.serialization.MapCodec;

import cz.mcsworld.eroded.client.death.ErodedDeathCompassAngle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

@Environment(EnvType.CLIENT)
@Mixin(RangeSelectItemModelProperties.class)
public class NumericPropertiesMixin {

    @Shadow @Final
    public static ExtraCodecs.LateBoundIdMapper<ResourceLocation, MapCodec<? extends RangeSelectItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap()V", at = @At("TAIL"))
    private static void eroded$registerAngle(CallbackInfo info) {
        ID_MAPPER.put(ResourceLocation.fromNamespaceAndPath("eroded", "angle"), ErodedDeathCompassAngle.MAP_CODEC);
    }
}