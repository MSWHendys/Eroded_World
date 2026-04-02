package cz.mcsworld.eroded.skills;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public record SkillDataRecord(
        Map<SkillType, Float> cg,
        int energy
) {

    public static final Codec<SkillDataRecord> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.unboundedMap(SkillType.CODEC, Codec.FLOAT)
                                    .fieldOf("cg")
                                    .forGetter(SkillDataRecord::cg),

                            Codec.INT.fieldOf("energy")
                                    .forGetter(SkillDataRecord::energy)
                    ).apply(instance, SkillDataRecord::new)
            );

    public static SkillDataRecord fromSkillData(SkillData data) {
        return new SkillDataRecord(
                new HashMap<>(data.getAllCg()),
                data.getEnergy()
        );
    }

    public static void applyToSkillData(SkillData data, SkillDataRecord record) {
        data.setAllCg(record.cg());
        data.setEnergy(record.energy());
    }
}