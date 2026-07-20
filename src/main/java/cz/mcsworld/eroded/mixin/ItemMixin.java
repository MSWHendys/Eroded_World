package cz.mcsworld.eroded.mixin;

import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
            Level world,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (world.isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        ItemStack stack = player.getItemInHand(hand);

        if (stack.get(DataComponents.FOOD) == null) return;

        if (player.canEat(false)) return;

        SkillData data = SkillManager.get(sp);

        if (data.getEnergy() >= data.getMaxEnergy()) return;

        player.startUsingItem(hand);
        cir.setReturnValue(InteractionResult.CONSUME);
    }
}
