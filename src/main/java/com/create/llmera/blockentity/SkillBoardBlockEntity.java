package com.create.llmera.blockentity;

import com.create.llmera.ModBlockEntityTypes;
import com.create.llmera.util.NetworkBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SkillBoardBlockEntity extends BlockEntity {
    private String skillName = "";
    private String skillDescription = "";
    private String skillSteps = "";
    private boolean enabled = true;
    @Nullable
    private BlockPos networkPos;

    public SkillBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SKILL_BOARD_BE.get(), pos, state);
    }

    public String getSkillName() {
        return skillName;
    }

    public String getDisplayNameForNetwork() {
        return skillName.isBlank() ? "skill@" + worldPosition.toShortString() : skillName;
    }

    public String getSkillDescription() {
        return skillDescription;
    }

    public String getSkillSteps() {
        return skillSteps;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public BlockPos getNetworkPos() {
        return networkPos;
    }

    public void setNetworkPos(@Nullable BlockPos networkPos) {
        this.networkPos = networkPos;
        sync();
    }

    public void applyClientData(CompoundTag tag) {
        if (tag.contains("SkillName")) {
            skillName = tag.getString("SkillName").trim();
        }
        if (tag.contains("SkillDescription")) {
            skillDescription = tag.getString("SkillDescription");
        }
        if (tag.contains("SkillSteps")) {
            skillSteps = tag.getString("SkillSteps");
        }
        if (tag.contains("Enabled")) {
            enabled = tag.getBoolean("Enabled");
        }
        sync();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        skillName = tag.getString("SkillName");
        skillDescription = tag.getString("SkillDescription");
        skillSteps = tag.getString("SkillSteps");
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        networkPos = NetworkBinding.readNetworkPos(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeData(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
    }

    private void writeData(CompoundTag tag) {
        tag.putString("SkillName", skillName);
        tag.putString("SkillDescription", skillDescription);
        tag.putString("SkillSteps", skillSteps);
        tag.putBoolean("Enabled", enabled);
        if (networkPos != null) {
            NetworkBinding.writeNetworkPos(tag, networkPos);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
