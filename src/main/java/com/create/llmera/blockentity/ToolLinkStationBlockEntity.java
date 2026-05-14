package com.create.llmera.blockentity;

import com.create.llmera.ModBlockEntityTypes;
import com.create.llmera.util.NetworkBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ToolLinkStationBlockEntity extends BlockEntity {
    private String toolName = "";
    private String toolDescription = "";
    private String toolType = "switch";
    private boolean enabled = true;
    private int redstoneSignal = 0;
    private int pulseTicksRemaining = 0;
    private int defaultPulseTicks = 20;
    private int rotationDirection = 1;
    private int rotationAngle = 90;
    private int rotationSpeed = 64;
    private String lastResult = "";
    @Nullable
    private BlockPos networkPos;

    public ToolLinkStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.TOOL_LINK_STATION_BE.get(), pos, state);
    }

    public String getToolName() {
        return toolName;
    }

    public String getDisplayNameForNetwork() {
        return toolName.isBlank() ? "tool@" + worldPosition.toShortString() : toolName;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public String getToolType() {
        return toolType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRedstoneSignal() {
        return redstoneSignal;
    }

    public int getDefaultPulseTicks() {
        return defaultPulseTicks;
    }

    public int getRotationDirection() {
        return rotationDirection;
    }

    public int getRotationAngle() {
        return rotationAngle;
    }

    public int getRotationSpeed() {
        return rotationSpeed;
    }

    public String getLastResult() {
        return lastResult;
    }

    @Nullable
    public BlockPos getNetworkPos() {
        return networkPos;
    }

    public void setNetworkPos(@Nullable BlockPos networkPos) {
        this.networkPos = networkPos;
        sync();
    }

    public String getTargetKind() {
        if (level == null) {
            return "normal";
        }
        BlockPos targetPos = getTargetPos();
        BlockEntity targetEntity = level.getBlockEntity(targetPos);
        if (targetEntity instanceof Container) {
            return "container";
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock()).toString();
        if (blockId.equals("create:sequenced_gearshift") || blockId.contains("programmable") || blockId.contains("gearbox")) {
            return "programmable";
        }
        return "normal";
    }

    public String getTargetDescription() {
        if (level == null) {
            return "unknown";
        }
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(getTargetPos()).getBlock()).toString();
    }

    public String[] getAllowedToolTypes() {
        return switch (getTargetKind()) {
            case "container" -> new String[] { "get" };
            case "programmable" -> new String[] { "pulse", "program" };
            default -> new String[] { "switch", "pulse" };
        };
    }

    public void applyClientData(CompoundTag tag) {
        if (tag.contains("ToolName")) {
            toolName = tag.getString("ToolName").trim();
        }
        if (tag.contains("ToolDescription")) {
            toolDescription = tag.getString("ToolDescription");
        }
        if (tag.contains("ToolType")) {
            toolType = normalizeToolType(tag.getString("ToolType"));
        }
        if (tag.contains("Enabled")) {
            enabled = tag.getBoolean("Enabled");
        }
        if (tag.contains("DefaultPulseTicks")) {
            defaultPulseTicks = clamp(tag.getInt("DefaultPulseTicks"), 1, 20 * 60);
        }
        if (tag.contains("RotationDirection")) {
            rotationDirection = tag.getInt("RotationDirection") < 0 ? -1 : 1;
        }
        if (tag.contains("RotationAngle")) {
            rotationAngle = clamp(tag.getInt("RotationAngle"), 1, 3600);
        }
        if (tag.contains("RotationSpeed")) {
            rotationSpeed = clamp(tag.getInt("RotationSpeed"), 1, 4096);
        }
        ensureToolTypeAllowed();
        sync();
    }

    public String invokeFromUi(int requestedPulseTicks) {
        ensureToolTypeAllowed();
        if (!enabled) {
            lastResult = "工具已禁用";
            sync();
            return lastResult;
        }

        switch (toolType) {
            case "switch" -> {
                setRedstoneSignal(redstoneSignal > 0 ? 0 : 15);
                lastResult = redstoneSignal > 0 ? "开关工具已开启" : "开关工具已关闭";
            }
            case "pulse" -> {
                int ticks = requestedPulseTicks > 0 ? requestedPulseTicks : defaultPulseTicks;
                startPulse(clamp(ticks, 1, 20 * 60));
                lastResult = "已输出脉冲，持续 " + pulseTicksRemaining + " 刻";
            }
            case "get" -> lastResult = exportTargetInventory();
            case "program" -> {
                startPulse(defaultPulseTicks);
                lastResult = "已请求编程：方向=" + (rotationDirection < 0 ? "反向" : "正向")
                        + "，角度=" + rotationAngle + "，速度=" + rotationSpeed;
            }
            default -> lastResult = "未知工具类型";
        }
        sync();
        return lastResult;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ToolLinkStationBlockEntity station) {
        if (level.isClientSide || station.pulseTicksRemaining <= 0) {
            return;
        }
        station.pulseTicksRemaining--;
        if (station.pulseTicksRemaining == 0) {
            station.setRedstoneSignal(0);
        } else {
            station.setChanged();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        toolName = tag.getString("ToolName");
        toolDescription = tag.getString("ToolDescription");
        toolType = normalizeToolType(tag.getString("ToolType"));
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        redstoneSignal = clamp(tag.getInt("RedstoneSignal"), 0, 15);
        pulseTicksRemaining = Math.max(0, tag.getInt("PulseTicksRemaining"));
        defaultPulseTicks = tag.contains("DefaultPulseTicks") ? clamp(tag.getInt("DefaultPulseTicks"), 1, 20 * 60) : 20;
        rotationDirection = tag.getInt("RotationDirection") < 0 ? -1 : 1;
        rotationAngle = tag.contains("RotationAngle") ? clamp(tag.getInt("RotationAngle"), 1, 3600) : 90;
        rotationSpeed = tag.contains("RotationSpeed") ? clamp(tag.getInt("RotationSpeed"), 1, 4096) : 64;
        lastResult = tag.getString("LastResult");
        networkPos = NetworkBinding.readNetworkPos(tag);
        ensureToolTypeAllowed();
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
        tag.putString("ToolName", toolName);
        tag.putString("ToolDescription", toolDescription);
        tag.putString("ToolType", toolType);
        tag.putBoolean("Enabled", enabled);
        tag.putInt("RedstoneSignal", redstoneSignal);
        tag.putInt("PulseTicksRemaining", pulseTicksRemaining);
        tag.putInt("DefaultPulseTicks", defaultPulseTicks);
        tag.putInt("RotationDirection", rotationDirection);
        tag.putInt("RotationAngle", rotationAngle);
        tag.putInt("RotationSpeed", rotationSpeed);
        tag.putString("LastResult", lastResult);
        if (networkPos != null) {
            NetworkBinding.writeNetworkPos(tag, networkPos);
        }
    }

    private BlockPos getTargetPos() {
        return worldPosition.below();
    }

    private void startPulse(int ticks) {
        pulseTicksRemaining = ticks;
        setRedstoneSignal(15);
    }

    private void setRedstoneSignal(int strength) {
        int next = clamp(strength, 0, 15);
        if (redstoneSignal == next) {
            return;
        }
        redstoneSignal = next;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            level.updateNeighborsAt(worldPosition.below(), getBlockState().getBlock());
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private String exportTargetInventory() {
        if (level == null) {
            return "世界未加载";
        }
        BlockEntity targetEntity = level.getBlockEntity(getTargetPos());
        if (!(targetEntity instanceof Container container)) {
            return "目标方块不是容器";
        }

        ListTag items = new ListTag();
        HolderLookup.Provider registries = level.registryAccess();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Stack", stack.save(registries));
            items.add(entry);
        }
        CompoundTag result = new CompoundTag();
        result.putString("Target", getTargetDescription());
        result.put("Items", items);
        return result.toString();
    }

    private void ensureToolTypeAllowed() {
        String[] allowed = getAllowedToolTypes();
        for (String allowedType : allowed) {
            if (allowedType.equals(toolType)) {
                return;
            }
        }
        toolType = allowed[0];
    }

    private static String normalizeToolType(String type) {
        return switch (type) {
            case "pulse", "get", "program" -> type;
            default -> "switch";
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
