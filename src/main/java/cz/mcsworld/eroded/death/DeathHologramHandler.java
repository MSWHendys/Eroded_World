package cz.mcsworld.eroded.death;

import com.mojang.authlib.GameProfile;
import cz.mcsworld.eroded.config.death.DeathConfig;
import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;


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

    public static void spawn(ServerWorld world, BlockPos pos, GameProfile profile, int protectionSeconds, UUID hologramId) {
        long expiryEpochMs = System.currentTimeMillis() + (protectionSeconds * 1000L);
        String name = profile.getName();
        String hidTag = TAG_HOLOGRAM_ID + hologramId;

        double baseX = pos.getX() + 0.5;
        double baseY = pos.getY();
        double baseZ = pos.getZ() + 0.5;


        double standBaseY = baseY + 0.3;
        ArmorStandEntity stand = new ArmorStandEntity(world, baseX, standBaseY, baseZ);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setSilent(true);
        stand.setInvulnerable(true);

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
        stand.equipStack(EquipmentSlot.HEAD, head);

        stand.addCommandTag(TAG);
        stand.addCommandTag(TAG_HEAD);
        stand.addCommandTag(hidTag);
        stand.addCommandTag(TAG_EXPIRY + expiryEpochMs);
        stand.addCommandTag(TAG_BASE_Y + standBaseY);

        world.spawnEntity(stand);

        double textY = baseY + 2.6;
        DisplayEntity.TextDisplayEntity text = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
        text.setPosition(baseX, textY, baseZ);
        text.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
        text.setBackground(0x60000000);
        text.setText(buildText(name, protectionSeconds));

        text.addCommandTag(TAG);
        text.addCommandTag(hidTag);
        text.addCommandTag(TAG_EXPIRY + expiryEpochMs);
        text.addCommandTag(TAG_NAME + name);

        world.spawnEntity(text);
    }

    public static void tick(ServerWorld world) {
        boolean eachSecond = world.getServer().getTicks() % 20 == 0;
        var cfg = DeathConfig.get().hologram;
        DeathChestState state = DeathChestState.get(world);

        if (eachSecond) {
            Set<UUID> activeHids = state.all().stream()
                    .map(DeathChestState.Entry::hologramId)
                    .collect(Collectors.toSet());

            for (Entity e : world.iterateEntities()) {
                if (e.getCommandTags().contains(TAG)) {
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


            if (!world.getBlockState(pos).isOf(ErodedBlocks.DEATH_ENDER_CHEST)) {
                removeById(world, hid);
                continue;
            }

            if (!world.isChunkLoaded(pos)) continue;

            Box box = new Box(pos).expand(1.0, 4.0, 1.0);
            for (Entity e : world.getOtherEntities(null, box)) {
                if (!e.getCommandTags().contains(hidTag)) continue;

                if (e instanceof ArmorStandEntity stand && e.getCommandTags().contains(TAG_HEAD)) {
                    float yaw = (world.getServer().getTicks() * cfg.rotationSpeed) % 360f;
                    stand.setYaw(yaw);

                    double baseY = getBaseY(stand);
                    double bob = Math.sin(world.getServer().getTicks() * cfg.bobbingSpeed) * cfg.bobbingAmplitude;
                    stand.setPosition(stand.getX(), baseY + bob, stand.getZ());
                }

                if (eachSecond && e instanceof DisplayEntity.TextDisplayEntity text) {
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

    public static void removeById(ServerWorld world, UUID hologramId) {
        String hidTag = TAG_HOLOGRAM_ID + hologramId;
        for (Entity e : world.iterateEntities()) {
            if (e.getCommandTags().contains(hidTag)) {
                e.discard();
            }
        }
    }

    public static Text buildText(String name, int seconds) {
        int safeSeconds = Math.max(0, seconds);
        int min = safeSeconds / 60;
        int sec = safeSeconds % 60;

        Formatting timeColor = safeSeconds >= 60 ? Formatting.GREEN :
                safeSeconds >= 30 ? Formatting.YELLOW : Formatting.RED;

        return Text.literal("\n ")
                .append(Text.literal(name).formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.translatable("eroded.death.hologram.line.protection"))
                .append(Text.literal(String.format(" %d:%02d", min, sec)).formatted(timeColor, Formatting.BOLD))
                .append(Text.translatable("eroded.death.hologram.line.hint"))
                .append(Text.literal("\n "));
    }

    private static UUID getHologramIdFromTags(Entity e) {
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith(TAG_HOLOGRAM_ID)) {
                try {
                    return UUID.fromString(tag.substring(TAG_HOLOGRAM_ID.length()));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static double getBaseY(Entity e) {
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith(TAG_BASE_Y)) {
                try { return Double.parseDouble(tag.substring(TAG_BASE_Y.length())); } catch (Exception ignored) {}
            }
        }
        return e.getY();
    }

    private static long getExpiry(Entity e) {
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith(TAG_EXPIRY)) {
                try { return Long.parseLong(tag.substring(TAG_EXPIRY.length())); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    private static String getName(Entity e) {
        for (String tag : e.getCommandTags()) {
            if (tag.startsWith(TAG_NAME)) {
                return tag.substring(TAG_NAME.length());
            }
        }
        return "Unknown";
    }
}