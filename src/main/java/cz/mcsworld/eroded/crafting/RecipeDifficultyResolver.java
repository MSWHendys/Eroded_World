package cz.mcsworld.eroded.crafting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class RecipeDifficultyResolver {

    private static final TagKey<Item> SIMPLE =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("eroded", "simple"));

    private static final TagKey<Item> NORMAL =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("eroded", "normal"));

    private static final TagKey<Item> COMPLEX =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("eroded", "complex"));

    private RecipeDifficultyResolver() {}

    public static RecipeDifficulty resolve(ItemStack result) {

        RecipeDifficulty difficulty;

        if (result.isIn(SIMPLE)) {
            difficulty = RecipeDifficulty.SIMPLE;
        } else if (result.isIn(NORMAL)) {
            difficulty = RecipeDifficulty.NORMAL;
        } else {
            difficulty = RecipeDifficulty.COMPLEX;
        }

        return difficulty;
    }
}
