package cz.mcsworld.eroded.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResultSlot.class)
public interface CraftingResultSlotAccessor {

    @Accessor("craftSlots")
    CraftingContainer eroded$getInput();

    @Accessor("player")
    Player eroded$getPlayer();
}