package cz.mcsworld.eroded.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import cz.mcsworld.eroded.config.ErodedConfigs;
import cz.mcsworld.eroded.world.territory.*;
import cz.mcsworld.eroded.world.territory.ecosystem.TerritoryEcosystemTicker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

public final class ErodedCommand {

    private ErodedCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        registerInternal(dispatcher)
        );
    }

    private static void registerInternal(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
                CommandManager.literal("eroded")
                        .requires(src -> src.hasPermissionLevel(2))

                        .then(CommandManager.literal("reload")
                                .executes(ctx -> {
                                    ErodedConfigs.reload();
                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("§a[Eroded] Konfigurace byla znovu načtena."),
                                            false
                                    );
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("force")
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    TerritoryEcosystemTicker.forceTick(
                                            (ServerWorld) player.getWorld()
                                    );

                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal("§c[Ecosystem] FORCE tick proveden."),
                                            false
                                    );
                                    return 1;
                                })
                        )

                        .then(CommandManager.literal("territory")

                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    var world = player.getWorld();
                                    long tick = world.getServer().getTicks();

                                    ChunkPos cp = new ChunkPos(player.getBlockPos());
                                    TerritoryData data = TerritoryStorage.get(world, cp);

                                    int mining = data.getMining(tick);
                                    int forest = data.getForestation(tick);
                                    int pollution = data.getPollution(tick);

                                    float threat =
                                            TerritoryThreatResolver.computeThreat(data, tick);

                                    ctx.getSource().sendFeedback(
                                            () -> Text.literal(
                                                    "§6[Území]\n" +
                                                            "§7Chunk: §f" + cp.x + " / " + cp.z + "\n" +
                                                            "§7Těžba: §f" + mining + "\n" +
                                                            "§7Zalesnění: §f" + forest + "\n" +
                                                            "§7Znečištění: §f" + pollution + "\n" +
                                                            "§7Hrozba: §f" + String.format("%.2f", threat)
                                            ),
                                            false
                                    );
                                    return 1;
                                })

                                .then(CommandManager.literal("add")
                                        .then(CommandManager.argument("type", StringArgumentType.word())
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> modifyTerritory(
                                                                ctx.getSource(),
                                                                ctx.getArgument("type", String.class),
                                                                ctx.getArgument("value", Integer.class),
                                                                false
                                                        ))
                                                )
                                        )
                                )

                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("type", StringArgumentType.word())
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> modifyTerritory(
                                                                ctx.getSource(),
                                                                ctx.getArgument("type", String.class),
                                                                ctx.getArgument("value", Integer.class),
                                                                true
                                                        ))
                                                )
                                        )
                                )

                                .then(CommandManager.literal("reset")
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                                            var world = player.getWorld();
                                            long tick = world.getServer().getTicks();

                                            TerritoryData data =
                                                    TerritoryStorage.get(world, new ChunkPos(player.getBlockPos()));

                                            data.addMining(-data.getMining(tick), tick);
                                            data.addForestation(-data.getForestation(tick), tick);
                                            data.addPollution(-data.getPollution(tick), tick);

                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("§a[Území] Hodnoty byly resetovány."),
                                                    false
                                            );
                                            return 1;
                                        })
                                )

                                .then(CommandManager.literal("ecosystem")

                                        .then(CommandManager.literal("tick")
                                                .executes(ctx -> {
                                                    MinecraftServer server = ctx.getSource().getServer();
                                                    TerritoryEcosystemTicker.register();
                                                    server.getPlayerManager().broadcast(
                                                            Text.literal("§e[Ekosystém] Testovací tick byl spuštěn."),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )

                                        .then(CommandManager.literal("preview")
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                                    var world = player.getWorld();
                                                    long tick = world.getServer().getTicks();

                                                    TerritoryData data =
                                                            TerritoryStorage.get(world, new ChunkPos(player.getBlockPos()));

                                                    float threat =
                                                            TerritoryThreatResolver.computeThreat(data, tick);

                                                    int pollution = data.getPollution(tick);

                                                    ctx.getSource().sendFeedback(
                                                            () -> Text.literal(
                                                                    "§6[Náhled ekosystému]\n" +
                                                                            "§7Degradace: §f" + (threat > 0.6f) + "\n" +
                                                                            "§7Regenerace: §f" + (threat < 0.25f && pollution < 50) + "\n" +
                                                                            "§7Trvalé jizvy: §f" + (threat > 0.85f && pollution > 80)
                                                            ),
                                                            false
                                                    );
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static int modifyTerritory(
            ServerCommandSource source,
            String type,
            int value,
            boolean absolute
    ) {
        ServerPlayerEntity player = source.getPlayer();
        var world = player.getWorld();
        long tick = world.getServer().getTicks();

        TerritoryData data =
                TerritoryStorage.get(world, new ChunkPos(player.getBlockPos()));

        int current;

        switch (type.toLowerCase()) {
            case "mining" -> {
                current = data.getMining(tick);
                data.addMining(absolute ? value - current : value, tick);
            }
            case "forest", "forestation" -> {
                current = data.getForestation(tick);
                data.addForestation(absolute ? value - current : value, tick);
            }
            case "pollution" -> {
                current = data.getPollution(tick);
                data.addPollution(absolute ? value - current : value, tick);
            }
            default -> {
                source.sendError(Text.literal("§cNeznámý typ hodnoty: " + type));
                return 0;
            }
        }

        source.sendFeedback(
                () -> Text.literal("§a[Území] Hodnota '" + type + "' byla upravena."),
                false
        );
        return 1;
    }
}
