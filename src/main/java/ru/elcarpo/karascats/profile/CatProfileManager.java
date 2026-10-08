package ru.elcarpo.karascats.profile;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.elcarpo.karascats.KarasCatsMod;

public final class CatProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(KarasCatsMod.MOD_ID + "/cat_profiles");
    private static final Identifier RELOAD_LISTENER_ID = KarasCatsMod.id("cat_profiles");
    private static final FileToIdConverter PROFILE_FILES = FileToIdConverter.json("cat_profile");

    private static final Identifier AGILITY_MODIFIER_ID = KarasCatsMod.id("cat_profile/agility");
    private static final Identifier STRENGTH_MODIFIER_ID = KarasCatsMod.id("cat_profile/strength");
    private static final Identifier DEFENSE_MODIFIER_ID = KarasCatsMod.id("cat_profile/defense");

    private static volatile Map<Identifier, CatProfile> profiles = Map.of();

    private CatProfileManager() {
    }

    public static void initialize() {
        FabricDefaultAttributeRegistry.MODIFY.register(context -> context.modify(
                EntityTypes.CAT,
                (type, builder) -> {
                    AttributeSupplier existing = DefaultAttributes.getSupplier(type);
                    if (!existing.hasAttribute(Attributes.ATTACK_DAMAGE)) {
                        builder.add(Attributes.ATTACK_DAMAGE, 3.0);
                    }
                    if (!existing.hasAttribute(Attributes.ARMOR)) {
                        builder.add(Attributes.ARMOR);
                    }
                }
        ));

        ResourceLoader.get(PackType.SERVER_DATA)
                .registerReloadListener(RELOAD_LISTENER_ID, new ProfileReloadListener());

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                registerCommands(dispatcher));

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Cat cat) {
                applyAttributes(cat);
            }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Cat cat) {
                        applyAttributes(cat);
                    }
                }
            }
        });
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("karas")
                .then(Commands.literal("profiles")
                        .executes(context -> listProfiles(context.getSource()))));
    }

    private static int listProfiles(CommandSourceStack source) {
        Map<Identifier, CatProfile> snapshot = profiles;
        if (snapshot.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Профили котов ещё не загружены."), false);
            return 0;
        }

        snapshot.entrySet().stream()
                .sorted((left, right) -> left.getKey().toString().compareTo(right.getKey().toString()))
                .forEach(entry -> {
                    Identifier id = entry.getKey();
                    CatProfile profile = entry.getValue();
                    String subscriber = profile.subscriber().isBlank() ? "" : " · " + profile.subscriber();
                    source.sendSuccess(() -> Component.literal(
                            profile.name() + subscriber + " [" + id + "] — ловкость " + profile.agility()
                                    + ", сила " + profile.strength() + ", защита " + profile.defense()
                    ), false);
                    source.sendSuccess(() -> Component.literal(
                            "Призыв: /summon minecraft:cat ~ ~ ~ {variant:\"" + id + "\"}"
                    ), false);
                });
        return snapshot.size();
    }

    public static CatProfile getProfile(Cat cat) {
        return cat.getVariant().unwrapKey()
                .map(key -> profiles.getOrDefault(key.identifier(), CatProfile.DEFAULT))
                .orElse(CatProfile.DEFAULT);
    }

    public static void applyAttributes(Cat cat) {
        if (cat.level().isClientSide()) {
            return;
        }

        CatProfile profile = getProfile(cat);
        updateModifier(cat.getAttribute(Attributes.MOVEMENT_SPEED), AGILITY_MODIFIER_ID,
                profile.movementSpeedModifier(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        updateModifier(cat.getAttribute(Attributes.ATTACK_DAMAGE), STRENGTH_MODIFIER_ID,
                profile.attackDamageModifier(), AttributeModifier.Operation.ADD_VALUE);
        updateModifier(cat.getAttribute(Attributes.ARMOR), DEFENSE_MODIFIER_ID,
                profile.armorBonus(), AttributeModifier.Operation.ADD_VALUE);
    }

    public static void sendProfile(Player player, Cat cat) {
        CatProfile profile = getProfile(cat);
        String subscriber = profile.subscriber().isBlank() ? "" : " (" + profile.subscriber() + ")";
        String description = profile.description().isBlank() ? "" : " — " + profile.description();

        player.sendSystemMessage(Component.literal(profile.name() + subscriber + description));
        player.sendSystemMessage(Component.literal(
                "Ловкость " + profile.agility() + "/100 · сила " + profile.strength() + "/100 · защита " + profile.defense() + "/100"
        ));
    }

    private static void updateModifier(
            AttributeInstance attribute,
            Identifier modifierId,
            double amount,
            AttributeModifier.Operation operation
    ) {
        if (attribute != null) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(modifierId, amount, operation));
        }
    }

    private static final class ProfileReloadListener extends SimpleReloadListener<Map<Identifier, CatProfile>> {
        @Override
        protected Map<Identifier, CatProfile> prepare(SharedState state) {
            Map<Identifier, CatProfile> loaded = new HashMap<>();
            for (Map.Entry<Identifier, Resource> entry : PROFILE_FILES.listMatchingResources(state.resourceManager()).entrySet()) {
                Identifier profileId = PROFILE_FILES.fileToId(entry.getKey());
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = StrictJsonParser.parse(reader);
                    var result = CatProfile.CODEC.parse(JsonOps.INSTANCE, json);
                    if (result.error().isPresent()) {
                        LOGGER.error("Could not read cat profile '{}': {}", profileId, result.error().orElseThrow().message());
                        continue;
                    }
                    loaded.put(profileId, result.result().orElseThrow());
                } catch (IOException | JsonParseException exception) {
                    LOGGER.error("Could not read cat profile '{}'", profileId, exception);
                }
            }
            return loaded;
        }

        @Override
        protected void apply(Map<Identifier, CatProfile> prepared, SharedState state) {
            profiles = Map.copyOf(prepared);
            LOGGER.info("Loaded {} cat profile(s)", profiles.size());
        }
    }
}
