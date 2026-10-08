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
import ru.elcarpo.karascats.profile.CatProfileManager;

/** Shows the needs of the player's cat without consuming the journal. */
public final class CatCareJournalItem extends Item {
    public CatCareJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat) || !CatCare.isOwnedBy(cat, player)) {
            return InteractionResult.PASS;
        }

        if (!cat.level().isClientSide()) {
            CatCareStats stats = CatCare.getCurrent(cat, cat.level().getGameTime());
            CatCare.sendStatus(player, cat, stats);
            CatProfileManager.sendProfile(player, cat);
        }
        return InteractionResult.SUCCESS;
    }
}
