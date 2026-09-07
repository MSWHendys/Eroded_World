package cz.mcsworld.eroded.config.energy;

import cz.mcsworld.eroded.config.ConfigValidation;
import cz.mcsworld.eroded.config.ConfigValidationException;
import cz.mcsworld.eroded.config.ErodedConfig;
import cz.mcsworld.eroded.config.ErodedConfigs;

public class EnergyConfig implements ErodedConfig {

    public Server server = new Server();

    public Client client = new Client();

    public static EnergyConfig get() {
        return ErodedConfigs.ENERGY;
    }

    @Override
    public void validatePostLoad() throws ConfigValidationException {
        ConfigValidation.notNull(server, "energy.server");
        ConfigValidation.notNull(client, "energy.client");
        ConfigValidation.notNull(server.core, "energy.server.core");
        ConfigValidation.notNull(server.mining, "energy.server.mining");
        ConfigValidation.notNull(server.thresholds, "energy.server.thresholds");
        ConfigValidation.notNull(server.hudThresholds, "energy.server.hudThresholds");
        ConfigValidation.notNull(server.regen, "energy.server.regen");
        ConfigValidation.notNull(server.sleep, "energy.server.sleep");
        ConfigValidation.notNull(server.food, "energy.server.food");
        ConfigValidation.notNull(server.adrenalineShot, "energy.server.adrenalineShot");
        ConfigValidation.notNull(server.collapse, "energy.server.collapse");
        ConfigValidation.notNull(server.warnings, "energy.server.warnings");
        ConfigValidation.notNull(client.hud, "energy.client.hud");
        ConfigValidation.notNull(client.hud.hudPosition, "energy.client.hud.hudPosition");

        ConfigValidation.min(server.core.maxEnergy, 1, "energy.server.core.maxEnergy");
        ConfigValidation.min(server.core.energyPerSegment, 1, "energy.server.core.energyPerSegment");
        ConfigValidation.notNull(server.mining.hardBlocks, "energy.server.mining.hardBlocks");
        ConfigValidation.notNull(server.mining.softBlocks, "energy.server.mining.softBlocks");
        ConfigValidation.min(server.mining.hardBlocks.blocksPerEnergy, 1, "energy.server.mining.hardBlocks.blocksPerEnergy");
        ConfigValidation.min(server.mining.hardBlocks.slowToolMultiplier, 1, "energy.server.mining.hardBlocks.slowToolMultiplier");
        ConfigValidation.min(server.mining.hardBlocks.wrongToolMultiplier, 1, "energy.server.mining.hardBlocks.wrongToolMultiplier");
        ConfigValidation.min(server.mining.softBlocks.blocksPerEnergy, 1, "energy.server.mining.softBlocks.blocksPerEnergy");
        ConfigValidation.min(server.mining.softBlocks.slowToolMultiplier, 1, "energy.server.mining.softBlocks.slowToolMultiplier");
        ConfigValidation.min(server.mining.softBlocks.wrongToolMultiplier, 1, "energy.server.mining.softBlocks.wrongToolMultiplier");
        ConfigValidation.range(server.mining.fullSpeedFromPercent, 1, 100, "energy.server.mining.fullSpeedFromPercent");
        ConfigValidation.range(server.mining.reducedSpeedFromPercent, 1, 100, "energy.server.mining.reducedSpeedFromPercent");
        ConfigValidation.require(
                server.mining.reducedSpeedFromPercent < server.mining.fullSpeedFromPercent,
                "energy.server.mining",
                "must satisfy reducedSpeedFromPercent < fullSpeedFromPercent"
        );
        ConfigValidation.range(server.mining.reducedSpeedPercent, 0, 100, "energy.server.mining.reducedSpeedPercent");
        ConfigValidation.range(server.mining.criticalSpeedPercent, 0, 100, "energy.server.mining.criticalSpeedPercent");
        ConfigValidation.range(server.thresholds.emptyPercent, 0.0f, 100.0f, "energy.server.thresholds.emptyPercent");
        ConfigValidation.range(server.thresholds.exhaustedPercent, 0.0f, 100.0f, "energy.server.thresholds.exhaustedPercent");
        ConfigValidation.range(server.thresholds.tiredPercent, 0.0f, 100.0f, "energy.server.thresholds.tiredPercent");
        ConfigValidation.require(server.thresholds.emptyPercent <= server.thresholds.exhaustedPercent
                        && server.thresholds.exhaustedPercent <= server.thresholds.tiredPercent,
                "energy.server.thresholds", "must satisfy emptyPercent <= exhaustedPercent <= tiredPercent");
        ConfigValidation.range(server.hudThresholds.greenFromPercent, 0.0f, 100.0f, "energy.server.hudThresholds.greenFromPercent");
        ConfigValidation.range(server.hudThresholds.yellowFromPercent, 0.0f, 100.0f, "energy.server.hudThresholds.yellowFromPercent");
        ConfigValidation.range(server.hudThresholds.orangeFromPercent, 0.0f, 100.0f, "energy.server.hudThresholds.orangeFromPercent");
        ConfigValidation.range(server.hudThresholds.blinkBelowPercent, 0.0f, 100.0f, "energy.server.hudThresholds.blinkBelowPercent");
        ConfigValidation.min(server.regen.regenIntervalSeconds, 1, "energy.server.regen.regenIntervalSeconds");
        ConfigValidation.min(server.food.fruitBase, 0, "energy.server.food.fruitBase");
        ConfigValidation.min(server.food.vegetableBase, 0, "energy.server.food.vegetableBase");
        ConfigValidation.min(server.food.grainBase, 0, "energy.server.food.grainBase");
        ConfigValidation.min(server.food.meatBase, 0, "energy.server.food.meatBase");
        ConfigValidation.min(server.food.fishBase, 0, "energy.server.food.fishBase");
        ConfigValidation.min(server.food.mealBase, 0, "energy.server.food.mealBase");
        ConfigValidation.min(server.food.rawMultiplier, 0.0f, "energy.server.food.rawMultiplier");
        ConfigValidation.min(server.food.cookedMultiplier, 0.0f, "energy.server.food.cookedMultiplier");
        ConfigValidation.min(server.food.processedMultiplier, 0.0f, "energy.server.food.processedMultiplier");
        ConfigValidation.min(server.food.specialMultiplier, 0.0f, "energy.server.food.specialMultiplier");
        ConfigValidation.min(server.food.dangerousEnergyPenalty, 0, "energy.server.food.dangerousEnergyPenalty");
        ConfigValidation.min(server.adrenalineShot.immunitySeconds, 0, "energy.server.adrenalineShot.immunitySeconds");
        ConfigValidation.min(server.collapse.collapseDelayMs, 0, "energy.server.collapse.collapseDelayMs");
        ConfigValidation.range(server.collapse.movementSpeedMultiplier, 0.0, 1.0,
                "energy.server.collapse.movementSpeedMultiplier");
        ConfigValidation.min(server.warnings.warningCooldownMs, 0, "energy.server.warnings.warningCooldownMs");

        ConfigValidation.min(client.hud.numberEnergyFlashes, 0, "energy.client.hud.numberEnergyFlashes");
        ConfigValidation.min(client.hud.hudMargin, 0, "energy.client.hud.hudMargin");
        ConfigValidation.min(client.hud.warningMessageTime, 0, "energy.client.hud.warningMessageTime");
        ConfigValidation.min(client.hud.tiredWarningDurationMs, 0, "energy.client.hud.tiredWarningDurationMs");
        ConfigValidation.min(client.hud.exhaustedWarningDurationMs, 0, "energy.client.hud.exhaustedWarningDurationMs");
        ConfigValidation.min(client.hud.emptyWarningDurationMs, 0, "energy.client.hud.emptyWarningDurationMs");
    }

    public static class Server {

        public boolean enabled = true;

        public Core core = new Core();

        public Mining mining = new Mining();

        public Thresholds thresholds = new Thresholds();

        public HudThresholds hudThresholds = new HudThresholds();

        public Regen regen = new Regen();

        public Sleep sleep = new Sleep();

        public Food food = new Food();

        public AdrenalineShot adrenalineShot = new AdrenalineShot();

        public Collapse collapse = new Collapse();

        public Warnings warnings = new Warnings();

        public boolean fatigueWhenExhausted = true;

        public static class Core {

            public int maxEnergy = 100;

            public int energyPerSegment = 1;

            public boolean blockWorkAtZero = true;
        }

        /**
         * Deterministic Energy cost for block mining.  The old miningCost
         * value was a probability (0.001 = 0.1% per block), which made the
         * feature appear non-functional.  Work is now accumulated explicitly.
         */
        public static class Mining {

            public boolean enabled = true;

            /**
             * Vanilla pickaxe/axe tagged blocks: stone, ores, deepslate,
             * metal blocks, logs, planks and similar substantial work.
             */
            public HardBlocks hardBlocks = new HardBlocks();

            /**
             * Vanilla shovel/hoe tagged blocks plus uncategorised natural
             * hand-work: dirt, sand, gravel, clay, snow, wool, glass, leaves,
             * decorations, etc. Untagged hand-work uses the base SOFT cost
             * instead of the wrong-tool penalty.
             */
            public SoftBlocks softBlocks = new SoftBlocks();

            /**
             * While the server is actively processing block breaking for the
             * player, passive Energy regeneration is paused. This prevents
             * slow hand-mining from regenerating more Energy than mining costs.
             */
            public boolean pausePassiveRegenWhileMining = true;

            /** Scale actual block-breaking speed according to current Energy percentage. */
            public boolean speedScalingEnabled = true;

            /** At or above this Energy percentage mining uses 100% vanilla speed. */
            public int fullSpeedFromPercent = 51;

            /** At or above this percentage (and below fullSpeedFromPercent) use reducedSpeedPercent. */
            public int reducedSpeedFromPercent = 21;

            /** Mining speed percentage in the middle Energy band. */
            public int reducedSpeedPercent = 50;

            /** Mining speed percentage from 0 Energy up to reducedSpeedFromPercent - 1. */
            public int criticalSpeedPercent = 10;

            /** When true, reaching 0 Energy keeps mining at criticalSpeedPercent instead of hard-stopping it. */
            public boolean allowMiningAtZero = true;

            public static class HardBlocks {
                /** Suitable fast pickaxe/axe: 1 Energy per this many blocks. */
                public int blocksPerEnergy = 4;

                /** Suitable but slow tool multiplier. */
                public int slowToolMultiplier = 2;

                /** Bare hand or unsuitable tool multiplier. */
                public int wrongToolMultiplier = 20;
            }

            public static class SoftBlocks {
                /** Suitable fast shovel/hoe or ordinary soft work. */
                public int blocksPerEnergy = 10;

                /** Suitable slow tool or bare-hand work on shovel/hoe-class SOFT blocks. */
                public int slowToolMultiplier = 2;

                /** Wrong held tool on a block with an actual vanilla mining-tool requirement. */
                public int wrongToolMultiplier = 10;
            }
        }

        public static class Thresholds {

            public float emptyPercent = 0f;

            public float exhaustedPercent = 20f;

            public float tiredPercent = 50f;
        }

        public static class HudThresholds {

            public float greenFromPercent = 51f;

            public float yellowFromPercent = 35f;

            public float orangeFromPercent = 25f;

            public float blinkBelowPercent = 20f;
        }

        public static class Regen {

            public boolean passiveRegenEnabled = true;

            public int regenIntervalSeconds = 15;
        }

        public static class Sleep {

            public boolean sleepRestoresFull = true;
        }

        public static class Food {

            public int fruitBase = 2;

            public int vegetableBase = 2;

            public int grainBase = 3;

            public int meatBase = 4;

            public int fishBase = 3;

            public int mealBase = 6;

            public float rawMultiplier = 0.6f;

            public float cookedMultiplier = 1.2f;

            public float processedMultiplier = 1.5f;

            public float specialMultiplier = 2.0f;

            public int dangerousEnergyPenalty = 4;
        }

        public static class AdrenalineShot {

            public int immunitySeconds = 120;
        }

        public static class Collapse {

            public int collapseDelayMs = 5000;

            /**
             * Walking-speed multiplier while Energy is collapsed at zero.
             * 1.0 = vanilla walking speed, 0.5 = half speed.
             */
            public double movementSpeedMultiplier = 0.5;
        }

        public static class Warnings {

            public boolean warningsEnabled = true;

            public int warningCooldownMs = 1500;
        }
    }

    public static class Client {

        public Hud hud = new Hud();

        public static class Hud {

            public boolean energyHudEnabled = true;

            public boolean showHudWhenFull = false;

            public int numberEnergyFlashes = 10;

            public EnergyHudPosition hudPosition = EnergyHudPosition.LEFT_DOWN;

            public int hudMargin = 6;

            public int posIconHUD_Y = 15;

            public int posTextHUD_Y = 40;

            /**
             * Legacy frame-count duration used by non-state HUD messages
             * (crafting/anvil/custom messages). Energy-state warnings below
             * use real milliseconds so their visibility is independent of FPS.
             */
            public int warningMessageTime = 60;

            public int tiredWarningDurationMs = 3000;

            public int exhaustedWarningDurationMs = 4000;

            public int emptyWarningDurationMs = 5000;
        }
    }
}
