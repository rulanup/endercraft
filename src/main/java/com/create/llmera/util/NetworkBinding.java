package com.create.llmera.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

public final class NetworkBinding {
    private static final String HAS_NETWORK = "HasNetwork";
    private static final String NETWORK_X = "NetworkX";
    private static final String NETWORK_Y = "NetworkY";
    private static final String NETWORK_Z = "NetworkZ";

    private NetworkBinding() {
    }

    public static void writeNetworkPos(CompoundTag tag, BlockPos pos) {
        tag.putBoolean(HAS_NETWORK, true);
        tag.putInt(NETWORK_X, pos.getX());
        tag.putInt(NETWORK_Y, pos.getY());
        tag.putInt(NETWORK_Z, pos.getZ());
    }

    @Nullable
    public static BlockPos readNetworkPos(CompoundTag tag) {
        if (!tag.getBoolean(HAS_NETWORK)) {
            return null;
        }
        return new BlockPos(tag.getInt(NETWORK_X), tag.getInt(NETWORK_Y), tag.getInt(NETWORK_Z));
    }

    public static void bindStackToNetwork(ItemStack stack, BlockPos networkPos) {
        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> writeNetworkPos(tag, networkPos));
    }

    public static void clearStackNetwork(ItemStack stack) {
        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    @Nullable
    public static BlockPos readNetworkPos(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        BlockPos networkPos = data == null ? null : readNetworkPos(data.copyTag());
        if (networkPos != null) {
            return networkPos;
        }

        CustomData legacyData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (legacyData == null || !legacyData.contains(HAS_NETWORK)) {
            return null;
        }

        networkPos = readNetworkPos(legacyData.copyTag());
        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
        if (networkPos != null) {
            bindStackToNetwork(stack, networkPos);
        }
        return networkPos;
    }
}
