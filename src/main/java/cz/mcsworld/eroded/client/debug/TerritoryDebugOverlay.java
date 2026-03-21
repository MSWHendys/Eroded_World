package cz.mcsworld.eroded.client.debug;

import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

@Environment(EnvType.CLIENT)
public final class TerritoryDebugOverlay {

    private TerritoryDebugOverlay() {}

    public static void register() {

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {

            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null) return;
            if (client.world == null) return;

            if (!(client.world instanceof net.minecraft.client.world.ClientWorld)) return;

            if (!ErodedDebug.territoryOverlay) return;
            var player = client.player;

            if (client.getServer() == null) return;

            ServerWorld world =
                    client.getServer().getWorld(client.world.getRegistryKey());

            if (world == null) return;

            ChunkPos cp = new ChunkPos(player.getBlockPos());

            TerritoryWorldState state = TerritoryWorldState.get(world);

            TerritoryCell cell =
                    state.getOrCreateCell(
                            TerritoryCellKey.fromChunk(cp.x, cp.z)
                    );

            int mining = cell.getMiningRaw();
            int pollution = cell.getPollutionRaw();
            int forest = cell.getForestationRaw();
            int minedBlocks = cell.getMiningScore();

            float threat =
                    TerritoryThreatResolver.computeThreat(
                            cell,
                            world.getServer().getTicks()
                    );

            int x = 10;
            int y = 10;

            DrawContext context = drawContext;
            context.drawText(client.textRenderer, Text.translatable("eroded.debug.cell.title"), 10, 10, 0xFF55FF55, true);
            context.drawText(client.textRenderer, Text.translatable("eroded.debug.cell.mining_blocks").append(String.valueOf(minedBlocks)), 10, 22, 0xFFFFFFFF, false);
            context.drawText(client.textRenderer, Text.translatable("eroded.debug.cell.mining").append(String.valueOf(mining)), 10, 32, 0xFFFFFFFF, false);
            context.drawText(client.textRenderer, Text.translatable("eroded.debug.cell.pollution").append(String.valueOf(pollution)), 10, 42, 0xFFFF5555, false);
            context.drawText(client.textRenderer, Text.translatable("eroded.debug.cell.forest").append(String.valueOf(forest)), 10, 52, 0xFF55FF55, false);
            context.drawText(client.textRenderer, Text.translatable("eroded.debug.cell.threat").append(String.format("%.2f", threat)), 10, 62, 0xFFFFAA00, false);
        });
    }
}