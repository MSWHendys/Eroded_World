package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.core.ErodedItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import java.util.Optional;

public final class TraumaEffectHandler {

    private TraumaEffectHandler() {}

    public static void register() {

        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {

                    if (alive) return;

                    newPlayer.addEffect(
                            new MobEffectInstance(
                                    MobEffects.SLOWNESS,
                                    20 * 60,
                                    0,
                                    true,
                                    false
                            )
                    );

                    ErodedDeathMemory mem =
                            ErodedDeathStorage.get(newPlayer.getUUID());

                    if (mem == null) return;

                    for (int i = 0; i < newPlayer.getInventory().getContainerSize(); i++) {
                        ItemStack s = newPlayer.getInventory().getItem(i);
                        if (!s.isEmpty() && s.is(ErodedItems.DEATH_COMPASS)) {
                            return;
                        }
                    }

                    ItemStack compass = new ItemStack(ErodedItems.DEATH_COMPASS);

                    BlockPos pos = mem.getDeathPos();

                    compass.set(
                            DataComponents.LODESTONE_TRACKER,
                            new LodestoneTracker(
                                    Optional.of(
                                            GlobalPos.of(
                                                    newPlayer.level().dimension(),
                                                    pos
                                            )
                                    ),
                                    false
                            )
                    );

                    newPlayer.getInventory().add(compass);
                }
        );
    }
}
