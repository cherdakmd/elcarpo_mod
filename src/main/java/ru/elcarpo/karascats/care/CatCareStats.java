package ru.elcarpo.karascats.care;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent needs (0-100) and a non-decreasing affection counter for one tamed cat. */
public record CatCareStats(
        int hunger,
        int happiness,
        int cleanliness,
        long lastUpdatedAt,
        long lastPlayedAt,
        long lastGroomedAt,
        long affection
) {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final long PLAY_COOLDOWN_TICKS = 600L;
    public static final long GROOM_COOLDOWN_TICKS = 1_200L;
    public static final long TREAT_AFFECTION = 2L;
    public static final long PLAY_AFFECTION = 5L;
    public static final long GROOM_AFFECTION = 3L;

    public static final Codec<CatCareStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("hunger").forGetter(CatCareStats::hunger),
            Codec.INT.fieldOf("happiness").forGetter(CatCareStats::happiness),
            Codec.INT.fieldOf("cleanliness").forGetter(CatCareStats::cleanliness),
            Codec.LONG.fieldOf("last_updated_at").forGetter(CatCareStats::lastUpdatedAt),
            Codec.LONG.fieldOf("last_played_at").forGetter(CatCareStats::lastPlayedAt),
            Codec.LONG.fieldOf("last_groomed_at").forGetter(CatCareStats::lastGroomedAt),
            Codec.LONG.optionalFieldOf("affection", 0L).forGetter(CatCareStats::affection)
    ).apply(instance, CatCareStats::new));

    public CatCareStats {
        hunger = clamp(hunger);
        happiness = clamp(happiness);
        cleanliness = clamp(cleanliness);
        affection = Math.max(0L, affection);
    }

    public static CatCareStats fresh(long gameTime) {
        return new CatCareStats(
                100,
                80,
                100,
                gameTime,
                gameTime - PLAY_COOLDOWN_TICKS,
                gameTime - GROOM_COOLDOWN_TICKS,
                0L
        );
    }

    /** Hunger, mood and cleanliness decline by in-game day; affection does not decline. */
    public CatCareStats decayedTo(long gameTime) {
        if (gameTime <= this.lastUpdatedAt) {
            return this;
        }

        long daysPassed = (gameTime - this.lastUpdatedAt) / TICKS_PER_DAY;
        if (daysPassed <= 0) {
            return this;
        }

        return new CatCareStats(
                decline(this.hunger, 20, daysPassed),
                decline(this.happiness, 8, daysPassed),
                decline(this.cleanliness, 8, daysPassed),
                this.lastUpdatedAt + daysPassed * TICKS_PER_DAY,
                this.lastPlayedAt,
                this.lastGroomedAt,
                this.affection
        );
    }

    public CatCareStats fed(long gameTime) {
        CatCareStats current = decayedTo(gameTime);
        return new CatCareStats(
                current.hunger + 45,
                current.happiness + 5,
                current.cleanliness,
                gameTime,
                current.lastPlayedAt,
                current.lastGroomedAt,
                current.affection
        ).withAffectionReward(TREAT_AFFECTION);
    }

    public boolean canPlay(long gameTime) {
        return gameTime - this.lastPlayedAt >= PLAY_COOLDOWN_TICKS;
    }

    public CatCareStats played(long gameTime) {
        CatCareStats current = decayedTo(gameTime);
        return new CatCareStats(
                current.hunger - 3,
                current.happiness + 30,
                current.cleanliness,
                gameTime,
                gameTime,
                current.lastGroomedAt,
                current.affection
        ).withAffectionReward(PLAY_AFFECTION);
    }

    public boolean canGroom(long gameTime) {
        return gameTime - this.lastGroomedAt >= GROOM_COOLDOWN_TICKS;
    }

    public CatCareStats groomed(long gameTime) {
        CatCareStats current = decayedTo(gameTime);
        return new CatCareStats(
                current.hunger,
                current.happiness + 8,
                current.cleanliness + 35,
                gameTime,
                current.lastPlayedAt,
                gameTime,
                current.affection
        ).withAffectionReward(GROOM_AFFECTION);
    }

    /** Rewards a successful interaction without changing needs or their timestamps. */
    public CatCareStats withAffectionReward(long reward) {
        if (reward < 0) {
            throw new IllegalArgumentException("Affection rewards must not be negative");
        }
        if (reward == 0) {
            return this;
        }
        long increased = this.affection > Long.MAX_VALUE - reward ? Long.MAX_VALUE : this.affection + reward;
        return new CatCareStats(
                this.hunger,
                this.happiness,
                this.cleanliness,
                this.lastUpdatedAt,
                this.lastPlayedAt,
                this.lastGroomedAt,
                increased
        );
    }

    private static int decline(int value, int amountPerDay, long daysPassed) {
        long amount = Math.min(value, (long) amountPerDay * daysPassed);
        return (int) Math.max(0, value - amount);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
