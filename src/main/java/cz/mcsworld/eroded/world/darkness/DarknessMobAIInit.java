package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;

public final class DarknessMobAIInit {

    private DarknessMobAIInit() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(DarknessMobAIInit::onLoad);
    }

    private static void onLoad(Entity entity, ServerWorld world) {
        if (!(entity instanceof HostileEntity mob)) return;
        if (!mob.isAlive()) return;

        if (mob.getCommandTags().contains("eroded_light_fear_ai")) return;
        mob.addCommandTag("eroded_light_fear_ai");

        GoalSelector selector = ((MobEntityAccessor) mob).eroded$getGoalSelector();
        selector.add(1, new LightStartleImpulseGoal(mob));

        selector.add(4, new EscapeFromLightGoal(mob, 1.2));

        selector.add(5, new StartleFromLightGoal(mob, 5, 1.0));
    }
}