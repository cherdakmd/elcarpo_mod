package ru.elcarpo.karascats.profile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Data-pack profile attached to a cat variant. Ratings use a 0-100 scale.
 */
public record CatProfile(
        String name,
        String subscriber,
        String description,
        int agility,
        int strength,
        int defense
) {
    public static final Codec<CatProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CatProfile::name),
            Codec.STRING.optionalFieldOf("subscriber", "").forGetter(CatProfile::subscriber),
            Codec.STRING.optionalFieldOf("description", "").forGetter(CatProfile::description),
            Codec.intRange(0, 100).fieldOf("agility").forGetter(CatProfile::agility),
            Codec.intRange(0, 100).fieldOf("strength").forGetter(CatProfile::strength),
            Codec.intRange(0, 100).fieldOf("defense").forGetter(CatProfile::defense)
    ).apply(instance, CatProfile::new));

    public static final CatProfile DEFAULT = new CatProfile(
            "Обычная кошка",
            "",
            "Обычный кошачий профиль.",
            50,
            50,
            0
    );

    /** Converts a 0-100 agility rating to a movement-speed multiplier around vanilla speed. */
    public double movementSpeedModifier() {
        return (this.agility - 50) / 100.0;
    }

    /** Converts strength into a small additive melee-damage bonus around the vanilla cat value. */
    public double attackDamageModifier() {
        return (this.strength - 50) * 0.04;
    }

    /** Converts the defense rating into Minecraft armor points (0-5). */
    public double armorBonus() {
        return this.defense / 20.0;
    }
}
