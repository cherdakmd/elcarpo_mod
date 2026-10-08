package ru.elcarpo.karascats.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A keepsake that gives the owner's cat the name Karas. */
public final class KarasMedallionItem extends Item {
    public KarasMedallionItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Cat cat) || !cat.isTame() || cat.getOwner() != player) {
            return InteractionResult.PASS;
        }

        if (cat.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        cat.setCustomName(Component.literal("Карась"));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
