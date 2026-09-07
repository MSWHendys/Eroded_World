package cz.mcsworld.eroded.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public record SkillDataRecord(
        Map<SkillType, Float> cg,
        int energy,
        boolean collapsed,
        long collapseUntilMs,
        long immunityUntilMs,
        int miningWorkProgress
) {

    public static final Codec<SkillDataRecord> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.unboundedMap(SkillType.CODEC, Codec.FLOAT)
                                    .fieldOf("cg")
                                    .forGetter(SkillDataRecord::cg),

                            Codec.INT.fieldOf("energy")
                                    .forGetter(SkillDataRecord::energy),

                            Codec.BOOL.optionalFieldOf("collapsed", false)
                                    .forGetter(SkillDataRecord::collapsed),

                            Codec.LONG.optionalFieldOf("collapseUntilMs", 0L)
                                    .forGetter(SkillDataRecord::collapseUntilMs),

                            Codec.LONG.optionalFieldOf("immunityUntilMs", 0L)
                                    .forGetter(SkillDataRecord::immunityUntilMs),

                            Codec.INT.optionalFieldOf("miningWorkProgress", 0)
                                    .forGetter(SkillDataRecord::miningWorkProgress)
                    ).apply(instance, SkillDataRecord::new)
            );

    public static SkillDataRecord fromSkillData(SkillData data) {
        // getEnergy() first intentionally normalizes an expired collapse before
        // the related timer fields are serialized.
        int energy = data.getEnergy();

        return new SkillDataRecord(
                new HashMap<>(data.getAllCg()),
                energy,
                data.isCollapsed(),
                data.getCollapseUntilMs(),
                data.getImmunityUntilMs(),
                data.getMiningWorkProgress()
        );
    }

    public static void applyToSkillData(SkillData data, SkillDataRecord record) {
        data.setAllCg(record.cg());
        data.setEnergy(record.energy());
        data.restoreTemporalState(
                record.collapsed(),
                record.collapseUntilMs(),
                record.immunityUntilMs()
        );
        data.setMiningWorkProgress(record.miningWorkProgress());
    }
}
