package ru.elcarpo.karascats.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ru.elcarpo.karascats.care.CatCare;
import ru.elcarpo.karascats.care.CatCareStats;

/** A special snack that wins a stray cat's trust or feeds and heals its owner's cat. */
public final class KarasTreatItem extends Item {
    public KarasTreatItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat)) {
            return InteractionResult.PASS;
        }

        boolean strayCat = !cat.isTame();
        if (!strayCat && !CatCare.isOwnedBy(cat, player)) {
            return InteractionResult.PASS;
        }

        // Predict the interaction on the client; all care data and item use are changed server-side.
        if (cat.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        long gameTime = cat.level().getGameTime();
        if (strayCat) {
            cat.tame(player);
            CatCareStats stats = CatCareStats.fresh(gameTime).withAffectionReward(CatCareStats.TREAT_AFFECTION);
            CatCare.set(cat, stats);
            CatCare.sendStatus(player, cat, stats);
        } else {
            CatCareStats stats = CatCare.getCurrent(cat, gameTime);
            boolean canFeed = stats.hunger() < 100;
            boolean canHeal = cat.getHealth() < cat.getMaxHealth();
            if (!canFeed && !canHeal) {
                return InteractionResult.PASS;
            }

            CatCareStats updated = stats.fed(gameTime);
            CatCare.set(cat, updated);
            if (canHeal) {
                cat.heal(4.0F);
            }
            CatCare.sendStatus(player, cat, updated);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
