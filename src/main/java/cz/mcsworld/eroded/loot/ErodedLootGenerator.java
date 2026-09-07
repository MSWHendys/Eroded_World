package cz.mcsworld.eroded.loot;

import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.loot.LootEntry;

import cz.mcsworld.eroded.crafting.CraftingQualityApplier;
import cz.mcsworld.eroded.crafting.QualityApplicable;
import cz.mcsworld.eroded.crafting.Quality;

import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ErodedLootGenerator {

    private static final Random random = new Random();

    public static void generate(Container inv) {

        LootConfig config = LootConfig.get();

        if (config.loot == null || config.loot.isEmpty())
            return;

        int max = Math.min(config.maxItemsPerChest, inv.getContainerSize());

        for (int i = 0; i < max; i++) {

            LootEntry entry = config.loot.get(random.nextInt(config.loot.size()));

            if (random.nextDouble() > entry.chance)
                continue;

            ResourceLocation id = ResourceLocation.tryParse(entry.item);

            if (id == null)
                continue;

            Item item = BuiltInRegistries.ITEM.getValue(id);

            if (item == null)
                continue;

            ItemStack stack = new ItemStack(item);
            int count = 1 + random.nextInt(Math.max(1, entry.maxStack));
            count = Math.min(count, stack.getMaxStackSize());
            stack.setCount(count);

            applyQuality(stack);

            int slot = random.nextInt(inv.getContainerSize());

            if (inv.getItem(slot).isEmpty()) {
                inv.setItem(slot, stack);
            }
        }
    }



    private static void applyQuality(ItemStack stack) {

        if (stack.isEmpty())
            return;

        if (QualityApplicable.isApplicable(stack)) {
            CraftingQualityApplier.apply(stack, Quality.POOR);
        }

        stack.setDamageValue(0);
    }
}