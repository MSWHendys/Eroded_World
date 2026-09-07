package cz.mcsworld.eroded.energy;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla-tag based mining classification shared by Energy cost logic.
 *
 * HARD = pickaxe/axe work (stone, ores, deepslate, metal, wood, logs, ...)
 * SOFT = shovel/hoe work and uncategorised natural hand-work (dirt, sand,
 *        gravel, clay, snow, wool, glass, leaves, decorations, ...)
 */
public final class MiningBlockRules {

    public enum Category {
        HARD,
        SOFT
    }

    /**
     * How the currently held item relates to the block from the point of view
     * of Energy consumption. NATURAL_HAND_WORK is deliberately separate from
     * WRONG: vanilla has many soft blocks with no mineable-with-* tag at all.
     * Mining those by hand must not be charged as if a genuinely wrong tool
     * was being forced onto the block.
     */
    public enum ToolUse {
        FAST,
        SLOW,
        NATURAL_HAND_WORK,
        WRONG
    }

    private MiningBlockRules() {}

    public static Category category(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return Category.HARD;
        }
        return Category.SOFT;
    }

    /**
     * Returns true when vanilla explicitly assigns one of its ordinary mining
     * tool families to this block. Untagged blocks are treated as natural
     * hand-work unless the held item has a real vanilla destroy-speed bonus.
     */
    public static boolean hasTaggedMiningTool(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                || state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_HOE);
    }

    /**
     * Classifies the held item for deterministic Energy cost calculation.
     *
     * ItemStack#isCorrectToolForDrops is intentionally NOT used here: on
     * blocks that do not require a tool for drops it can say "correct" even
     * when vanilla gives that item no mining-speed advantage.
     */
    public static ToolUse toolUse(ItemStack stack, BlockState state) {
        boolean taggedToolBlock = hasTaggedMiningTool(state);

        if (stack.isEmpty()) {
            if (!taggedToolBlock) {
                return ToolUse.NATURAL_HAND_WORK;
            }

            // Bare hands are a legitimate, just inefficient, way to work
            // shovel/hoe-class SOFT blocks. HARD pickaxe/axe work still
            // treats bare hands as genuinely unsuitable.
            return category(state) == Category.SOFT ? ToolUse.SLOW : ToolUse.WRONG;
        }

        boolean taggedMatch = false;
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            taggedMatch |= stack.is(ItemTags.PICKAXES);
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            taggedMatch |= stack.is(ItemTags.AXES);
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            taggedMatch |= stack.is(ItemTags.SHOVELS);
        }
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) {
            taggedMatch |= stack.is(ItemTags.HOES);
        }

        float destroySpeed = stack.getDestroySpeed(state);

        // Preserve vanilla alternative efficiencies such as shears on wool/
        // leaves and modded tools that expose a real destroy-speed bonus.
        boolean effective = taggedMatch || destroySpeed > 1.0f;
        if (!effective) {
            // No ordinary vanilla mining-tool tag means there is no "wrong"
            // tool requirement to violate. A neutral held item behaves like
            // natural hand-work and keeps the SOFT baseline Energy cost.
            return taggedToolBlock ? ToolUse.WRONG : ToolUse.NATURAL_HAND_WORK;
        }

        return destroySpeed <= 2.0f ? ToolUse.SLOW : ToolUse.FAST;
    }

    /**
     * Convenience helper retained for callers that only need effective vs.
     * ineffective semantics.
     */
    public static boolean isSuitableTool(ItemStack stack, BlockState state) {
        ToolUse use = toolUse(stack, state);
        return use != ToolUse.WRONG;
    }
}
