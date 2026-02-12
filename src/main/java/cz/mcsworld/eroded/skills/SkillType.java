package cz.mcsworld.eroded.skills;

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
}
