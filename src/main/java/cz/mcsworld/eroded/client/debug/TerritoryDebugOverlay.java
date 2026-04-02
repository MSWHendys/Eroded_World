package cz.mcsworld.eroded.client.debug;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.data.ClientSkillData;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class TerritoryDebugOverlay {

    private TerritoryDebugOverlay() {}

    private record DebugLine(Text text, int color, int extraYSpace) {}

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null || client.world == null || !ErodedDebug.territoryOverlay) return;
            if (client.getServer() == null) return;

            ServerWorld world = client.getServer().getWorld(client.world.getRegistryKey());
            if (world == null) return;

            var player = client.player;
            int energy = ClientEnergyData.getEnergy();
            int maxEnergy = ClientEnergyData.getMaxEnergy();
            float wood = ClientSkillData.getWoodworking();
            float smelt = ClientSkillData.getSmelting();
            float avgCg = (wood + smelt) / 2f;

            float percent = (maxEnergy > 0) ? (energy / (float) maxEnergy) * 100f : 100f;
            var cfg = EnergyConfig.get().server.thresholds;
            var craftingCfg = CraftingConfig.get().quality;

            String energyState = (percent <= cfg.emptyPercent) ? "EMPTY" :
                    (percent <= cfg.exhaustedPercent) ? "EXHAUSTED" :
                            (percent <= cfg.tiredPercent) ? "TIRED" : "NORMAL";

            ChunkPos cp = new ChunkPos(player.getBlockPos());
            TerritoryWorldState state = TerritoryWorldState.get(world);
            TerritoryCell cell = state.getOrCreateCell(TerritoryCellKey.fromChunk(cp.x, cp.z));

            float threat = TerritoryThreatResolver.computeThreat(cell, world.getServer().getTicks());

            String predictedKey = (avgCg < craftingCfg.qualityPoorToStandard) ? "eroded.crafting.quality.poor" :
                    (avgCg < craftingCfg.qualityStandardToExcellent) ? "eroded.crafting.quality.standard" :
                            "eroded.crafting.quality.excellent";

            List<DebugLine> lines = new ArrayList<>();
            lines.add(new DebugLine(Text.translatable("eroded.debug.cell.title"), 0xFF55FF55, 0));
            lines.add(new DebugLine(Text.translatable("eroded.debug.cell.mining_blocks").append(String.valueOf(cell.getMiningScore())), 0xFFFFFFFF, 0));
            lines.add(new DebugLine(Text.translatable("eroded.debug.cell.mining").append(String.valueOf(cell.getMiningRaw())), 0xFFFFFFFF, 0));
            lines.add(new DebugLine(Text.translatable("eroded.debug.cell.pollution").append(String.valueOf(cell.getPollutionRaw())), 0xFFFF5555, 0));
            lines.add(new DebugLine(Text.translatable("eroded.debug.cell.forest").append(String.valueOf(cell.getForestationRaw())), 0xFF55FF55, 0));
            lines.add(new DebugLine(Text.translatable("eroded.debug.cell.threat").append(String.format("%.2f", threat)), 0xFFFFAA00, 5));

            lines.add(new DebugLine(Text.literal(Text.translatable("eroded.config.title.energy").getString().toUpperCase() + ":"),0xFF55FF55, 0));
            lines.add(new DebugLine(Text.translatable("eroded.config.title.energy").append(": " + energy + " / " + maxEnergy), 0xFFFFFF00, 0));
            lines.add(new DebugLine(Text.translatable("eroded.debug.energy.state").append(": " + energyState), 0xFFFFAA00, 5));

            lines.add(new DebugLine(Text.literal("CG:"), 0xFF55FF55, 0));
            lines.add(new DebugLine(Text.translatable("eroded.skill.woodworking").append(": " + String.format("%.2f", wood)), 0xFFFFFFFF, 0));
            lines.add(new DebugLine(Text.translatable("eroded.skill.smelting").append(": " + String.format("%.2f", smelt)), 0xFFFFFFFF, 5));

            lines.add(new DebugLine(Text.translatable("eroded.text.quality.line"), 0xFF55FF55, 0));
            lines.add(new DebugLine(Text.translatable("eroded.crafting.quality.poor.standard").append(" " + craftingCfg.qualityPoorToStandard + " cg"), 0xFFFFFFFF, 0));
            lines.add(new DebugLine(Text.translatable("eroded.crafting.quality.standard.excellent").append(" " + craftingCfg.qualityStandardToExcellent + " cg"), 0xFFFFFFFF, 0));
            lines.add(new DebugLine(Text.literal("CG avg: " + String.format("%.2f", avgCg)), 0xFF55FFFF, 0));
            lines.add(new DebugLine(Text.translatable("eroded.crafting.predicted").append(Text.translatable(predictedKey)), 0xFFFFFF55, 0));

            TextRenderer tr = client.textRenderer;
            int maxWidth = 0;
            int totalHeight = 0;
            int lineHeight = 10;

            for (DebugLine line : lines) {
                int w = tr.getWidth(line.text);
                if (w > maxWidth) maxWidth = w;
                totalHeight += lineHeight + line.extraYSpace;
            }

            int x = 10;
            int y = 10;
            int padding = 5;

            drawContext.fill(
                    x - padding,
                    y - padding,
                    x + maxWidth + padding,
                    y + totalHeight - (lineHeight - 8)+10,
                    0x88000000
            );

            int currentY = y;
            for (DebugLine line : lines) {
                drawContext.drawText(tr, line.text, x, currentY, line.color, true);
                currentY += lineHeight + line.extraYSpace;
            }
        });
    }
}