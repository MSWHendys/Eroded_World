package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;

public final class DarknessMobAIInit {

    private static final String TAG_LIGHT_FEAR_AI = "eroded_light_fear_ai";

    private DarknessMobAIInit() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(DarknessMobAIInit::onLoad);
    }

    private static void onLoad(Entity entity, ServerLevel world) {
        var cfg = DarknessConfigs.get().server;

        if (!(entity instanceof Monster mob)) {
            return;
        }

        if (mob instanceof AbstractSkeleton) {
            mob.removeTag(TAG_LIGHT_FEAR_AI);
            return;
        }

        if (!mob.isAlive()) {
            return;
        }

        if (mob.entityTags().contains(TAG_LIGHT_FEAR_AI)) {
            return;
        }

        mob.addTag(TAG_LIGHT_FEAR_AI);

        GoalSelector selector = ((MobEntityAccessor) mob).eroded$getGoalSelector();

        selector.addGoal(1, new LightStartleImpulseGoal(mob));
        selector.addGoal(4, new EscapeFromLightGoal(mob));
        selector.addGoal(5, new StartleFromLightGoal(mob, 1.0));
    }
}