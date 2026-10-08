package ru.elcarpo.karascats.item;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import ru.elcarpo.karascats.KarasCatsMod;

public final class ModItems {
    public static final Item KARAS_TREAT = register(
            "karas_treat",
            KarasTreatItem::new,
            new Item.Properties().stacksTo(16)
    );

    public static final Item KARAS_MEDALLION = register(
            "karas_medallion",
            KarasMedallionItem::new,
            new Item.Properties().stacksTo(16)
    );

    public static final Item CAT_TOY = register(
            "cat_toy",
            CatToyItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static final Item CAT_BRUSH = register(
            "cat_brush",
            CatBrushItem::new,
            new Item.Properties().stacksTo(1)
    );

    public static final Item CAT_JOURNAL = register(
            "cat_journal",
            CatCareJournalItem::new,
            new Item.Properties().stacksTo(1)
    );

    private ModItems() {
    }

    private static Item register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, KarasCatsMod.id(name));
        Item item = itemFactory.apply(settings.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(KARAS_TREAT);
            entries.accept(KARAS_MEDALLION);
            entries.accept(CAT_TOY);
            entries.accept(CAT_BRUSH);
            entries.accept(CAT_JOURNAL);
        });
    }
}
