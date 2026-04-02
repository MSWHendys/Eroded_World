package cz.mcsworld.eroded.skills;

import com.mojang.serialization.Codec;

public enum SkillType {

    WOODWORKING("eroded.skill.woodworking"),
    SMELTING("eroded.skill.smelting");

    private final String translationKey;

    SkillType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public static final Codec<SkillType> CODEC =
            Codec.STRING.xmap(
                    SkillType::valueOf,
                    SkillType::name
            );
}
