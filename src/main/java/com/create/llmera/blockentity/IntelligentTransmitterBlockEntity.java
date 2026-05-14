package com.create.llmera.blockentity;

import com.create.llmera.ModBlockEntityTypes;
import com.create.llmera.block.IntelligentTransmitterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class IntelligentTransmitterBlockEntity extends BlockEntity {
    private String modelUrl = "";
    private String apiKey = "";
    private String aiName = "末影助手";
    private String systemPrompt = "";

    public IntelligentTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.INTELLIGENT_TRANSMITTER_BE.get(), pos, state);
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAiName() {
        return aiName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getNetworkId() {
        return worldPosition.getX() + "," + worldPosition.getY() + "," + worldPosition.getZ();
    }

    public boolean hasBlazeBurner() {
        return level != null && IntelligentTransmitterBlock.hasBlazeBurnerAdjacent(level, worldPosition);
    }

    public List<Component> getNetworkStatusLines() {
        List<Component> lines = new ArrayList<>();
        if (level == null) {
            return lines;
        }

        BlockPos min = worldPosition.offset(-24, -12, -24);
        BlockPos max = worldPosition.offset(24, 12, 24);
        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            if (lines.size() >= 8) {
                return;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ToolLinkStationBlockEntity station && worldPosition.equals(station.getNetworkPos())) {
                lines.add(Component.translatable(
                        "screen.llmera.network.tool_entry",
                        station.getDisplayNameForNetwork(),
                        Component.translatable("screen.llmera.tool_type." + station.getToolType()),
                        station.isEnabled() ? Component.translatable("screen.llmera.value.enabled") : Component.translatable("screen.llmera.value.disabled")
                ));
            } else if (blockEntity instanceof SkillBoardBlockEntity board && worldPosition.equals(board.getNetworkPos())) {
                lines.add(Component.translatable(
                        "screen.llmera.network.skill_entry",
                        board.getDisplayNameForNetwork(),
                        board.isEnabled() ? Component.translatable("screen.llmera.value.enabled") : Component.translatable("screen.llmera.value.disabled")
                ));
            }
        });
        return lines;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        modelUrl = getString(tag, "ModelUrl", "modelUrl");
        apiKey = getString(tag, "ApiKey", "apiKey");
        aiName = getString(tag, "AiName", "aiName");
        if (aiName.isBlank()) {
            aiName = "末影助手";
        }
        systemPrompt = getString(tag, "SystemPrompt", "systemPrompt");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag, true);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeData(tag, false);
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

    public void applyClientData(CompoundTag tag) {
        if (tag.contains("ModelUrl")) {
            modelUrl = tag.getString("ModelUrl").trim();
        }
        if (tag.contains("ApiKey") && !tag.getString("ApiKey").isBlank()) {
            apiKey = tag.getString("ApiKey").trim();
        }
        if (tag.contains("AiName")) {
            aiName = tag.getString("AiName").trim();
            if (aiName.isBlank()) {
                aiName = "末影助手";
            }
        }
        if (tag.contains("SystemPrompt")) {
            systemPrompt = tag.getString("SystemPrompt");
        }
        sync();
    }

    private void writeData(CompoundTag tag, boolean includeSecrets) {
        tag.putString("ModelUrl", modelUrl);
        if (includeSecrets) {
            tag.putString("ApiKey", apiKey);
        }
        tag.putString("AiName", aiName);
        tag.putString("SystemPrompt", systemPrompt);
    }

    private static String getString(CompoundTag tag, String key, String legacyKey) {
        if (tag.contains(key)) {
            return tag.getString(key);
        }
        return tag.getString(legacyKey);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
