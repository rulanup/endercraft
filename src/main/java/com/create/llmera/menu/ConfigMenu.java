package com.create.llmera.menu;

import com.create.llmera.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public class ConfigMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    public final boolean conversationMode;

    public ConfigMenu(int containerId, Inventory playerInv, BlockPos pos) {
        this(containerId, playerInv, pos, false);
    }

    public ConfigMenu(int containerId, Inventory playerInv, BlockPos pos, boolean conversationMode) {
        super(ModMenuTypes.CONFIG_MENU.get(), containerId);
        this.pos = pos;
        this.conversationMode = conversationMode;
    }

    public ConfigMenu(int containerId, Inventory playerInv, RegistryFriendlyByteBuf extraData) {
        super(ModMenuTypes.CONFIG_MENU.get(), containerId);
        this.pos = extraData.readBlockPos();
        this.conversationMode = extraData.readableBytes() > 0 && extraData.readBoolean();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64.0D && player.level().getBlockEntity(pos) != null;
    }
}
