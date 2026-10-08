package ru.elcarpo.karascats;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.elcarpo.karascats.care.CatCare;
import ru.elcarpo.karascats.item.ModItems;

public final class KarasCatsMod implements ModInitializer {
    public static final String MOD_ID = "karas_cats";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        CatCare.initialize();
        LOGGER.info("Карась и его пушистые друзья готовы к мурчанию!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
