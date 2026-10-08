package ru.elcarpo.karascats.care;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent needs for one tamed cat. Values are kept between 0 and 100. */
public record CatCareStats(
        int hunger,
        int happiness,
        int cleanliness,
        long lastUpdatedAt,
        long lastPlayedAt,
        long lastGroomedAt
) {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final long PLAY_COOLDOWN_TICKS = 600L;
    public static final long GROOM_COOLDOWN_TICKS = 1_200L;

    public static final Codec<CatCareStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("hunger").forGetter(CatCareStats::hunger),
            Codec.INT.fieldOf("happiness").forGetter(CatCareStats::happiness),
            Codec.INT.fieldOf("cleanliness").forGetter(CatCareStats::cleanliness),
            Codec.LONG.fieldOf("last_updated_at").forGetter(CatCareStats::lastUpdatedAt),
            Codec.LONG.fieldOf("last_played_at").forGetter(CatCareStats::lastPlayedAt),
            Codec.LONG.fieldOf("last_groomed_at").forGetter(CatCareStats::lastGroomedAt)
    ).apply(instance, CatCareStats::new));

    public CatCareStats {
        hunger = clamp(hunger);
        happiness = clamp(happiness);
        cleanliness = clamp(cleanliness);
    }

    public static CatCareStats fresh(long gameTime) {
        return new CatCareStats(
                100,
                80,
                100,
                gameTime,
                gameTime - PLAY_COOLDOWN_TICKS,
                gameTime - GROOM_COOLDOWN_TICKS
        );
    }

    /** Hunger, mood and cleanliness slowly decline by in-game day, not while a world is offline. */
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
                this.lastGroomedAt
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
                current.lastGroomedAt
        );
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
                current.lastGroomedAt
        );
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
                gameTime
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
