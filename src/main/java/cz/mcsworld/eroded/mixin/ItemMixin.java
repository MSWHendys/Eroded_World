package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(
            method = "use",
            at = @At("HEAD"),
            cancellable = true
    )
    private void eroded$allowEatingForEnergy(
            World world,
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (world.isClient) return;
        if (!(player instanceof ServerPlayerEntity sp)) return;

        ItemStack stack = player.getStackInHand(hand);

        if (stack.get(DataComponentTypes.FOOD) == null) return;

        if (player.canConsume(false)) return;

        SkillData data = SkillManager.get(sp);

        if (data.getEnergy() >= data.getMaxEnergy()) return;

        player.setCurrentHand(hand);
        cir.setReturnValue(ActionResult.CONSUME);
    }
}
