package cz.mcsworld.eroded.client.hud;

import cz.mcsworld.eroded.skills.SkillData;

public final class EnergyHudLogic {

    private EnergyHudLogic() {}

    public static final int GREEN  = 0xFF2ECC71;
    public static final int YELLOW = 0xFFF4D03F;
    public static final int ORANGE = 0xFFE67E22;
    public static final int RED    = 0xFFFF5555;
    public static final int EMPTY  = 0xFF555555;

    public static final String WARN_TIRED      = "eroded.energy.state.tired";
    public static final String WARN_EXHAUSTED  = "eroded.energy.state.exhausted";
    public static final String WARN_EMPTY      = "eroded.energy.state.empty";

    public record SegmentVisual(boolean visible, int color, float scale) {}

    public static SegmentVisual resolve(int index, int total, int energy, int maxEnergy, boolean isRegenerating, int ticks) {
        if (maxEnergy <= 0) return new SegmentVisual(false, EMPTY, 1.0f);

        float ratio = energy / (float) maxEnergy;
        float percent = ratio * 100f;

        if (percent <= 1f && !isRegenerating) {
            return new SegmentVisual(false, EMPTY, 1.0f);
        }

        float thresholdForThisIcon = (index / (float) total) * 100f;
        float thresholdForNextIcon = ((index + 1) / (float) total) * 100f;

        boolean visible = percent > thresholdForThisIcon;

        if (!visible) {
            return new SegmentVisual(false, EMPTY, 1.0f);
        }

        boolean blinkOn = ((ticks / 10) % 2 == 0);
        boolean baseBlink = percent <= 20f;

        int lastActiveIndex = (int) (ratio * total);
        boolean isLeadingIcon = (index == lastActiveIndex);

        boolean isRegenIcon = isRegenerating && isLeadingIcon;

        boolean blink = isRegenIcon ? percent <= 20f : (isLeadingIcon && baseBlink);
        if (blink && !blinkOn) {
            return new SegmentVisual(false, EMPTY, 1.0f);
        }

        int color = isRegenIcon ? YELLOW : baseColorByPercent(percent);

        float scale = 1.0f;
        if (isRegenIcon) {
            float pulse = (float) Math.sin((ticks % 20) / 20f * Math.PI);
            scale = 1.0f + 0.16f * pulse;
        }

        return new SegmentVisual(true, color, scale);
    }

    public static SkillData.EnergyState getCurrentState(int energy, int maxEnergy) {
        if (maxEnergy <= 0) return SkillData.EnergyState.NORMAL;
        float percent = (energy / (float) maxEnergy) * 100f;

        if (percent <= 1f) return SkillData.EnergyState.EMPTY;
        if (percent <= 35f) return SkillData.EnergyState.EXHAUSTED;
        if (percent <= 51f) return SkillData.EnergyState.TIRED;
        return SkillData.EnergyState.NORMAL;
    }

    public static String getWarningTranslationKey(SkillData.EnergyState state) {
        return switch (state) {
            case TIRED -> WARN_TIRED;
            case EXHAUSTED -> WARN_EXHAUSTED;
            case EMPTY -> WARN_EMPTY;
            default -> null;
        };
    }

    private static int baseColorByPercent(float percent) {
        if (percent >= 51f) return GREEN;
        if (percent >= 35f) return YELLOW;
        if (percent >= 25f) return ORANGE;
        return RED;
    }
}