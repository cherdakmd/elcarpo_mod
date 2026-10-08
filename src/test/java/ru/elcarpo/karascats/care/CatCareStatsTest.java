package ru.elcarpo.karascats.care;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatCareStatsTest {
    private static CatCareStats sample() {
        return new CatCareStats(40, 30, 20, 1_000L, 900L, 800L, 123L);
    }

    @Test
    void newCatsStartWithoutAffection() {
        CatCareStats fresh = CatCareStats.fresh(1_000L);

        assertEquals(0L, fresh.affection());
        assertEquals(100, fresh.hunger());
        assertEquals(80, fresh.happiness());
        assertEquals(100, fresh.cleanliness());
    }

    @Test
    void tamingTreatRewardsAffectionWithoutChangingInitialNeedsOrTimers() {
        CatCareStats fresh = CatCareStats.fresh(1_000L);
        CatCareStats tamed = fresh.withAffectionReward(CatCareStats.TREAT_AFFECTION);

        assertEquals(new CatCareStats(
                fresh.hunger(), fresh.happiness(), fresh.cleanliness(),
                fresh.lastUpdatedAt(), fresh.lastPlayedAt(), fresh.lastGroomedAt(), 2L
        ), tamed);
    }

    @Test
    void feedingAddsTwoAffectionAlongsideItsExistingEffects() {
        assertEquals(new CatCareStats(85, 35, 20, 1_000L, 900L, 800L, 125L), sample().fed(1_000L));
    }

    @Test
    void playingAddsFiveAffectionAlongsideItsExistingEffects() {
        assertEquals(new CatCareStats(37, 60, 20, 1_600L, 1_600L, 800L, 128L), sample().played(1_600L));
    }

    @Test
    void groomingAddsThreeAffectionAlongsideItsExistingEffects() {
        assertEquals(new CatCareStats(40, 38, 55, 2_000L, 900L, 2_000L, 126L), sample().groomed(2_000L));
    }

    @Test
    void consecutiveCareRewardsAccumulate() {
        CatCareStats original = sample();
        CatCareStats caredFor = original.fed(1_000L).played(1_600L).groomed(2_200L);

        assertEquals(133L, caredFor.affection());
        assertEquals(123L, original.affection());
    }

    @Test
    void affectionIsNotClampedToTheNeedsRange() {
        CatCareStats stats = new CatCareStats(200, -1, 101, 0L, 0L, 0L, 10_000L);

        assertEquals(100, stats.hunger());
        assertEquals(0, stats.happiness());
        assertEquals(100, stats.cleanliness());
        assertEquals(10_000L, stats.affection());
    }

    @Test
    void negativeStoredAffectionIsSanitized() {
        assertEquals(0L, new CatCareStats(100, 80, 100, 0L, 0L, 0L, -1L).affection());
    }

    @Test
    void affectionCannotBeReducedByAReward() {
        CatCareStats stats = sample();

        assertThrows(IllegalArgumentException.class, () -> stats.withAffectionReward(-1L));
        assertEquals(123L, stats.affection());
    }

    @Test
    void zeroRewardLeavesTheRecordUntouched() {
        CatCareStats stats = sample();

        assertSame(stats, stats.withAffectionReward(0L));
    }

    @Test
    void affectionSaturatesInsteadOfOverflowing() {
        CatCareStats nearlyFull = new CatCareStats(40, 30, 20, 0L, 0L, 0L, Long.MAX_VALUE - 1L);

        assertEquals(Long.MAX_VALUE, nearlyFull.fed(0L).affection());
        assertEquals(Long.MAX_VALUE, nearlyFull.played(0L).affection());
        assertEquals(Long.MAX_VALUE, nearlyFull.groomed(0L).affection());
        assertEquals(Long.MAX_VALUE, nearlyFull.withAffectionReward(Long.MAX_VALUE).affection());
        assertEquals(Long.MAX_VALUE, nearlyFull.withAffectionReward(Long.MAX_VALUE).played(0L).affection());
    }

    @Test
    void affectionSurvivesDailyDecayEvenWhenAllNeedsReachZero() {
        CatCareStats decayed = sample().decayedTo(1_000L + 100L * CatCareStats.TICKS_PER_DAY);

        assertEquals(0, decayed.hunger());
        assertEquals(0, decayed.happiness());
        assertEquals(0, decayed.cleanliness());
        assertEquals(123L, decayed.affection());
    }

    @Test
    void readingBeforeNextDayOrAfterClockMovesBackDoesNotChangeAffection() {
        CatCareStats stats = sample();

        assertSame(stats, stats.decayedTo(1_000L + CatCareStats.TICKS_PER_DAY - 1L));
        assertSame(stats, stats.decayedTo(0L));
        assertTrue(stats.canPlay(1_600L));
        assertTrue(stats.canGroom(2_000L));
        assertEquals(123L, stats.affection());
    }

    @Test
    void olderSavesWithoutAffectionLoadWithZeroAndCanEarnRewards() {
        JsonElement oldSave = JsonParser.parseString("""
                {
                  "hunger": 40,
                  "happiness": 30,
                  "cleanliness": 20,
                  "last_updated_at": 1000,
                  "last_played_at": 900,
                  "last_groomed_at": 800
                }
                """);
        CatCareStats loaded = CatCareStats.CODEC.parse(JsonOps.INSTANCE, oldSave).result().orElseThrow();

        assertEquals(new CatCareStats(40, 30, 20, 1_000L, 900L, 800L, 0L), loaded);
        assertEquals(2L, loaded.fed(1_000L).affection());
    }

    @Test
    void affectionAndNeedsSurviveCodecRoundTrip() {
        CatCareStats original = new CatCareStats(40, 30, 20, 1_000L, 900L, 800L, Long.MAX_VALUE);
        JsonElement saved = CatCareStats.CODEC.encodeStart(JsonOps.INSTANCE, original).result().orElseThrow();
        CatCareStats loaded = CatCareStats.CODEC.parse(JsonOps.INSTANCE, saved).result().orElseThrow();

        assertEquals(Long.MAX_VALUE, saved.getAsJsonObject().get("affection").getAsLong());
        assertEquals(original, loaded);
    }
}
