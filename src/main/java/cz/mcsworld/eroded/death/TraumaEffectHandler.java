package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.Optional;

public final class TraumaEffectHandler {

    private TraumaEffectHandler() {}

    public static void register() {

        ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> {

                    if (alive) return;

                    SkillData data = SkillManager.get(newPlayer);
                    data.setEnergyAfterDeath(0.35f);

                    newPlayer.addStatusEffect(
                            new StatusEffectInstance(
                                    StatusEffects.SLOWNESS,
                                    20 * 60,
                                    0,
                                    true,
                                    false
                            )
                    );

                    ErodedDeathMemory mem =
                            ErodedDeathStorage.get(newPlayer.getUuid());

                    if (mem == null) return;

                    for (int i = 0; i < newPlayer.getInventory().size(); i++) {
                        ItemStack s = newPlayer.getInventory().getStack(i);
                        if (!s.isEmpty() && s.isOf(ErodedItems.DEATH_COMPASS)) {
                            return;
                        }
                    }

                    ItemStack compass = new ItemStack(ErodedItems.DEATH_COMPASS);

                    BlockPos pos = mem.getDeathPos();

                    compass.set(
                            DataComponentTypes.LODESTONE_TRACKER,
                            new LodestoneTrackerComponent(
                                    Optional.of(
                                            GlobalPos.create(
                                                    newPlayer.getWorld().getRegistryKey(),
                                                    pos
                                            )
                                    ),
                                    false
                            )
                    );

                    newPlayer.getInventory().insertStack(compass);
                }
        );
    }
}
