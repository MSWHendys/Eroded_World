package cz.mcsworld.eroded.world.loot;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

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

        if (!(entity instanceof Monster mob)) return;
        if (!mob.getTags().contains(MUTATED_TAG)) return;
        if (!(entity.level() instanceof ServerLevel world)) return;

        RandomSource random = world.getRandom();
        Vec3 pos = entity.position();

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

    private static void spawn(ServerLevel world, Vec3 pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(
                world,
                pos.x,
                pos.y + 0.5,
                pos.z,
                stack
        );
        world.addFreshEntity(item);
    }
}
