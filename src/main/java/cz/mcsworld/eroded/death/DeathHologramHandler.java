package cz.mcsworld.eroded.death;

import com.mojang.authlib.GameProfile;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DeathHologramHandler {


    private static final String TAG = "eroded_death_hologram";
    private static final String TAG_HEAD = "rotating_head";
    private static final String TAG_EXPIRY = "expiry_";
    private static final String TAG_NAME = "name_";
    private static final String TAG_BASE_Y = "baseY_";
    private static final String TAG_HOLOGRAM_ID = "hid_";

    private DeathHologramHandler() {}

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DeathHologramHandler::tick);
    }

    public static void spawn(ServerLevel world, BlockPos pos, GameProfile profile, int protectionSeconds, UUID hologramId) {
        long expiryEpochMs = System.currentTimeMillis() + (protectionSeconds * 1000L);
        String name = profile.name();
        String hidTag = TAG_HOLOGRAM_ID + hologramId;

        double baseX = pos.getX() + 0.5;
        double baseY = pos.getY();
        double baseZ = pos.getZ() + 0.5;


        double standBaseY = baseY + 0.3;
        ArmorStand stand = new ArmorStand(world, baseX, standBaseY, baseZ);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setSilent(true);
        stand.setInvulnerable(true);

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        stand.setItemSlot(EquipmentSlot.HEAD, head);

        stand.addTag(TAG);
        stand.addTag(TAG_HEAD);
        stand.addTag(hidTag);
        stand.addTag(TAG_EXPIRY + expiryEpochMs);
        stand.addTag(TAG_BASE_Y + standBaseY);

        world.addFreshEntity(stand);

        double textY = baseY + 2.6;
        Display.TextDisplay text = new Display.TextDisplay(EntityType.TEXT_DISPLAY, world);
        text.setPos(baseX, textY, baseZ);
        text.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        text.setBackgroundColor(0x60000000);
        text.setText(buildText(name, protectionSeconds));

        text.addTag(TAG);
        text.addTag(hidTag);
        text.addTag(TAG_EXPIRY + expiryEpochMs);
        text.addTag(TAG_NAME + name);

        world.addFreshEntity(text);
    }

    public static void tick(ServerLevel world) {
        boolean eachSecond = world.getServer().getTickCount() % 20 == 0;
        var cfg = DeathConfig.get().hologram;
        DeathChestState state = DeathChestState.get(world);

        if (eachSecond) {
            Set<UUID> activeHids = state.all().stream()
                    .map(DeathChestState.Entry::hologramId)
                    .collect(Collectors.toSet());

            for (Entity e : world.getAllEntities()) {
                if (e == null || e.isRemoved()) continue;

                Set<String> tags = e.getTags();
                if (tags.contains(TAG)) {
                    UUID hid = getHologramIdFromTags(e);
                    if (hid != null && !activeHids.contains(hid)) {
                        e.discard();
                    }
                }
            }
        }

        for (DeathChestState.Entry entry : state.all()) {
            BlockPos pos = entry.pos();
            UUID hid = entry.hologramId();
            String hidTag = TAG_HOLOGRAM_ID + hid;


            if (!world.getBlockState(pos).is(ErodedBlocks.DEATH_ENDER_CHEST)) {
                removeById(world, hid);
                continue;
            }

            if (!world.hasChunkAt(pos)) continue;

            AABB box = new AABB(pos).inflate(1.0, 4.0, 1.0);
            for (Entity e : world.getEntities(null, box)) {
                if (e == null || e.isRemoved()) continue;

                Set<String> tags = e.getTags();
                if (!tags.contains(hidTag)) continue;

                if (e instanceof ArmorStand stand && e.getTags().contains(TAG_HEAD)) {
                    float yaw = (world.getServer().getTickCount() * cfg.rotationSpeed) % 360f;
                    stand.setYRot(yaw);

                    double baseY = getBaseY(stand);
                    double bob = Math.sin(world.getServer().getTickCount() * cfg.bobbingSpeed) * cfg.bobbingAmplitude;
                    stand.setPos(stand.getX(), baseY + bob, stand.getZ());
                }

                if (eachSecond && e instanceof Display.TextDisplay text) {
                    long expiry = getExpiry(e);
                    if (expiry <= 0 || System.currentTimeMillis() >= expiry) {
                        continue;
                    }

                    int remainingSeconds = (int) ((expiry - System.currentTimeMillis()) / 1000);
                    text.setText(buildText(getName(e), remainingSeconds));
                }
            }
        }
    }

    public static void removeById(ServerLevel world, UUID hologramId) {
        String hidTag = TAG_HOLOGRAM_ID + hologramId;
        for (Entity e : world.getAllEntities()) {
            if (e == null || e.isRemoved()) continue;

            if (e.getTags().contains(hidTag)) {
                e.discard();
            }
        }
    }

    public static Component buildText(String name, int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int min = safeSeconds / 60;
        int sec = safeSeconds % 60;

        ChatFormatting timeColor = safeSeconds >= 60 ? ChatFormatting.GREEN :
                safeSeconds >= 30 ? ChatFormatting.YELLOW : ChatFormatting.RED;

        return Component.literal("\n ")
                .append(Component.literal(name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.translatable("eroded.death.hologram.line.protection"))
                .append(Component.literal(String.format(" %d:%02d", min, sec)).withStyle(timeColor, ChatFormatting.BOLD))
                .append(Component.translatable("eroded.death.hologram.line.hint"))
                .append(Component.literal("\n "));
    }

    private static UUID getHologramIdFromTags(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(TAG_HOLOGRAM_ID)) {
                try {
                    return UUID.fromString(tag.substring(TAG_HOLOGRAM_ID.length()));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static double getBaseY(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(TAG_BASE_Y)) {
                try { return Double.parseDouble(tag.substring(TAG_BASE_Y.length())); } catch (Exception ignored) {}
            }
        }
        return e.getY();
    }

    private static long getExpiry(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(TAG_EXPIRY)) {
                try { return Long.parseLong(tag.substring(TAG_EXPIRY.length())); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    private static String getName(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(TAG_NAME)) {
                return tag.substring(TAG_NAME.length());
            }
        }
        return "Unknown";
    }
}