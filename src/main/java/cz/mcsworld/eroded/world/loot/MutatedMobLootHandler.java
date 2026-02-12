package cz.mcsworld.eroded.world.loot;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

public final class MutatedMobLootHandler {

    public static final String MUTATED_TAG = "eroded_mutated";

    private MutatedMobLootHandler() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(
                MutatedMobLootHandler::onDeath
        );
    }

    private static void onDeath(
            LivingEntity entity,
            DamageSource source
    ) {

        if (!(entity instanceof HostileEntity mob)) return;
        if (!mob.getCommandTags().contains(MUTATED_TAG)) return;
        if (!(entity.getWorld() instanceof ServerWorld world)) return;

        Random random = world.getRandom();
        Vec3d pos = entity.getPos();

        if (random.nextFloat() < 0.75f) {
            spawn(world, pos, new ItemStack(Items.ROTTEN_FLESH));
        }

        if (random.nextFloat() < 0.25f) {
            spawn(world, pos, new ItemStack(Items.AMETHYST_SHARD));
        }

        if (random.nextFloat() < 0.10f) {
            spawn(world, pos, new ItemStack(Items.ECHO_SHARD));
        }
    }

    private static void spawn(ServerWorld world, Vec3d pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(
                world,
                pos.x,
                pos.y + 0.5,
                pos.z,
                stack
        );
        world.spawnEntity(item);
    }
}
