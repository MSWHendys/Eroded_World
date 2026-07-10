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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

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
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal("eroded")

                        .then(Commands.literal("reload")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    try {
                                        ErodedConfigs.reload();

                                        ctx.getSource().sendSuccess(
                                                () -> Component.translatable(
                                                        "eroded.command.reload.success"
                                                ),
                                                false
                                        );

                                        return 1;

                                    } catch (Exception e) {
                                        ctx.getSource().sendFailure(
                                                Component.translatable(
                                                        "eroded.command.reload.error"
                                                )
                                        );

                                        e.printStackTrace();
                                        return 0;
                                    }
                                })
                        )

                        .then(Commands.literal("chest")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer player =
                                            ctx.getSource().getPlayer();

                                    ItemStack stack = new ItemStack(Items.CHEST);

                                    CompoundTag tag = new CompoundTag();
                                    tag.putBoolean(
                                            "eroded_loot_chest",
                                            true
                                    );

                                    stack.set(
                                            DataComponents.CUSTOM_DATA,
                                            CustomData.of(tag)
                                    );

                                    assert player != null;
                                    player.addItem(stack);

                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable(
                                                    "eroded.command.chest.success"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )

                        .then(Commands.literal("energy")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument(
                                                        "player",
                                                        EntityArgument.player()
                                                )
                                                .then(Commands.argument(
                                                                        "amount",
                                                                        IntegerArgumentType.integer(0)
                                                                )
                                                                .executes(ctx -> {
                                                                    ServerPlayer target =
                                                                            EntityArgument.getPlayer(
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

                                                                    ctx.getSource().sendSuccess(
                                                                            () -> Component.translatable(
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

                        .then(Commands.literal("sound")

                                .then(Commands.literal("volume")
                                        .then(Commands.argument(
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

                                                            ServerPlayer player =
                                                                    ctx.getSource().getPlayer();

                                                            long now =
                                                                    System.currentTimeMillis();

                                                            assert player != null;

                                                            Long last =
                                                                    SOUND_COOLDOWN.get(
                                                                            player.getUUID()
                                                                    );

                                                            if (last != null
                                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                                ctx.getSource().sendFailure(
                                                                        Component.translatable(
                                                                                "eroded.command.sound.cooldown"
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            SOUND_COOLDOWN.put(
                                                                    player.getUUID(),
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

                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.translatable(
                                                                            "eroded.command.sound.volume.set",
                                                                            value
                                                                    ),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                        )
                                )

                                .then(Commands.literal("delay")
                                        .then(Commands.argument(
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

                                                            ServerPlayer player =
                                                                    ctx.getSource().getPlayer();

                                                            long now =
                                                                    System.currentTimeMillis();

                                                            assert player != null;

                                                            Long last =
                                                                    SOUND_COOLDOWN.get(
                                                                            player.getUUID()
                                                                    );

                                                            if (last != null
                                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                                ctx.getSource().sendFailure(
                                                                        Component.translatable(
                                                                                "eroded.command.sound.cooldown"
                                                                        )
                                                                );

                                                                return 0;
                                                            }

                                                            SOUND_COOLDOWN.put(
                                                                    player.getUUID(),
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

                                                            ctx.getSource().sendSuccess(
                                                                    () -> Component.translatable(
                                                                            "eroded.command.sound.delay.set",
                                                                            value
                                                                    ),
                                                                    false
                                                            );

                                                            return 1;
                                                        })
                                        )
                                )

                                .then(Commands.literal("info")
                                        .executes(ctx -> {
                                            ServerPlayer player =
                                                    ctx.getSource().getPlayer();

                                            long now =
                                                    System.currentTimeMillis();

                                            assert player != null;

                                            Long last =
                                                    SOUND_COOLDOWN.get(
                                                            player.getUUID()
                                                    );

                                            if (last != null
                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                ctx.getSource().sendFailure(
                                                        Component.translatable(
                                                                "eroded.command.sound.cooldown"
                                                        )
                                                );

                                                return 0;
                                            }

                                            SOUND_COOLDOWN.put(
                                                    player.getUUID(),
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

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(
                                                                    "eroded.command.sound.info.title"
                                                            )
                                                            .append(Component.translatable(
                                                                    "eroded.command.sound.info.volume",
                                                                    volumeUser
                                                            ))
                                                            .append(Component.translatable(
                                                                    "eroded.command.sound.info.delay",
                                                                    delayUser
                                                            )),
                                                    false
                                            );

                                            return 1;
                                        })
                                )

                                .then(Commands.literal("reset")
                                        .executes(ctx -> {
                                            ServerPlayer player =
                                                    ctx.getSource().getPlayer();

                                            long now =
                                                    System.currentTimeMillis();

                                            assert player != null;

                                            Long last =
                                                    SOUND_COOLDOWN.get(
                                                            player.getUUID()
                                                    );

                                            if (last != null
                                                    && now - last < SOUND_COOLDOWN_MS) {

                                                ctx.getSource().sendFailure(
                                                        Component.translatable(
                                                                "eroded.command.sound.cooldown"
                                                        )
                                                );

                                                return 0;
                                            }

                                            SOUND_COOLDOWN.put(
                                                    player.getUUID(),
                                                    now
                                            );

                                            SoundTuningSyncPacket.sendTo(
                                                    player,
                                                    1.0f,
                                                    1.0f
                                            );

                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable(
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

                                                        ctx.getSource().sendSuccess(
                                                                () -> Component.translatable(
                                                                        "eroded.energy.hud.position.changed",
                                                                        newPos.getTranslation()
                                                                ),
                                                                false
                                                        );

                                                    } catch (IllegalArgumentException e) {
                                                        ctx.getSource().sendFailure(
                                                                Component.translatable(
                                                                        "eroded.energy.hud.position.invalid"
                                                                )
                                                        );
                                                    }

                                                    return 1;
                                                })
                                )
                        )

                        .then(Commands.literal("territory")
                                .requires(src -> src.hasPermission(2))

                                .then(Commands.literal("info")
                                        .executes(ctx -> {
                                            CommandSourceStack source =
                                                    ctx.getSource();

                                            ServerLevel world =
                                                    source.getLevel();

                                            ServerPlayer player =
                                                    source.getPlayer();

                                            assert player != null;

                                            TerritoryClaim claim =
                                                    findClaimForCommand(
                                                            world,
                                                            player.blockPosition()
                                                    );

                                            if (claim == null) {
                                                source.sendSuccess(
                                                        () -> Component.translatable(
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

                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            CommandSourceStack source =
                                                    ctx.getSource();

                                            ServerLevel world =
                                                    source.getLevel();

                                            List<TerritoryClaim> claims =
                                                    new ArrayList<>(
                                                            TerritoryClaimState
                                                                    .get(world)
                                                                    .all()
                                                    );

                                            if (claims.isEmpty()) {
                                                source.sendSuccess(
                                                        () -> Component.translatable(
                                                                "eroded.command.territory.list.empty"
                                                        ),
                                                        false
                                                );

                                                return 0;
                                            }

                                            source.sendSuccess(
                                                    () -> Component.translatable(
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

                                                source.sendSuccess(
                                                        () -> Component.translatable(
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

                                .then(Commands.literal("remove")
                                        .executes(ctx -> {
                                            CommandSourceStack source =
                                                    ctx.getSource();

                                            ServerLevel world =
                                                    source.getLevel();

                                            ServerPlayer player =
                                                    source.getPlayer();

                                            assert player != null;

                                            TerritoryClaim claim =
                                                    findClaimForCommand(
                                                            world,
                                                            player.blockPosition()
                                                    );

                                            if (claim == null) {
                                                source.sendSuccess(
                                                        () -> Component.translatable(
                                                                "eroded.command.territory.remove.none_here"
                                                        ),
                                                        false
                                                );

                                                return 0;
                                            }

                                            removeClaimByAdmin(world, claim);

                                            source.sendSuccess(
                                                    () -> Component.translatable(
                                                            "eroded.command.territory.remove.success",
                                                            formatPos(
                                                                    claim.anchorPos()
                                                            )
                                                    ),
                                                    true
                                            );

                                            return 1;
                                        })

                                        .then(Commands.argument(
                                                                "pos",
                                                                BlockPosArgument.blockPos()
                                                        )
                                                        .executes(ctx -> {
                                                            CommandSourceStack source =
                                                                    ctx.getSource();

                                                            ServerLevel world =
                                                                    source.getLevel();

                                                            BlockPos pos =
                                                                    BlockPosArgument
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
                                                                source.sendSuccess(
                                                                        () -> Component.translatable(
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

                                                            source.sendSuccess(
                                                                    () -> Component.translatable(
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

                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            CommandSourceStack source =
                                                    ctx.getSource();

                                            ServerLevel world =
                                                    source.getLevel();

                                            List<TerritoryClaim> claims =
                                                    new ArrayList<>(
                                                            TerritoryClaimState
                                                                    .get(world)
                                                                    .all()
                                                    );

                                            if (claims.isEmpty()) {
                                                source.sendSuccess(
                                                        () -> Component.translatable(
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

                                            source.sendSuccess(
                                                    () -> Component.translatable(
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
            ServerLevel world,
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
            CommandSourceStack source,
            TerritoryClaim claim
    ) {
        BlockPos pos = claim.anchorPos();

        int areaSize = (claim.radius() * 2) + 1;

        source.sendSuccess(
                () -> Component.translatable(
                        "eroded.command.territory.info.title"
                ),
                false
        );

        source.sendSuccess(
                () -> Component.translatable(
                        "eroded.command.territory.info.owner",
                        claim.ownerName(),
                        claim.ownerUuid().toString()
                ),
                false
        );

        source.sendSuccess(
                () -> Component.translatable(
                        "eroded.command.territory.info.anchor",
                        formatPos(pos)
                ),
                false
        );

        source.sendSuccess(
                () -> Component.translatable(
                        "eroded.command.territory.info.radius",
                        claim.radius(),
                        areaSize,
                        areaSize
                ),
                false
        );

        source.sendSuccess(
                () -> Component.translatable(
                        "eroded.command.territory.info.active",
                        claim.active()
                ),
                false
        );
    }

    private static void removeClaimByAdmin(
            ServerLevel world,
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
            ServerLevel world,
            BlockPos pos
    ) {
        BlockState state = world.getBlockState(pos);

        if (!state.is(ErodedBlocks.TERRITORY_ANCHOR)) {
            return;
        }

        world.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
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