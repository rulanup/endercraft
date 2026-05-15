package com.create.llmera.item;

import com.create.llmera.util.NetworkBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class NetworkBindableBlockItem extends BlockItem {
    public NetworkBindableBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPos networkPos = NetworkBinding.readNetworkPos(stack);
        if (!player.isShiftKeyDown() || networkPos == null) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            NetworkBinding.clearStackNetwork(stack);
            player.sendSystemMessage(Component.translatable("message.llmera.network.cleared"));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos networkPos = NetworkBinding.readNetworkPos(stack);
        if (networkPos == null) {
            tooltip.add(Component.translatable("tooltip.llmera.network.bind_hint"));
            return;
        }

        tooltip.add(Component.translatable("tooltip.llmera.network.bound", networkPos.toShortString()));
        tooltip.add(Component.translatable("tooltip.llmera.network.clear_hint"));
    }
}
