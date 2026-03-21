package cz.mcsworld.eroded.loot;

import cz.mcsworld.eroded.config.loot.LootConfig;
import cz.mcsworld.eroded.config.loot.LootEntry;

import cz.mcsworld.eroded.crafting.CraftingQualityApplier;
import cz.mcsworld.eroded.crafting.QualityApplicable;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import cz.mcsworld.eroded.crafting.Quality;

import java.util.Random;

public class ErodedLootGenerator {

    private static final Random random = new Random();

    public static void generate(Inventory inv) {

        LootConfig config = LootConfig.get();

        if (config.loot == null || config.loot.isEmpty())
            return;

        int max = Math.min(config.maxItemsPerChest, inv.size());

        for (int i = 0; i < max; i++) {

            LootEntry entry = config.loot.get(random.nextInt(config.loot.size()));

            if (random.nextDouble() > entry.chance)
                continue;

            Identifier id = Identifier.tryParse(entry.item);

            if (id == null)
                continue;

            Item item = Registries.ITEM.get(id);

            if (item == null)
                continue;

            int count = 1 + random.nextInt(Math.max(1, entry.maxStack));

            ItemStack stack = new ItemStack(item, count);

            applyQuality(stack);

            int slot = random.nextInt(inv.size());

            if (inv.getStack(slot).isEmpty()) {
                inv.setStack(slot, stack);
            }
        }
    }



    private static void applyQuality(ItemStack stack) {

        if (stack.isEmpty())
            return;

        if (QualityApplicable.isApplicable(stack)) {
            CraftingQualityApplier.apply(stack, Quality.POOR);
        }

        stack.setDamage(0);
    }
}