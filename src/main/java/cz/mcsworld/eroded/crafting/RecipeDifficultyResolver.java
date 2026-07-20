package cz.mcsworld.eroded.crafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class RecipeDifficultyResolver {

    private static final TagKey<@NotNull Item> SIMPLE =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("eroded", "simple"));

    private static final TagKey<@NotNull Item> NORMAL =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("eroded", "normal"));

    private static final TagKey<@NotNull Item> COMPLEX =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("eroded", "complex"));

    private RecipeDifficultyResolver() {}

    public static RecipeDifficulty resolve(ItemStack result) {

        RecipeDifficulty difficulty;

        if (result.is(SIMPLE)) {
            difficulty = RecipeDifficulty.SIMPLE;
        } else if (result.is(NORMAL)) {
            difficulty = RecipeDifficulty.NORMAL;
        } else {
            difficulty = RecipeDifficulty.COMPLEX;
        }

        return difficulty;
    }
}
