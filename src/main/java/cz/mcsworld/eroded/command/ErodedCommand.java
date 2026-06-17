package cz.mcsworld.eroded.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import cz.mcsworld.eroded.config.ErodedConfigs;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.config.energy.EnergyHudPosition;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.network.SoundTuningSyncPacket;
import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryClaimState;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class ErodedCommand {

    private static final java.util.Map<java.util.UUID, Long> SOUND_COOLDOWN =
            new java.util.HashMap<>();

    private static final long SOUND_COOLDOWN_MS = 1000;

    private ErodedCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        registerInternal(dispatcher)
        );
    }

    private static void registerInternal(
            CommandDispatcher<ServerCommandSource> dispatcher
    ) {
        dispatcher.register(
                CommandManager.literal("eroded")

                        .then(CommandManager.literal("reload")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    try {
                                        ErodedConfigs.reload();

                                        ctx.getSource().sendFeedback(
                                                () -> Text.translatable(
                                                        "eroded.command.reload.success"
                                                ),
                                                false
                                        );

                                        return 1;

                                    } catch (Exception e) {
                                        ctx.getSource().sendError(
                                                Text.translatable(
                                                        "eroded.command.reload.error"
                                                )
                                        );

                                        e.printStackTrace();
                                        return 0;
                                    }
                                })
                        )

                        .then(CommandManager.literal("chest")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    ServerPlayerEntity player =
                                            ctx.getSource().getPlayer();

                                    ItemStack stack = new ItemStack(Items.CHEST);

                                    NbtCompound tag = new NbtCompound();
                                    tag.putBoolean(
                                            "eroded_loot_chest",
                                            true
                                    );

                                    stack.set(
                                            DataComponentTypes.CUSTOM_DATA,
                                            NbtComponent.of(tag)
                                    );

                                    assert player != null;
                                    player.giveItemStack(stack);

                                    ctx.getSource().sendFeedback(
                                            () -> Text.translatable(
                                                    "eroded.command.chest.success"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("energy")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument(
                                                        "player",
                                                        EntityArgumentType.player()
                                                )
                                                .then(CommandManager.argument(
                                                                        "amount",
                                                                        IntegerArgumentType.integer(0)
                                                                )
                                                                .executes(ctx -> {
                                                                    ServerPlayerEntity target =
                                                                            EntityArgumentType.getPlayer(
                                                                                    ctx,
                                                                                    "player"
                                                                            );

                                                                    int amount =
                                                                            IntegerArgumentType.getInteger(
                                                                                    ctx,
                                                                                    "amount"
                                                                            );

                                                                    SkillData data =
                                                                            SkillManager.get(target);

                                                                    data.setEnergy(amount);
                                                                    SkillManager.save(target);
                                                                    EnergySyncHandler.forceSync(target);

                                                                    ctx.getSource().sendFeedback(
                                                                            () -> Text.translatable(
                                                                                    "eroded.command.energy.set",
                                                                                    target.getName(),
                                                                                    amount
                                                                            ),
                                                                            true
                                                                    );

                                                                    return 1;
                                                                })
                                                )
                                )
                        )

                        .then(CommandManager.literal("sound")

                                .then(CommandManager.literal("volume")
                                        .then(CommandManager.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        10
                                                                )
                                                        )
                                                        .executes(ctx -> {
                                                            int value =
                                                                    IntegerArgumentType.getInteger(
                                                                            ctx,
                                                                            "value"
                                                                    );

                                                            ServerPlayerEntity player =
                                                                    ctx.getSource().getPlayer();

                                                            long now =
                                                                    System.currentTimeMillis();

                                                            assert player != null;

                                                            Long last =
                                                                    SOUND_COOLDOWN.get(
                                                                            player.getUuid()
                                                                    );

                                                            if (last != null
                                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                                ctx.getSource().sendError(
                                                                        Text.translatable(
                                                                                "eroded.command.sound.cooldown"
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            SOUND_COOLDOWN.put(
                                                                    player.getUuid(),
                                                                    now
                                                            );

                                                            float normalized =
                                                                    0.2f
                                                                            + ((value - 1) / 9.0f)
                                                                            * 0.8f;

                                                            SoundTuningSyncPacket.sendTo(
                                                                    player,
                                                                    normalized,
                                                                    null
                                                            );

                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.translatable(
                                                                            "eroded.command.sound.volume.set",
                                                                            value
                                                                    ),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                        )
                                )

                                .then(CommandManager.literal("delay")
                                        .then(CommandManager.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        10
                                                                )
                                                        )
                                                        .executes(ctx -> {
                                                            int value =
                                                                    IntegerArgumentType.getInteger(
                                                                            ctx,
                                                                            "value"
                                                                    );

                                                            ServerPlayerEntity player =
                                                                    ctx.getSource().getPlayer();

                                                            long now =
                                                                    System.currentTimeMillis();

                                                            assert player != null;

                                                            Long last =
                                                                    SOUND_COOLDOWN.get(
                                                                            player.getUuid()
                                                                    );

                                                            if (last != null
                                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                                ctx.getSource().sendError(
                                                                        Text.translatable(
                                                                                "eroded.command.sound.cooldown"
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            SOUND_COOLDOWN.put(
                                                                    player.getUuid(),
                                                                    now
                                                            );

                                                            float normalized =
                                                                    0.5f
                                                                            + ((value - 1) / 9.0f)
                                                                            * 1.5f;

                                                            SoundTuningSyncPacket.sendTo(
                                                                    player,
                                                                    null,
                                                                    normalized
                                                            );

                                                            ctx.getSource().sendFeedback(
                                                                    () -> Text.translatable(
                                                                            "eroded.command.sound.delay.set",
                                                                            value
                                                                    ),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                        )
                                )

                                .then(CommandManager.literal("info")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player =
                                                    ctx.getSource().getPlayer();

                                            long now =
                                                    System.currentTimeMillis();

                                            assert player != null;

                                            Long last =
                                                    SOUND_COOLDOWN.get(
                                                            player.getUuid()
                                                    );

                                            if (last != null
                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                ctx.getSource().sendError(
                                                        Text.translatable(
                                                                "eroded.command.sound.cooldown"
                                                        )
                                                );

                                                return 0;
                                            }

                                            SOUND_COOLDOWN.put(
                                                    player.getUuid(),
                                                    now
                                            );

                                            float volume =
                                                    cz.mcsworld.eroded.config.darkness.DarknessConfigs
                                                            .get()
                                                            .client
                                                            .audio
                                                            .volumeMultiplier;

                                            float delay =
                                                    cz.mcsworld.eroded.config.darkness.DarknessConfigs
                                                            .get()
                                                            .client
                                                            .audio
                                                            .delayMultiplier;

                                            int volumeUser =
                                                    Math.round(
                                                            (volume - 0.2f)
                                                                    / 0.8f
                                                                    * 9f
                                                    ) + 1;

                                            int delayUser =
                                                    Math.round(
                                                            (delay - 0.5f)
                                                                    / 1.5f
                                                                    * 9f
                                                    ) + 1;

                                            ctx.getSource().sendFeedback(
                                                    () -> Text.translatable(
                                                                    "eroded.command.sound.info.title"
                                                            )
                                                            .append(Text.translatable(
                                                                    "eroded.command.sound.info.volume",
                                                                    volumeUser
                                                            ))
                                                            .append(Text.translatable(
                                                                    "eroded.command.sound.info.delay",
                                                                    delayUser
                                                            )),
                                                    false
                                            );

                                            return 1;
                                        })
                                )

                                .then(CommandManager.literal("reset")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player =
                                                    ctx.getSource().getPlayer();

                                            long now =
                                                    System.currentTimeMillis();

                                            assert player != null;

                                            Long last =
                                                    SOUND_COOLDOWN.get(
                                                            player.getUuid()
                                                    );

                                            if (last != null
                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                ctx.getSource().sendError(
                                                        Text.translatable(
                                                                "eroded.command.sound.cooldown"
                                                        )
                                                );

                                                return 0;
                                            }

                                            SOUND_COOLDOWN.put(
                                                    player.getUuid(),
                                                    now
                                            );

                                            SoundTuningSyncPacket.sendTo(
                                                    player,
                                                    1.0f,
                                                    1.0f
                                            );

                                            ctx.getSource().sendFeedback(
                                                    () -> Text.translatable(
                                                            "eroded.command.sound.reset"
                                                    ),
                                                    false
                                            );

                                            return 1;
                                        })
                                )
                        )

                        .then(literal("icon")
                                .then(argument(
                                                "position",
                                                StringArgumentType.word()
                                        )
                                                .suggests((ctx, builder) -> {
                                                    for (var pos :
                                                            EnergyHudPosition.values()) {

                                                        builder.suggest(
                                                                pos.name()
                                                                        .toLowerCase()
                                                        );
                                                    }

                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    String input =
                                                            StringArgumentType.getString(
                                                                            ctx,
                                                                            "position"
                                                                    )
                                                                    .toUpperCase();

                                                    try {
                                                        EnergyHudPosition newPos =
                                                                EnergyHudPosition.valueOf(
                                                                        input
                                                                );

                                                        var cfg =
                                                                EnergyConfig.get()
                                                                        .client
                                                                        .hud;

                                                        cfg.hudPosition = newPos;

                                                        AutoConfig
                                                                .getConfigHolder(
                                                                        EnergyConfig.class
                                                                )
                                                                .save();

                                                        ctx.getSource().sendFeedback(
                                                                () -> Text.translatable(
                                                                        "eroded.energy.hud.position.changed",
                                                                        newPos.getTranslation()
                                                                ),
                                                                false
                                                        );

                                                    } catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendError(
                                                                Text.translatable(
                                                                        "eroded.energy.hud.position.invalid"
                                                                )
                                                        );
                                                    }

                                                    return 1;
                                                })
                                )
                        )

                        .then(CommandManager.literal("territory")
                                .requires(src -> src.hasPermissionLevel(2))

                                .then(CommandManager.literal("info")
                                        .executes(ctx -> {
                                            ServerCommandSource source =
                                                    ctx.getSource();

                                            ServerWorld world =
                                                    source.getWorld();

                                            ServerPlayerEntity player =
                                                    source.getPlayer();

                                            assert player != null;

                                            TerritoryClaim claim =
                                                    findClaimForCommand(
                                                            world,
                                                            player.getBlockPos()
                                                    );

                                            if (claim == null) {
                                                source.sendFeedback(
                                                        () -> Text.translatable(
                                                                "eroded.command.territory.info.none_here"
                                                        ),
                                                        false
                                                );

                                                return 0;
                                            }

                                            sendClaimInfo(source, claim);
                                            return 1;
                                        })
                                )

                                .then(CommandManager.literal("list")
                                        .executes(ctx -> {
                                            ServerCommandSource source =
                                                    ctx.getSource();

                                            ServerWorld world =
                                                    source.getWorld();

                                            List<TerritoryClaim> claims =
                                                    new ArrayList<>(
                                                            TerritoryClaimState
                                                                    .get(world)
                                                                    .all()
                                                    );

                                            if (claims.isEmpty()) {
                                                source.sendFeedback(
                                                        () -> Text.translatable(
                                                                "eroded.command.territory.list.empty"
                                                        ),
                                                        false
                                                );

                                                return 0;
                                            }

                                            source.sendFeedback(
                                                    () -> Text.translatable(
                                                            "eroded.command.territory.list.header",
                                                            claims.size()
                                                    ),
                                                    false
                                            );

                                            for (int i = 0;
                                                 i < claims.size();
                                                 i++) {

                                                TerritoryClaim claim =
                                                        claims.get(i);

                                                BlockPos pos =
                                                        claim.anchorPos();

                                                int index = i + 1;

                                                source.sendFeedback(
                                                        () -> Text.translatable(
                                                                "eroded.command.territory.list.entry",
                                                                index,
                                                                claim.ownerName(),
                                                                formatPos(pos),
                                                                claim.radius(),
                                                                claim.active()
                                                        ),
                                                        false
                                                );
                                            }

                                            return claims.size();
                                        })
                                )

                                .then(CommandManager.literal("remove")
                                        .executes(ctx -> {
                                            ServerCommandSource source =
                                                    ctx.getSource();

                                            ServerWorld world =
                                                    source.getWorld();

                                            ServerPlayerEntity player =
                                                    source.getPlayer();

                                            assert player != null;

                                            TerritoryClaim claim =
                                                    findClaimForCommand(
                                                            world,
                                                            player.getBlockPos()
                                                    );

                                            if (claim == null) {
                                                source.sendFeedback(
                                                        () -> Text.translatable(
                                                                "eroded.command.territory.remove.none_here"
                                                        ),
                                                        false
                                                );

                                                return 0;
                                            }

                                            removeClaimByAdmin(world, claim);

                                            source.sendFeedback(
                                                    () -> Text.translatable(
                                                            "eroded.command.territory.remove.success",
                                                            formatPos(
                                                                    claim.anchorPos()
                                                            )
                                                    ),
                                                    true
                                            );

                                            return 1;
                                        })

                                        .then(CommandManager.argument(
                                                                "pos",
                                                                BlockPosArgumentType.blockPos()
                                                        )
                                                        .executes(ctx -> {
                                                            ServerCommandSource source =
                                                                    ctx.getSource();

                                                            ServerWorld world =
                                                                    source.getWorld();

                                                            BlockPos pos =
                                                                    BlockPosArgumentType
                                                                            .getBlockPos(
                                                                                    ctx,
                                                                                    "pos"
                                                                            );

                                                            TerritoryClaim claim =
                                                                    findClaimForCommand(
                                                                            world,
                                                                            pos
                                                                    );

                                                            if (claim == null) {
                                                                source.sendFeedback(
                                                                        () -> Text.translatable(
                                                                                "eroded.command.territory.remove.none_at_pos"
                                                                        ),
                                                                        false
                                                                );

                                                                return 0;
                                                            }

                                                            removeClaimByAdmin(
                                                                    world,
                                                                    claim
                                                            );

                                                            source.sendFeedback(
                                                                    () -> Text.translatable(
                                                                            "eroded.command.territory.remove.success",
                                                                            formatPos(
                                                                                    claim.anchorPos()
                                                                            )
                                                                    ),
                                                                    true
                                                            );

                                                            return 1;
                                                        })
                                        )
                                )

                                .then(CommandManager.literal("clear")
                                        .executes(ctx -> {
                                            ServerCommandSource source =
                                                    ctx.getSource();

                                            ServerWorld world =
                                                    source.getWorld();

                                            List<TerritoryClaim> claims =
                                                    new ArrayList<>(
                                                            TerritoryClaimState
                                                                    .get(world)
                                                                    .all()
                                                    );

                                            if (claims.isEmpty()) {
                                                source.sendFeedback(
                                                        () -> Text.translatable(
                                                                "eroded.command.territory.clear.empty"
                                                        ),
                                                        false
                                                );

                                                return 0;
                                            }

                                            for (TerritoryClaim claim : claims) {
                                                removeClaimByAdmin(
                                                        world,
                                                        claim
                                                );
                                            }

                                            source.sendFeedback(
                                                    () -> Text.translatable(
                                                            "eroded.command.territory.clear.success",
                                                            claims.size()
                                                    ),
                                                    true
                                            );

                                            return claims.size();
                                        })
                                )
                        )
        );
    }

    private static TerritoryClaim findClaimForCommand(
            ServerWorld world,
            BlockPos pos
    ) {
        TerritoryClaim anchorClaim =
                TerritoryProtectionManager.getAnchorClaim(
                        world,
                        pos
                );

        if (anchorClaim != null) {
            return anchorClaim;
        }

        TerritoryClaim activeClaim =
                TerritoryProtectionManager.getActiveClaimAt(
                        world,
                        pos
                );

        if (activeClaim != null) {
            return activeClaim;
        }

        for (TerritoryClaim claim :
                TerritoryClaimState.get(world).all()) {

            if (claim.contains(pos)) {
                return claim;
            }
        }

        return null;
    }

    private static void sendClaimInfo(
            ServerCommandSource source,
            TerritoryClaim claim
    ) {
        BlockPos pos = claim.anchorPos();

        int areaSize = (claim.radius() * 2) + 1;

        source.sendFeedback(
                () -> Text.translatable(
                        "eroded.command.territory.info.title"
                ),
                false
        );

        source.sendFeedback(
                () -> Text.translatable(
                        "eroded.command.territory.info.owner",
                        claim.ownerName(),
                        claim.ownerUuid().toString()
                ),
                false
        );

        source.sendFeedback(
                () -> Text.translatable(
                        "eroded.command.territory.info.anchor",
                        formatPos(pos)
                ),
                false
        );

        source.sendFeedback(
                () -> Text.translatable(
                        "eroded.command.territory.info.radius",
                        claim.radius(),
                        areaSize,
                        areaSize
                ),
                false
        );

        source.sendFeedback(
                () -> Text.translatable(
                        "eroded.command.territory.info.active",
                        claim.active()
                ),
                false
        );
    }

    private static void removeClaimByAdmin(
            ServerWorld world,
            TerritoryClaim claim
    ) {
        BlockPos anchorPos = claim.anchorPos();

        TerritoryProtectionManager.removeClaim(
                world,
                anchorPos
        );

        removeAnchorBlockByAdmin(
                world,
                anchorPos
        );
    }

    private static void removeAnchorBlockByAdmin(
            ServerWorld world,
            BlockPos pos
    ) {
        BlockState state = world.getBlockState(pos);

        if (!state.isOf(ErodedBlocks.TERRITORY_ANCHOR)) {
            return;
        }

        world.setBlockState(
                pos,
                Blocks.AIR.getDefaultState(),
                Block.NOTIFY_ALL
        );
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX()
                + " "
                + pos.getY()
                + " "
                + pos.getZ();
    }
}