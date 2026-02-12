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
import net.minecraft.client.render.item.property.numeric.NumericProperties;
import net.minecraft.client.render.item.property.numeric.NumericProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

@Environment(EnvType.CLIENT)
@Mixin(NumericProperties.class)
public class NumericPropertiesMixin {

    @Shadow @Final
    public static Codecs.IdMapper<Identifier, MapCodec<? extends NumericProperty>> ID_MAPPER;

    @Inject(method = "bootstrap()V", at = @At("TAIL"))
    private static void eroded$registerAngle(CallbackInfo info) {
        ID_MAPPER.put(Identifier.of("eroded", "angle"), ErodedDeathCompassAngle.MAP_CODEC);
    }
}