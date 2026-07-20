package cz.mcsworld.eroded.world.darkness;

import cz.mcsworld.eroded.config.darkness.DarknessConfigs;
import cz.mcsworld.eroded.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

public final class DarknessMobAIInit {

    private static final String TAG_LIGHT_FEAR_AI = "eroded_light_fear_ai";

    private DarknessMobAIInit() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register(DarknessMobAIInit::onLoad);
    }

    private static void onLoad(Entity entity, ServerLevel world) {
        var root = DarknessConfigs.get();
        var cfg = root.server;

        if (!root.enabled) {
            return;
        }

        if (!cfg.mobLightFearEnabled) {
            return;
        }

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

        if (mob.getTags().contains(TAG_LIGHT_FEAR_AI)) {
            return;
        }

        mob.addTag(TAG_LIGHT_FEAR_AI);

        GoalSelector selector = ((MobEntityAccessor) mob).eroded$getGoalSelector();

        selector.addGoal(1, new LightStartleImpulseGoal(mob));
        selector.addGoal(4, new EscapeFromLightGoal(mob, cfg.escapeSpeed));
        selector.addGoal(5, new StartleFromLightGoal(mob, cfg.escapeDistance, 1.0));
    }
}