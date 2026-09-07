package cz.mcsworld.eroded.config.loot;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

import java.util.ArrayList;
import java.util.List;

public class LootConfig implements ErodedConfig {

    public boolean enabled = true;

    public int maxItemsPerChest = 5;

    public double erodedLootChance = 0.5;

    public List<LootEntry> loot = List.of(
            entry("minecraft:bread", 4, 0.6),
            entry("minecraft:cooked_beef", 3, 0.4),
            entry("minecraft:cooked_porkchop", 3, 0.4),
            entry("minecraft:cooked_chicken", 3, 0.4),
            entry("minecraft:apple", 4, 0.5),
            entry("minecraft:golden_apple", 1, 0.05),

            entry("minecraft:torch", 32, 0.8),
            entry("minecraft:lantern", 2, 0.25),

            entry("minecraft:stick", 16, 0.7),
            entry("minecraft:string", 8, 0.5),
            entry("minecraft:leather", 4, 0.4),
            entry("minecraft:bone", 6, 0.4),
            entry("minecraft:coal", 12, 0.6),
            entry("minecraft:charcoal", 8, 0.5),

            entry("minecraft:oak_planks", 16, 0.6),
            entry("minecraft:spruce_planks", 16, 0.4),
            entry("minecraft:birch_planks", 16, 0.4),

            entry("minecraft:wooden_sword", 1, 0.3),
            entry("minecraft:wooden_pickaxe", 1, 0.3),
            entry("minecraft:wooden_axe", 1, 0.3),
            entry("minecraft:wooden_shovel", 1, 0.3),

            entry("minecraft:stone_sword", 1, 0.25),
            entry("minecraft:stone_pickaxe", 1, 0.25),
            entry("minecraft:stone_axe", 1, 0.25),
            entry("minecraft:stone_shovel", 1, 0.25),

            entry("minecraft:iron_sword", 1, 0.15),
            entry("minecraft:iron_pickaxe", 1, 0.15),
            entry("minecraft:iron_axe", 1, 0.15),
            entry("minecraft:iron_shovel", 1, 0.15),

            entry("minecraft:bow", 1, 0.2),
            entry("minecraft:crossbow", 1, 0.1),
            entry("minecraft:arrow", 16, 0.4),

            entry("minecraft:leather_helmet", 1, 0.25),
            entry("minecraft:leather_chestplate", 1, 0.25),
            entry("minecraft:leather_leggings", 1, 0.25),
            entry("minecraft:leather_boots", 1, 0.25),

            entry("minecraft:iron_helmet", 1, 0.1),
            entry("minecraft:iron_chestplate", 1, 0.1),
            entry("minecraft:iron_leggings", 1, 0.1),
            entry("minecraft:iron_boots", 1, 0.1),

            entry("minecraft:iron_ingot", 4, 0.3),
            entry("minecraft:gold_ingot", 3, 0.2),
            entry("minecraft:diamond", 1, 0.05),
            entry("minecraft:emerald", 2, 0.1),

            entry("minecraft:redstone", 8, 0.25),
            entry("minecraft:lapis_lazuli", 8, 0.25),
            entry("minecraft:quartz", 6, 0.2),

            entry("minecraft:bucket", 1, 0.2),
            entry("minecraft:flint_and_steel", 1, 0.15),
            entry("minecraft:fishing_rod", 1, 0.2),
            entry("minecraft:shears", 1, 0.2),

            entry("minecraft:enchanted_book", 1, 0.05),
            entry("minecraft:name_tag", 1, 0.05),
            entry("minecraft:saddle", 1, 0.05),

            entry("minecraft:trial_key", 1, 0.05),

            entry("eroded:territory_module", 1, 0.05),
            entry("eroded:territory_anchor", 1, 0.09),
            entry("eroded:energy_drink", 1, 0.5),
            entry("eroded:adrenaline_shot", 1, 0.1),
            entry("eroded:eroded_lamp", 1, 0.01),
            entry("eroded:eroded_torch", 1, 0.3)
    );

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.range(maxItemsPerChest, 0, 54, "loot.maxItemsPerChest");
        ConfigValidation.range(erodedLootChance, 0.0, 1.0, "loot.erodedLootChance");
        ConfigValidation.notNull(loot, "loot.loot");

        for (int i = 0; i < loot.size(); i++) {
            LootEntry entry = loot.get(i);
            ConfigValidation.notNull(entry, "loot.loot[" + i + "]");
            ConfigValidation.notNull(entry.item, "loot.loot[" + i + "].item");
            ConfigValidation.require(!entry.item.isBlank(), "loot.loot[" + i + "].item", "must not be blank");
            ConfigValidation.min(entry.maxStack, 1, "loot.loot[" + i + "].maxStack");
            ConfigValidation.range(entry.chance, 0.0, 1.0, "loot.loot[" + i + "].chance");
        }
    }

    public static LootConfig get() {
        return ErodedConfigs.LOOT;
    }

    public boolean ensureDefaultEntries() {
        ArrayList<LootEntry> updated = new ArrayList<>(loot);
        boolean changed = false;

        changed |= addIfMissing(updated, "eroded:territory_module", 1, 0.05);
        changed |= addIfMissing(updated, "eroded:territory_anchor", 1, 0.09);
        changed |= addIfMissing(updated, "eroded:energy_drink", 1, 0.5);
        changed |= addIfMissing(updated, "eroded:adrenaline_shot", 1, 0.1);
        changed |= addIfMissing(updated, "eroded:eroded_lamp", 1, 0.01);
        changed |= addIfMissing(updated, "eroded:eroded_torch", 1, 0.3);

        if (changed) {
            loot = updated;
        }

        return changed;
    }

    private static boolean addIfMissing(
            List<LootEntry> list,
            String item,
            int maxStack,
            double chance
    ) {
        for (LootEntry entry : list) {
            if (entry.item != null
                    && entry.item.equalsIgnoreCase(item)) {
                return false;
            }
        }

        list.add(entry(item, maxStack, chance));
        return true;
    }

    private static LootEntry entry(
            String item,
            int maxStack,
            double chance
    ) {
        LootEntry entry = new LootEntry();

        entry.item = item;
        entry.maxStack = maxStack;
        entry.chance = chance;

        return entry;
    }
}
