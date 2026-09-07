package cz.mcsworld.eroded.client.debug;

import cz.mcsworld.eroded.client.data.ClientEnergyData;
import cz.mcsworld.eroded.client.data.ClientSkillData;
import cz.mcsworld.eroded.config.crafting.CraftingConfig;
import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.world.territory.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class TerritoryDebugOverlay {

    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("eroded", "territory_debug_overlay");

    private TerritoryDebugOverlay() {}

    private record DebugLine(Component text, int color, int extraYSpace) {}

    public static void register() {
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                ID,
                TerritoryDebugOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null || !ErodedDebug.territoryOverlay) {
            return;
        }

        if (client.getSingleplayerServer() == null) {
            return;
        }

        ServerLevel world = client.getSingleplayerServer().getLevel(client.level.dimension());
        if (world == null) {
            return;
        }

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

        int chunkX = SectionPos.blockToSectionCoord(player.getBlockX());
        int chunkZ = SectionPos.blockToSectionCoord(player.getBlockZ());

        TerritoryWorldState state = TerritoryWorldState.getIfPresent(world);
        TerritoryCell cell = state == null
                ? new TerritoryCell()
                : state.copyCellOrEmpty(TerritoryCellKey.fromChunk(chunkX, chunkZ));

        float threat = TerritoryThreatResolver.computeThreat(
                cell,
                world.getGameTime()
        );

        String predictedKey = (avgCg < craftingCfg.qualityPoorToStandard)
                ? "eroded.crafting.quality.poor"
                : (avgCg < craftingCfg.qualityStandardToExcellent)
                ? "eroded.crafting.quality.standard"
                : "eroded.crafting.quality.excellent";

        List<DebugLine> lines = new ArrayList<>();

        lines.add(new DebugLine(Component.translatable("eroded.debug.cell.title"), 0xFF55FF55, 0));
        lines.add(new DebugLine(Component.translatable("eroded.debug.cell.mining_blocks").append(String.valueOf(cell.getMiningScore())), 0xFFFFFFFF, 0));
        lines.add(new DebugLine(Component.translatable("eroded.debug.cell.mining").append(String.valueOf(cell.getMiningRaw())), 0xFFFFFFFF, 0));
        lines.add(new DebugLine(Component.translatable("eroded.debug.cell.pollution").append(String.valueOf(cell.getPollutionRaw())), 0xFFFF5555, 0));
        lines.add(new DebugLine(Component.translatable("eroded.debug.cell.forest").append(String.valueOf(cell.getForestationRaw())), 0xFF55FF55, 0));
        lines.add(new DebugLine(Component.translatable("eroded.debug.cell.threat").append(String.format("%.2f", threat)), 0xFFFFAA00, 5));

        lines.add(new DebugLine(Component.literal(Component.translatable("eroded.config.title.energy").getString().toUpperCase() + ":"), 0xFF55FF55, 0));
        lines.add(new DebugLine(Component.translatable("eroded.config.title.energy").append(": " + energy + " / " + maxEnergy), 0xFFFFFF00, 0));
        lines.add(new DebugLine(Component.translatable("eroded.debug.energy.state").append(": " + energyState), 0xFFFFAA00, 5));

        lines.add(new DebugLine(Component.literal("CG:"), 0xFF55FF55, 0));
        lines.add(new DebugLine(Component.translatable("eroded.skill.woodworking").append(": " + String.format("%.2f", wood)), 0xFFFFFFFF, 0));
        lines.add(new DebugLine(Component.translatable("eroded.skill.smelting").append(": " + String.format("%.2f", smelt)), 0xFFFFFFFF, 5));

        lines.add(new DebugLine(Component.translatable("eroded.text.quality.line"), 0xFF55FF55, 0));
        lines.add(new DebugLine(Component.translatable("eroded.crafting.quality.poor.standard").append(" " + craftingCfg.qualityPoorToStandard + " cg"), 0xFFFFFFFF, 0));
        lines.add(new DebugLine(Component.translatable("eroded.crafting.quality.standard.excellent").append(" " + craftingCfg.qualityStandardToExcellent + " cg"), 0xFFFFFFFF, 0));
        lines.add(new DebugLine(Component.literal("CG avg: " + String.format("%.2f", avgCg)), 0xFF55FFFF, 0));
        lines.add(new DebugLine(Component.translatable("eroded.crafting.predicted").append(Component.translatable(predictedKey)), 0xFFFFFF55, 0));

        Font font = client.font;

        int maxWidth = 0;
        int totalHeight = 0;
        int lineHeight = 10;

        for (DebugLine line : lines) {
            int width = font.width(line.text);
            if (width > maxWidth) {
                maxWidth = width;
            }

            totalHeight += lineHeight + line.extraYSpace;
        }

        int x = 10;
        int y = 10;
        int padding = 5;

        graphics.fill(
                x - padding,
                y - padding,
                x + maxWidth + padding,
                y + totalHeight + padding,
                0x88000000
        );

        int currentY = y;

        for (DebugLine line : lines) {
            graphics.text(
                    font,
                    line.text,
                    x,
                    currentY,
                    line.color,
                    true
            );

            currentY += lineHeight + line.extraYSpace;
        }
    }
}