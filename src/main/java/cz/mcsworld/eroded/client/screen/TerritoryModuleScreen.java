package cz.mcsworld.eroded.client.screen;

import cz.mcsworld.eroded.client.data.ClientTerritoryModuleData;
import cz.mcsworld.eroded.network.*;
import cz.mcsworld.eroded.protection.TerritoryPermission;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerritoryModuleScreen extends AbstractContainerScreen<@NotNull TerritoryModuleScreenHandler> {

    private static final long SUGGESTION_REQUEST_INTERVAL_MS = 250L;

    private static final int MAIN_VISIBLE_ROWS = 4;
    private static final int TRUSTED_NAME_BUTTON_WIDTH = 104;

    private enum Page {
        MAIN,
        PLAYER_DETAIL
    }

    private EditBox playerNameField;
    private int lastDataVersion = -1;
    private int lastScopeSyncVersion = -1;

    private String lastSentSuggestionQuery = null;
    private long lastSuggestionRequestAt = 0L;

    private final Map<UUID, Boolean> playerScopeModes = new HashMap<>();

    private UUID selectedTrustedUuid = null;
    private int trustedScrollOffset = 0;
    private Page page = Page.MAIN;

    public TerritoryModuleScreen(
            TerritoryModuleScreenHandler handler,
            Inventory inventory,
            Component title
    ) {
        super(handler, inventory, title, 340, 260);
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        ClientPlayNetworking.send(
                new TerritoryModuleRequestPayload(this.menu.getAnchorPos())
        );

        rebuildErodedWidgets();
    }

    private void rebuildErodedWidgets() {
        BlockPos anchorPos = this.menu.getAnchorPos();

        String oldText = "";
        boolean wasFocused = false;

        if (this.playerNameField != null) {
            oldText = this.playerNameField.getValue();
            wasFocused = this.playerNameField.isFocused();
        }

        this.clearWidgets();
        this.playerNameField = null;

        if (ClientTerritoryModuleData.isFor(anchorPos)) {
            ensureSelectedTrustedStillExists();
        }

        if (page == Page.PLAYER_DETAIL && selectedTrustedEntry() == null) {
            page = Page.MAIN;
        }

        if (page == Page.MAIN) {
            rebuildMainPage(anchorPos, oldText, wasFocused);
        } else {
            rebuildPlayerDetailPage(anchorPos);
        }

        this.lastDataVersion = ClientTerritoryModuleData.version();
    }

    private void rebuildMainPage(BlockPos anchorPos, String oldText, boolean wasFocused) {
        this.playerNameField = new EditBox(
                this.font,
                this.leftPos + 24,
                this.topPos + 104,
                258,
                20,
                Component.translatable("screen.eroded.territory_module.player_name")
        );

        this.playerNameField.setMaxLength(32);
        this.playerNameField.setHint(
                Component.translatable("screen.eroded.territory_module.player_name")
        );
        this.playerNameField.setValue(oldText);
        this.playerNameField.setFocused(wasFocused);

        this.addRenderableWidget(this.playerNameField);

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("+"),
                                button -> addPlayerFromField(anchorPos)
                        )
                        .bounds(this.leftPos + 288, this.topPos + 104, 28, 20)
                        .build()
        );

        if (ClientTerritoryModuleData.isFor(anchorPos)) {
            addSuggestionButtons(anchorPos);
            addTrustedButtons(anchorPos);
            addTrustedScrollButtons();
        }
    }

    private void rebuildPlayerDetailPage(BlockPos anchorPos) {
        ClientTerritoryModuleData.Entry selected = selectedTrustedEntry();

        this.addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.eroded.territory_module.detail.remove")
                                        .withStyle(ChatFormatting.RED),
                                button -> {
                                    if (selected == null) {
                                        return;
                                    }

                                    ClientPlayNetworking.send(
                                            new TerritoryTrustRemovePayload(
                                                    anchorPos,
                                                    selected.uuid().toString(),
                                                    selected.name(),
                                                    selectedPlayerConnectedAreaMode()
                                            )
                                    );

                                    playerScopeModes.remove(selected.uuid());
                                    selectedTrustedUuid = null;
                                    page = Page.MAIN;
                                    rebuildErodedWidgets();
                                }
                        )
                        .bounds(this.leftPos + 24, this.topPos + 104, 100, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                scopeButtonText(selectedPlayerConnectedAreaMode()),
                                button -> {
                                    if (selectedTrustedUuid != null) {
                                        boolean newValue = !selectedPlayerConnectedAreaMode();

                                        playerScopeModes.put(selectedTrustedUuid, newValue);

                                        ClientPlayNetworking.send(
                                                new TerritoryScopeUpdatePayload(
                                                        anchorPos,
                                                        selectedTrustedUuid.toString(),
                                                        newValue
                                                )
                                        );
                                    }

                                    rebuildErodedWidgets();
                                }
                        )
                        .bounds(this.leftPos + 231, this.topPos + 104, 85, 20)
                        .build()
        );

        if (selected == null) {
            return;
        }

        addPermissionButtons(anchorPos, selected);

        this.addRenderableWidget(
                Button.builder(
                                Component.translatable("screen.eroded.territory_module.detail.back"),
                                button -> {
                                    page = Page.MAIN;
                                    rebuildErodedWidgets();
                                }
                        )
                        .bounds(this.leftPos + 110, this.topPos + 220, 120, 20)
                        .build()
        );
    }

    private void ensureSelectedTrustedStillExists() {
        List<ClientTerritoryModuleData.Entry> trusted =
                ClientTerritoryModuleData.trusted();

        if (trusted.isEmpty()) {
            selectedTrustedUuid = null;
            trustedScrollOffset = 0;
            playerScopeModes.clear();
            lastScopeSyncVersion = ClientTerritoryModuleData.version();
            return;
        }

        clampTrustedScroll();
        syncPlayerScopeModesFromDataIfNeeded();

        if (selectedTrustedUuid != null) {
            for (ClientTerritoryModuleData.Entry entry : trusted) {
                if (entry.uuid().equals(selectedTrustedUuid)) {
                    return;
                }
            }
        }

        selectedTrustedUuid = trusted.get(
                Math.min(trustedScrollOffset, trusted.size() - 1)
        ).uuid();
    }

    private void clampTrustedScroll() {
        int size = ClientTerritoryModuleData.trusted().size();
        int maxOffset = Math.max(0, size - MAIN_VISIBLE_ROWS);

        if (trustedScrollOffset < 0) {
            trustedScrollOffset = 0;
        }

        if (trustedScrollOffset > maxOffset) {
            trustedScrollOffset = maxOffset;
        }
    }

    private ClientTerritoryModuleData.Entry selectedTrustedEntry() {
        if (selectedTrustedUuid == null) {
            return null;
        }

        for (ClientTerritoryModuleData.Entry entry : ClientTerritoryModuleData.trusted()) {
            if (entry.uuid().equals(selectedTrustedUuid)) {
                return entry;
            }
        }

        return null;
    }

    private void addPlayerFromField(BlockPos anchorPos) {
        if (this.playerNameField == null) {
            return;
        }

        String name = this.playerNameField.getValue().trim();

        if (name.isBlank()) {
            return;
        }

        ClientPlayNetworking.send(
                new TerritoryTrustAddPayload(
                        anchorPos,
                        name,
                        false
                )
        );

        this.playerNameField.setValue("");
    }

    private void addSuggestionButtons(BlockPos anchorPos) {
        List<ClientTerritoryModuleData.Entry> suggestions =
                ClientTerritoryModuleData.suggestions();

        int max = Math.min(MAIN_VISIBLE_ROWS, suggestions.size());

        for (int i = 0; i < max; i++) {
            ClientTerritoryModuleData.Entry entry = suggestions.get(i);

            int buttonX = this.leftPos + 24;
            int buttonY = this.topPos + 162 + (i * 21);

            this.addRenderableWidget(
                    Button.builder(
                                    Component.literal(entry.name() + "  +"),
                                    button -> ClientPlayNetworking.send(
                                            new TerritoryTrustAddPayload(
                                                    anchorPos,
                                                    entry.name(),
                                                    false
                                            )
                                    )
                            )
                            .bounds(buttonX, buttonY, 135, 19)
                            .build()
            );
        }
    }

    private void addTrustedButtons(BlockPos anchorPos) {
        List<ClientTerritoryModuleData.Entry> trusted =
                ClientTerritoryModuleData.trusted();

        clampTrustedScroll();

        int visible = Math.min(MAIN_VISIBLE_ROWS, trusted.size() - trustedScrollOffset);

        for (int i = 0; i < visible; i++) {
            ClientTerritoryModuleData.Entry entry = trusted.get(trustedScrollOffset + i);

            boolean selected = selectedTrustedUuid != null
                    && selectedTrustedUuid.equals(entry.uuid());

            int buttonY = this.topPos + 162 + (i * 21);

            this.addRenderableWidget(
                    Button.builder(
                                    trustedPlayerButtonText(entry, selected),
                                    button -> {
                                        selectedTrustedUuid = entry.uuid();
                                        page = Page.PLAYER_DETAIL;
                                        rebuildErodedWidgets();
                                    }
                            )
                            .bounds(this.leftPos + 181, buttonY, TRUSTED_NAME_BUTTON_WIDTH, 19)
                            .build()
            );

            this.addRenderableWidget(
                    Button.builder(
                                    Component.literal("X").withStyle(ChatFormatting.RED),
                                    button -> {
                                        ClientPlayNetworking.send(
                                                new TerritoryTrustRemovePayload(
                                                        anchorPos,
                                                        entry.uuid().toString(),
                                                        entry.name(),
                                                        false
                                                )
                                        );

                                        playerScopeModes.remove(entry.uuid());

                                        if (entry.uuid().equals(selectedTrustedUuid)) {
                                            selectedTrustedUuid = null;
                                        }

                                        rebuildErodedWidgets();
                                    }
                            )
                            .bounds(this.leftPos + 293, buttonY, 24, 19)
                            .build()
            );
        }
    }

    private Component trustedPlayerButtonText(ClientTerritoryModuleData.Entry entry, boolean selected) {
        return Component.literal((selected ? "▶ " : "") + entry.name())
                .withStyle(selected ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
    }

    private void addTrustedScrollButtons() {
        List<ClientTerritoryModuleData.Entry> trusted =
                ClientTerritoryModuleData.trusted();

        if (trusted.size() <= MAIN_VISIBLE_ROWS) {
            return;
        }

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("▲"),
                                button -> {
                                    trustedScrollOffset--;
                                    clampTrustedScroll();
                                    rebuildErodedWidgets();
                                }
                        )
                        .bounds(this.leftPos + 273, this.topPos + 140, 22, 18)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(
                                Component.literal("▼"),
                                button -> {
                                    trustedScrollOffset++;
                                    clampTrustedScroll();
                                    rebuildErodedWidgets();
                                }
                        )
                        .bounds(this.leftPos + 295, this.topPos + 140, 22, 18)
                        .build()
        );
    }

    private void addPermissionButtons(BlockPos anchorPos, ClientTerritoryModuleData.Entry selected) {
        TerritoryPermission[] permissions = new TerritoryPermission[]{
                TerritoryPermission.BUILD,
                TerritoryPermission.BREAK,
                TerritoryPermission.CONTAINERS,
                TerritoryPermission.REDSTONE,
                TerritoryPermission.FIRE,
                TerritoryPermission.ENTITIES
        };

        for (int i = 0; i < permissions.length; i++) {
            TerritoryPermission permission = permissions[i];

            boolean enabled = selected.hasPermission(permission);

            int col = i % 2;
            int row = i / 2;

            int buttonX = this.leftPos + 24 + (col * 152);
            int buttonY = this.topPos + 145 + (row * 24);

            this.addRenderableWidget(
                    Button.builder(
                                    permissionButtonText(permission, enabled),
                                    button -> ClientPlayNetworking.send(
                                            new TerritoryPermissionUpdatePayload(
                                                    anchorPos,
                                                    selected.uuid().toString(),
                                                    permission.name(),
                                                    !enabled,
                                                    connectedAreaModeFor(selected.uuid())
                                            )
                                    )
                            )
                            .tooltip(Tooltip.create(Component.translatable(permissionTooltipTranslationKey(permission))))
                            .bounds(buttonX, buttonY, 140, 21)
                            .build()
            );
        }
    }

    private Component permissionButtonText(TerritoryPermission permission, boolean enabled) {
        String prefix = enabled ? "✓ " : "✕ ";

        return Component.literal(prefix)
                .append(Component.translatable(permissionTranslationKey(permission)))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private String permissionTranslationKey(TerritoryPermission permission) {
        return switch (permission) {
            case BUILD -> "screen.eroded.territory_module.permission.build";
            case BREAK -> "screen.eroded.territory_module.permission.break";
            case CONTAINERS -> "screen.eroded.territory_module.permission.containers";
            case REDSTONE -> "screen.eroded.territory_module.permission.redstone";
            case FIRE -> "screen.eroded.territory_module.permission.fire";
            case ENTITIES -> "screen.eroded.territory_module.permission.entities";
        };
    }

    private String permissionTooltipTranslationKey(TerritoryPermission permission) {
        return switch (permission) {
            case BUILD -> "screen.eroded.territory_module.permission.build.tooltip";
            case BREAK -> "screen.eroded.territory_module.permission.break.tooltip";
            case CONTAINERS -> "screen.eroded.territory_module.permission.containers.tooltip";
            case REDSTONE -> "screen.eroded.territory_module.permission.redstone.tooltip";
            case FIRE -> "screen.eroded.territory_module.permission.fire.tooltip";
            case ENTITIES -> "screen.eroded.territory_module.permission.entities.tooltip";
        };
    }

    @Override
    public void extractRenderState(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        if (this.lastDataVersion != ClientTerritoryModuleData.version()) {
            rebuildErodedWidgets();
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        renderModuleText(graphics);
        updateSuggestionRequest();
    }

    @Override
    public void extractBackground(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta
    ) {
        drawPanel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void extractLabels(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
        // Vlastní text kreslíme v extractRenderState(), aby se souřadnice nezdvojily přes leftPos/topPos.
    }

    private void renderModuleText(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();

        if (client == null || client.font == null) {
            return;
        }

        BlockPos pos = this.menu.getAnchorPos();

        int left = this.leftPos;
        int top = this.topPos;

        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.title"),
                left + 18,
                top + 12,
                0xFFFFFFFF
        );

        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.section.info"),
                left + 18,
                top + 34,
                0xFF55FFFF
        );

        if (!ClientTerritoryModuleData.isFor(pos)) {
            drawText(
                    graphics,
                    Component.translatable("screen.eroded.territory_module.loading"),
                    left + 24,
                    top + 52,
                    0xFFFFFF55
            );
            return;
        }

        if (page == Page.MAIN) {
            renderInfoText(graphics, left, top);
            renderMainText(graphics, left, top);
        } else {
            renderPlayerInfoText(graphics, left, top);
            renderPlayerDetailText(graphics, left, top);
        }
    }

    private void renderPlayerInfoText(GuiGraphicsExtractor graphics, int left, int top) {
        ClientTerritoryModuleData.Entry selected = selectedTrustedEntry();

        if (selected == null) {
            drawText(
                    graphics,
                    Component.translatable("screen.eroded.territory_module.permissions.no_player"),
                    left + 24,
                    top + 52,
                    0xFF777777
            );
            return;
        }

        boolean selectedScope = selectedPlayerConnectedAreaMode();

        drawText(
                graphics,
                Component.translatable(
                        "screen.eroded.territory_module.permissions.player",
                        Component.literal(selected.name()).withStyle(ChatFormatting.GREEN)
                ),
                left + 24,
                top + 50,
                0xFFDDDDDD
        );

        drawText(
                graphics,
                Component.translatable(
                        "screen.eroded.territory_module.permissions.scope",
                        Component.translatable(
                                selectedScope
                                        ? "screen.eroded.territory_module.scope.text.connected"
                                        : "screen.eroded.territory_module.scope.text.anchor"
                        ).withStyle(ChatFormatting.DARK_GREEN)
                ),
                left + 24,
                top + 62,
                0xFFDDDDDD
        );
    }

    private void renderInfoText(GuiGraphicsExtractor graphics, int left, int top) {
        String owner = ClientTerritoryModuleData.ownerName();
        boolean active = ClientTerritoryModuleData.active();

        int connectedWidth = ClientTerritoryModuleData.connectedWidth();
        int connectedDepth = ClientTerritoryModuleData.connectedDepth();
        int connectedClaimCount = ClientTerritoryModuleData.connectedClaimCount();

        if (connectedWidth <= 0 || connectedDepth <= 0) {
            int radius = ClientTerritoryModuleData.radius();
            int size = (radius * 2) + 1;

            connectedWidth = size;
            connectedDepth = size;
        }

        if (connectedClaimCount <= 0) {
            connectedClaimCount = 1;
        }

        BlockPos pos = this.menu.getAnchorPos();

        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.owner", owner),
                left + 24,
                top + 48,
                0xFFDDDDDD
        );

        drawText(
                graphics,
                Component.translatable(
                        "screen.eroded.territory_module.anchor",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                ),
                left + 24,
                top + 60,
                0xFFAAAAAA
        );

        drawText(
                graphics,
                Component.translatable(
                        "screen.eroded.territory_module.area",
                        connectedWidth,
                        connectedDepth
                ),
                left + 190,
                top + 48,
                0xFFDDDDDD
        );

        drawText(
                graphics,
                Component.translatable(
                        "screen.eroded.territory_module.anchors",
                        connectedClaimCount
                ),
                left + 190,
                top + 60,
                0xFFAAAAAA
        );

        drawText(
                graphics,
                Component.translatable(
                        active
                                ? "screen.eroded.territory_module.status.active"
                                : "screen.eroded.territory_module.status.stabilizing"
                ),
                left + 190,
                top + 72,
                active ? 0xFF55FF55 : 0xFFFFFF55
        );
    }

    private void renderMainText(GuiGraphicsExtractor graphics, int left, int top) {
        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.section.add_player"),
                left + 18,
                top + 88,
                0xFF55FFFF
        );

        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.section.available"),
                left + 24,
                top + 144,
                0xFFFFFFFF
        );

        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.section.trusted"),
                left + 181,
                top + 144,
                0xFFFFFFFF
        );

        if (ClientTerritoryModuleData.suggestions().isEmpty()) {
            drawText(
                    graphics,
                    Component.translatable("screen.eroded.territory_module.no_available"),
                    left + 24,
                    top + 164,
                    0xFF777777
            );
        }

        if (ClientTerritoryModuleData.trusted().isEmpty()) {
            drawText(
                    graphics,
                    Component.translatable("screen.eroded.territory_module.no_trusted"),
                    left + 181,
                    top + 164,
                    0xFF777777
            );
        }
    }

    private void renderPlayerDetailText(GuiGraphicsExtractor graphics, int left, int top) {
        ClientTerritoryModuleData.Entry selected = selectedTrustedEntry();

        drawText(
                graphics,
                Component.translatable("screen.eroded.territory_module.section.permissions"),
                left + 18,
                top + 88,
                0xFF55FFFF
        );

        if (selected == null) {
            drawText(
                    graphics,
                    Component.translatable("screen.eroded.territory_module.permissions.no_player"),
                    left + 24,
                    top + 106,
                    0xFF777777
            );
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        graphics.fill(
                left,
                top,
                left + width,
                top + height,
                0xEE0B0D10
        );

        graphics.outline(
                left,
                top,
                width,
                height,
                0xFF6B6B6B
        );

        graphics.outline(
                left + 3,
                top + 3,
                width - 6,
                height - 6,
                0xFF222832
        );

        graphics.fill(
                left + 14,
                top + 30,
                left + width - 14,
                top + 80,
                0xAA141922
        );

        graphics.fill(
                left + 14,
                top + 84,
                left + width - 14,
                top + 132,
                0xAA141922
        );

        graphics.fill(
                left + 14,
                top + 138,
                left + width - 14,
                top + height - 14,
                0xAA141922
        );

        if (page == Page.MAIN) {
            graphics.fill(
                    left + 170,
                    top + 142,
                    left + 171,
                    top + height - 18,
                    0xFF333A44
            );
        }
    }

    private void drawText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        Minecraft client = Minecraft.getInstance();

        if (client == null || client.font == null) {
            return;
        }

        graphics.text(
                client.font,
                text,
                x,
                y,
                color,
                true
        );
    }

    private void updateSuggestionRequest() {
        if (page != Page.MAIN) {
            return;
        }

        if (this.playerNameField == null) {
            return;
        }

        String query = this.playerNameField.getValue().trim();

        if (query.equals(lastSentSuggestionQuery)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastSuggestionRequestAt < SUGGESTION_REQUEST_INTERVAL_MS) {
            return;
        }

        ClientPlayNetworking.send(
                new TerritorySuggestionRequestPayload(
                        this.menu.getAnchorPos(),
                        query
                )
        );

        lastSentSuggestionQuery = query;
        lastSuggestionRequestAt = now;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (page == Page.MAIN && ClientTerritoryModuleData.trusted().size() > MAIN_VISIBLE_ROWS) {
            if (verticalAmount < 0) {
                trustedScrollOffset++;
            } else if (verticalAmount > 0) {
                trustedScrollOffset--;
            }

            clampTrustedScroll();
            rebuildErodedWidgets();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();

        if (this.playerNameField != null && this.playerNameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.playerNameField.setFocused(false);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                addPlayerFromField(this.menu.getAnchorPos());
                return true;
            }

            if (this.playerNameField.keyPressed(input)) {
                return true;
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && page == Page.PLAYER_DETAIL) {
            page = Page.MAIN;
            rebuildErodedWidgets();
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (this.playerNameField != null && this.playerNameField.isFocused()) {
            return this.playerNameField.charTyped(input);
        }

        return super.charTyped(input);
    }

    private boolean selectedPlayerConnectedAreaMode() {
        if (selectedTrustedUuid == null) {
            return false;
        }

        return playerScopeModes.getOrDefault(selectedTrustedUuid, false);
    }

    private boolean connectedAreaModeFor(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        return playerScopeModes.getOrDefault(uuid, false);
    }

    private Component scopeButtonText(boolean connectedMode) {
        return Component.translatable(
                connectedMode
                        ? "screen.eroded.territory_module.scope.connected"
                        : "screen.eroded.territory_module.scope.anchor"
        ).withStyle(connectedMode ? ChatFormatting.AQUA : ChatFormatting.GOLD);
    }

    private void syncPlayerScopeModesFromDataIfNeeded() {
        if (lastScopeSyncVersion == ClientTerritoryModuleData.version()) {
            return;
        }

        playerScopeModes.keySet().removeIf(
                uuid -> ClientTerritoryModuleData.trustedByUuid(uuid) == null
        );

        for (ClientTerritoryModuleData.Entry entry : ClientTerritoryModuleData.trusted()) {
            playerScopeModes.put(entry.uuid(), entry.connectedScopeMode());
        }

        lastScopeSyncVersion = ClientTerritoryModuleData.version();
    }
}