package ru.elcarpo.karascats.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.elcarpo.karascats.care.CatCare;
import ru.elcarpo.karascats.care.CatCareStats;

/** A reusable brush that improves an owned cat's cleanliness. */
public final class CatBrushItem extends Item {
    public CatBrushItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat) || !CatCare.isOwnedBy(cat, player)) {
            return InteractionResult.PASS;
        }

        if (cat.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        long gameTime = cat.level().getGameTime();
        CatCareStats stats = CatCare.getCurrent(cat, gameTime);
        if (stats.cleanliness() >= 100 && stats.happiness() >= 100) {
            player.sendSystemMessage(Component.literal("Кошка уже чистая и довольная."));
            return InteractionResult.SUCCESS;
        }
        if (!stats.canGroom(gameTime)) {
            player.sendSystemMessage(Component.literal("Кошка уже недавно вычесана — попробуйте позже."));
            return InteractionResult.SUCCESS;
        }

        CatCareStats updated = stats.groomed(gameTime);
        CatCare.set(cat, updated);
        CatCare.sendStatus(player, cat, updated);
        return InteractionResult.SUCCESS;
    }
}
