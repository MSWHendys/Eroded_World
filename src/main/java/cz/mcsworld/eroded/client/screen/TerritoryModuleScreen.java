package cz.mcsworld.eroded.client.screen;

import cz.mcsworld.eroded.client.data.ClientTerritoryModuleData;
import cz.mcsworld.eroded.network.*;
import cz.mcsworld.eroded.protection.TerritoryPermission;
import cz.mcsworld.eroded.screen.TerritoryModuleScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.tooltip.Tooltip;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TerritoryModuleScreen extends HandledScreen<TerritoryModuleScreenHandler> {

    private static final long SUGGESTION_REQUEST_INTERVAL_MS = 250L;

    private static final int MAIN_VISIBLE_ROWS = 4;
    private static final int TRUSTED_NAME_BUTTON_WIDTH = 104;

    private enum Page {
        MAIN,
        PLAYER_DETAIL
    }

    private TextFieldWidget playerNameField;
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
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title);

        this.backgroundWidth = 340;
        this.backgroundHeight = 260;
    }

    @Override
    protected void init() {
        super.init();

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        ClientPlayNetworking.send(
                new TerritoryModuleRequestPayload(this.handler.getAnchorPos())
        );

        rebuildWidgets();
    }

    private void rebuildWidgets() {
        BlockPos anchorPos = this.handler.getAnchorPos();

        String oldText = "";
        boolean wasFocused = false;

        if (this.playerNameField != null) {
            oldText = this.playerNameField.getText();
            wasFocused = this.playerNameField.isFocused();
        }

        this.clearChildren();
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
        this.playerNameField = new TextFieldWidget(
                this.textRenderer,
                this.x + 24,
                this.y + 104,
                258,
                20,
                Text.translatable("screen.eroded.territory_module.player_name")
        );

        this.playerNameField.setMaxLength(32);
        this.playerNameField.setPlaceholder(
                Text.translatable("screen.eroded.territory_module.player_name")
        );
        this.playerNameField.setText(oldText);
        this.playerNameField.setFocused(wasFocused);

        this.addDrawableChild(this.playerNameField);

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("+"),
                                button -> addPlayerFromField(anchorPos)
                        )
                        .dimensions(this.x + 288, this.y + 104, 28, 20)
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

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("screen.eroded.territory_module.detail.remove")
                                        .formatted(Formatting.RED),
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
                                    rebuildWidgets();
                                }
                        )
                        .dimensions(this.x + 24, this.y + 104, 100, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
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

                                    rebuildWidgets();
                                }
                        )
                        .dimensions(this.x + 231, this.y + 104, 85, 20)
                        .build()
        );

        if (selected == null) {
            return;
        }

        addPermissionButtons(anchorPos, selected);

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("screen.eroded.territory_module.detail.back"),
                                button -> {
                                    page = Page.MAIN;
                                    rebuildWidgets();
                                }
                        )
                        .dimensions(this.x + 110, this.y + 220, 120, 20)
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

        String name = this.playerNameField.getText().trim();

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

        this.playerNameField.setText("");
    }

    private void addSuggestionButtons(BlockPos anchorPos) {
        List<ClientTerritoryModuleData.Entry> suggestions =
                ClientTerritoryModuleData.suggestions();

        int max = Math.min(MAIN_VISIBLE_ROWS, suggestions.size());

        for (int i = 0; i < max; i++) {
            ClientTerritoryModuleData.Entry entry = suggestions.get(i);

            int buttonX = this.x + 24;
            int buttonY = this.y + 162 + (i * 21);

            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal(entry.name() + "  +"),
                                    button -> ClientPlayNetworking.send(
                                            new TerritoryTrustAddPayload(
                                                    anchorPos,
                                                    entry.name(),
                                                    false
                                            )
                                    )
                            )
                            .dimensions(buttonX, buttonY, 135, 19)
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

            int buttonY = this.y + 162 + (i * 21);

            this.addDrawableChild(
                    ButtonWidget.builder(
                                    trustedPlayerButtonText(entry, selected),
                                    button -> {
                                        selectedTrustedUuid = entry.uuid();
                                        page = Page.PLAYER_DETAIL;
                                        rebuildWidgets();
                                    }
                            )
                            .dimensions(this.x + 181, buttonY, TRUSTED_NAME_BUTTON_WIDTH, 19)
                            .build()
            );

            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.literal("X").formatted(Formatting.RED),
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

                                        rebuildWidgets();
                                    }
                            )
                            .dimensions(this.x + 293, buttonY, 24, 19)
                            .build()
            );
        }
    }

    private Text trustedPlayerButtonText(ClientTerritoryModuleData.Entry entry, boolean selected) {

        return Text.literal((selected ? "▶ " : "") + entry.name())
                .formatted(selected ? Formatting.YELLOW : Formatting.WHITE);
    }

    private void addTrustedScrollButtons() {
        List<ClientTerritoryModuleData.Entry> trusted =
                ClientTerritoryModuleData.trusted();

        if (trusted.size() <= MAIN_VISIBLE_ROWS) {
            return;
        }

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("▲"),
                                button -> {
                                    trustedScrollOffset--;
                                    clampTrustedScroll();
                                    rebuildWidgets();
                                }
                        )
                        .dimensions(this.x + 273, this.y + 140, 22, 18)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.literal("▼"),
                                button -> {
                                    trustedScrollOffset++;
                                    clampTrustedScroll();
                                    rebuildWidgets();
                                }
                        )
                        .dimensions(this.x + 295, this.y + 140, 22, 18)
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

            int buttonX = this.x + 24 + (col * 152);
            int buttonY = this.y + 145 + (row * 24);

            this.addDrawableChild(
                    ButtonWidget.builder(
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
                            .tooltip(Tooltip.of(Text.translatable(permissionTooltipTranslationKey(permission))))
                            .dimensions(buttonX, buttonY, 140, 21)
                            .build()
            );
        }
    }

    private Text permissionButtonText(TerritoryPermission permission, boolean enabled) {
        String prefix = enabled ? "✓ " : "✕ ";

        return Text.literal(prefix)
                .append(Text.translatable(permissionTranslationKey(permission)))
                .formatted(enabled ? Formatting.GREEN : Formatting.RED);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.lastDataVersion != ClientTerritoryModuleData.version()) {
            rebuildWidgets();
        }

        drawPanel(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);

        super.render(context, mouseX, mouseY, delta);

        renderModuleText(context);
        updateSuggestionRequest();

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderModuleText(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.textRenderer == null) {
            return;
        }

        BlockPos pos = this.handler.getAnchorPos();

        int left = this.x;
        int top = this.y;

        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.title"),
                left + 18,
                top + 12,
                0xFFFFFFFF
        );

        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.section.info"),
                left + 18,
                top + 34,
                0xFF55FFFF
        );

        if (!ClientTerritoryModuleData.isFor(pos)) {
            drawText(
                    context,
                    Text.translatable("screen.eroded.territory_module.loading"),
                    left + 24,
                    top + 52,
                    0xFFFFFF55
            );
            return;
        }

        if (page == Page.MAIN) {
            renderInfoText(context, left, top);
            renderMainText(context, left, top);
        } else {
            renderPlayerInfoText(context, left, top);
            renderPlayerDetailText(context, left, top);
        }
    }

    private void renderPlayerInfoText(DrawContext context, int left, int top) {
        ClientTerritoryModuleData.Entry selected = selectedTrustedEntry();

        if (selected == null) {
            drawText(
                    context,
                    Text.translatable("screen.eroded.territory_module.permissions.no_player"),
                    left + 24,
                    top + 52,
                    0xFF777777
            );
            return;
        }

        boolean selectedScope = selectedPlayerConnectedAreaMode();

        drawText(
                context,
                Text.translatable(
                        "screen.eroded.territory_module.permissions.player",
                        Text.literal(selected.name()).formatted(Formatting.GREEN)
                ),
                left + 24,
                top + 50,
                0xFFDDDDDD
        );

        drawText(
                context,
                Text.translatable(
                        "screen.eroded.territory_module.permissions.scope",
                        Text.translatable(
                                selectedScope
                                        ? "screen.eroded.territory_module.scope.text.connected"
                                        : "screen.eroded.territory_module.scope.text.anchor"
                        ).formatted(Formatting.DARK_GREEN)
                ),
                left + 24,
                top + 62,
                0xFFDDDDDD
        );
    }

    private void renderInfoText(DrawContext context, int left, int top) {
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

        BlockPos pos = this.handler.getAnchorPos();

        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.owner", owner),
                left + 24,
                top + 48,
                0xFFDDDDDD
        );

        drawText(
                context,
                Text.translatable(
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
                context,
                Text.translatable(
                        "screen.eroded.territory_module.area",
                        connectedWidth,
                        connectedDepth
                ),
                left + 190,
                top + 48,
                0xFFDDDDDD
        );

        drawText(
                context,
                Text.translatable(
                        "screen.eroded.territory_module.anchors",
                        connectedClaimCount
                ),
                left + 190,
                top + 60,
                0xFFAAAAAA
        );

        drawText(
                context,
                Text.translatable(
                        active
                                ? "screen.eroded.territory_module.status.active"
                                : "screen.eroded.territory_module.status.stabilizing"
                ),
                left + 190,
                top + 72,
                active ? 0xFF55FF55 : 0xFFFFFF55
        );
    }

    private void renderMainText(DrawContext context, int left, int top) {
        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.section.add_player"),
                left + 18,
                top + 88,
                0xFF55FFFF
        );

        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.section.available"),
                left + 24,
                top + 144,
                0xFFFFFFFF
        );

        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.section.trusted"),
                left + 181,
                top + 144,
                0xFFFFFFFF
        );

        if (ClientTerritoryModuleData.suggestions().isEmpty()) {
            drawText(
                    context,
                    Text.translatable("screen.eroded.territory_module.no_available"),
                    left + 24,
                    top + 164,
                    0xFF777777
            );
        }

        if (ClientTerritoryModuleData.trusted().isEmpty()) {
            drawText(
                    context,
                    Text.translatable("screen.eroded.territory_module.no_trusted"),
                    left + 181,
                    top + 164,
                    0xFF777777
            );
        }
    }

    private void renderPlayerDetailText(DrawContext context, int left, int top) {
        ClientTerritoryModuleData.Entry selected = selectedTrustedEntry();

        drawText(
                context,
                Text.translatable("screen.eroded.territory_module.section.permissions"),
                left + 18,
                top + 88,
                0xFF55FFFF
        );

        if (selected == null) {
            drawText(
                    context,
                    Text.translatable("screen.eroded.territory_module.permissions.no_player"),
                    left + 24,
                    top + 106,
                    0xFF777777
            );
        }
    }

    private void drawPanel(DrawContext context, int left, int top, int width, int height) {
        context.fill(
                left,
                top,
                left + width,
                top + height,
                0xEE0B0D10
        );

        context.drawBorder(
                left,
                top,
                width,
                height,
                0xFF6B6B6B
        );

        context.drawBorder(
                left + 3,
                top + 3,
                width - 6,
                height - 6,
                0xFF222832
        );

        context.fill(
                left + 14,
                top + 30,
                left + width - 14,
                top + 80,
                0xAA141922
        );

        context.fill(
                left + 14,
                top + 84,
                left + width - 14,
                top + 132,
                0xAA141922
        );

        context.fill(
                left + 14,
                top + 138,
                left + width - 14,
                top + height - 14,
                0xAA141922
        );

        if (page == Page.MAIN) {
            context.fill(
                    left + 170,
                    top + 142,
                    left + 171,
                    top + height - 18,
                    0xFF333A44
            );
        }
    }

    private void drawText(DrawContext context, Text text, int x, int y, int color) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.textRenderer == null) {
            return;
        }

        context.drawText(
                client.textRenderer,
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

        String query = this.playerNameField.getText().trim();

        if (query.equals(lastSentSuggestionQuery)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastSuggestionRequestAt < SUGGESTION_REQUEST_INTERVAL_MS) {
            return;
        }

        ClientPlayNetworking.send(
                new TerritorySuggestionRequestPayload(
                        this.handler.getAnchorPos(),
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
            rebuildWidgets();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void drawBackground(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY
    ) {
    }

    @Override
    protected void drawForeground(
            DrawContext context,
            int mouseX,
            int mouseY
    ) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.playerNameField != null && this.playerNameField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.playerNameField.setFocused(false);
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                addPlayerFromField(this.handler.getAnchorPos());
                return true;
            }

            if (this.playerNameField.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && page == Page.PLAYER_DETAIL) {
            page = Page.MAIN;
            rebuildWidgets();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.playerNameField != null && this.playerNameField.isFocused()) {
            return this.playerNameField.charTyped(chr, modifiers);
        }

        return super.charTyped(chr, modifiers);
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

    private Text scopeButtonText(boolean connectedMode) {
        return Text.translatable(
                connectedMode
                        ? "screen.eroded.territory_module.scope.connected"
                        : "screen.eroded.territory_module.scope.anchor"
        ).formatted(connectedMode ? Formatting.AQUA : Formatting.GOLD);
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