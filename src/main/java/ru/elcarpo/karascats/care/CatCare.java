package ru.elcarpo.karascats.care;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import ru.elcarpo.karascats.KarasCatsMod;

public final class CatCare {
    private static final long UPDATE_INTERVAL_TICKS = 1_200L;

    public static final AttachmentType<CatCareStats> STATS = AttachmentRegistry.create(
            KarasCatsMod.id("cat_care"),
            builder -> builder.persistent(CatCareStats.CODEC)
    );

    private CatCare() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(CatCare::updateTamedCats);
    }

    public static CatCareStats getCurrent(Cat cat, long gameTime) {
        AttachmentTarget attachmentTarget = (AttachmentTarget) cat;
        CatCareStats stored = attachmentTarget.getAttachedOrSet(STATS, CatCareStats.fresh(gameTime));
        CatCareStats current = stored.decayedTo(gameTime);
        if (current != stored) {
            attachmentTarget.setAttached(STATS, current);
        }
        return current;
    }

    public static void set(Cat cat, CatCareStats stats) {
        ((AttachmentTarget) cat).setAttached(STATS, stats);
    }

    public static boolean isOwnedBy(Cat cat, Player player) {
        return cat.isTame() && cat.getOwner() == player;
    }

    public static void sendStatus(Player player, Cat cat, CatCareStats stats) {
        Component customName = cat.getCustomName();
        String name = customName == null ? "Кошка" : customName.getString();
        player.sendSystemMessage(Component.literal(
                name + " — сытость " + stats.hunger() + "/100, настроение "
                        + stats.happiness() + "/100, ухоженность " + stats.cleanliness() + "/100"
        ));
    }

    private static void updateTamedCats(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (gameTime % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Cat cat && cat.isTame()) {
                getCurrent(cat, gameTime);
            }
        }
    }
}
