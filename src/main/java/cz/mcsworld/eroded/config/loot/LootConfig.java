package cz.mcsworld.eroded.config.loot;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.List;

@Config(name = "ErodedWorld/loot")
public class LootConfig implements ConfigData {

    public static LootConfig get() {
        return AutoConfig.getConfigHolder(LootConfig.class).getConfig();
    }

    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public int maxItemsPerChest = 5;

    @ConfigEntry.Gui.Tooltip
    public double erodedLootChance = 0.5; // 50%

    @ConfigEntry.Gui.Tooltip
    public List<LootEntry> loot = List.of(

            // FOOD
            entry("minecraft:bread", 4, 0.6),
            entry("minecraft:cooked_beef", 3, 0.4),
            entry("minecraft:cooked_porkchop", 3, 0.4),
            entry("minecraft:cooked_chicken", 3, 0.4),
            entry("minecraft:apple", 4, 0.5),
            entry("minecraft:golden_apple", 1, 0.05),

            // LIGHT
            entry("minecraft:torch", 32, 0.8),
            entry("minecraft:lantern", 2, 0.25),

            // BASIC MATERIALS
            entry("minecraft:stick", 16, 0.7),
            entry("minecraft:string", 8, 0.5),
            entry("minecraft:leather", 4, 0.4),
            entry("minecraft:bone", 6, 0.4),
            entry("minecraft:coal", 12, 0.6),
            entry("minecraft:charcoal", 8, 0.5),

            // WOOD
            entry("minecraft:oak_planks", 16, 0.6),
            entry("minecraft:spruce_planks", 16, 0.4),
            entry("minecraft:birch_planks", 16, 0.4),

            // TOOLS - WOOD
            entry("minecraft:wooden_sword", 1, 0.3),
            entry("minecraft:wooden_pickaxe", 1, 0.3),
            entry("minecraft:wooden_axe", 1, 0.3),
            entry("minecraft:wooden_shovel", 1, 0.3),

            // TOOLS - STONE
            entry("minecraft:stone_sword", 1, 0.25),
            entry("minecraft:stone_pickaxe", 1, 0.25),
            entry("minecraft:stone_axe", 1, 0.25),
            entry("minecraft:stone_shovel", 1, 0.25),

            // TOOLS - IRON
            entry("minecraft:iron_sword", 1, 0.15),
            entry("minecraft:iron_pickaxe", 1, 0.15),
            entry("minecraft:iron_axe", 1, 0.15),
            entry("minecraft:iron_shovel", 1, 0.15),

            // RANGED
            entry("minecraft:bow", 1, 0.2),
            entry("minecraft:crossbow", 1, 0.1),
            entry("minecraft:arrow", 16, 0.4),

            // ARMOR - LEATHER
            entry("minecraft:leather_helmet", 1, 0.25),
            entry("minecraft:leather_chestplate", 1, 0.25),
            entry("minecraft:leather_leggings", 1, 0.25),
            entry("minecraft:leather_boots", 1, 0.25),

            // ARMOR - IRON
            entry("minecraft:iron_helmet", 1, 0.1),
            entry("minecraft:iron_chestplate", 1, 0.1),
            entry("minecraft:iron_leggings", 1, 0.1),
            entry("minecraft:iron_boots", 1, 0.1),

            // ORES / VALUABLES
            entry("minecraft:iron_ingot", 4, 0.3),
            entry("minecraft:gold_ingot", 3, 0.2),
            entry("minecraft:diamond", 1, 0.05),
            entry("minecraft:emerald", 2, 0.1),

            // REDSTONE / TECH
            entry("minecraft:redstone", 8, 0.25),
            entry("minecraft:lapis_lazuli", 8, 0.25),
            entry("minecraft:quartz", 6, 0.2),

            // UTILITY
            entry("minecraft:bucket", 1, 0.2),
            entry("minecraft:flint_and_steel", 1, 0.15),
            entry("minecraft:fishing_rod", 1, 0.2),
            entry("minecraft:shears", 1, 0.2),

            // RARE
            entry("minecraft:enchanted_book", 1, 0.05),
            entry("minecraft:name_tag", 1, 0.05),
            entry("minecraft:saddle", 1, 0.05),

            // SPECIAL
            entry("minecraft:trial_key", 1, 0.05)
    );

    private static LootEntry entry(String item, int max, double chance) {

        LootEntry e = new LootEntry();

        e.item = item;
        e.maxStack = max;
        e.chance = chance;

        return e;
    }
}