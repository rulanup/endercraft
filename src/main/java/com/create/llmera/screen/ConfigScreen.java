package com.create.llmera.screen;

import com.create.llmera.blockentity.IntelligentTransmitterBlockEntity;
import com.create.llmera.blockentity.SkillBoardBlockEntity;
import com.create.llmera.blockentity.ToolLinkStationBlockEntity;
import com.create.llmera.menu.ConfigMenu;
import com.create.llmera.network.UpdateBlockConfigPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

public class ConfigScreen extends AbstractContainerScreen<ConfigMenu> {
    private EditBox firstField;
    private EditBox secondField;
    private EditBox thirdField;
    private EditBox fourthField;
    private EditBox fifthField;
    private Button typeButton;
    private Button enabledButton;
    private Button directionButton;
    private String screenKind = "missing";
    private String toolType = "switch";
    private boolean enabled = true;
    private int rotationDirection = 1;
    private Component statusMessage = Component.empty();

    public ConfigScreen(ConfigMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 276;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + 12;
        int y = this.topPos;
        BlockEntity blockEntity = getBlockEntity();

        if (blockEntity instanceof IntelligentTransmitterBlockEntity transmitter) {
            initTransmitter(x, y, transmitter);
        } else if (blockEntity instanceof ToolLinkStationBlockEntity station) {
            initToolLinkStation(x, y, station);
        } else if (blockEntity instanceof SkillBoardBlockEntity skillBoard) {
            initSkillBoard(x, y, skillBoard);
        } else {
            screenKind = "missing";
            addRenderableWidget(Button.builder(tr("screen.llmera.button.close"), button -> onClose())
                    .pos(x, y + 42)
                    .size(70, 20)
                    .build());
        }
    }

    private void initTransmitter(int x, int y, IntelligentTransmitterBlockEntity transmitter) {
        screenKind = "transmitter";
        firstField = addField(x, y + 34, 146, transmitter.getModelUrl(), 512);
        secondField = addField(x, y + 64, 146, "", 512);
        thirdField = addField(x, y + 94, 146, transmitter.getAiName(), 64);
        fourthField = addField(x, y + 124, 146, transmitter.getSystemPrompt(), 1024);
        addRenderableWidget(Button.builder(tr("screen.llmera.button.save"), button -> saveTransmitter())
                .pos(x, y + 154)
                .size(70, 20)
                .build());
    }

    private void initToolLinkStation(int x, int y, ToolLinkStationBlockEntity station) {
        screenKind = "tool";
        toolType = station.getToolType();
        enabled = station.isEnabled();
        rotationDirection = station.getRotationDirection();
        firstField = addField(x, y + 34, 146, station.getToolName(), 64);
        secondField = addField(x, y + 64, 146, station.getToolDescription(), 256);
        thirdField = addField(x, y + 94, 70, Integer.toString(station.getDefaultPulseTicks()), 8);
        fourthField = addField(x, y + 124, 70, Integer.toString(station.getRotationAngle()), 8);
        fifthField = addField(x, y + 154, 70, Integer.toString(station.getRotationSpeed()), 8);

        typeButton = addRenderableWidget(Button.builder(typeLabel(), button -> cycleToolType())
                .pos(x + 84, y + 94)
                .size(62, 20)
                .build());
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> toggleEnabled())
                .pos(x + 84, y + 124)
                .size(62, 20)
                .build());
        directionButton = addRenderableWidget(Button.builder(directionLabel(), button -> toggleDirection())
                .pos(x + 84, y + 154)
                .size(62, 20)
                .build());
        addRenderableWidget(Button.builder(tr("screen.llmera.button.save"), button -> saveTool(false))
                .pos(x, y + 184)
                .size(70, 20)
                .build());
        addRenderableWidget(Button.builder(tr("screen.llmera.button.trigger"), button -> saveTool(true))
                .pos(x + 76, y + 184)
                .size(70, 20)
                .build());
    }

    private void initSkillBoard(int x, int y, SkillBoardBlockEntity skillBoard) {
        screenKind = "skill";
        enabled = skillBoard.isEnabled();
        firstField = addField(x, y + 34, 236, skillBoard.getSkillName(), 64);
        secondField = addField(x, y + 64, 236, skillBoard.getSkillDescription(), 256);
        thirdField = addField(x, y + 94, 236, skillBoard.getSkillSteps(), 2048);
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> toggleEnabled())
                .pos(x, y + 124)
                .size(70, 20)
                .build());
        addRenderableWidget(Button.builder(tr("screen.llmera.button.save"), button -> saveSkillBoard())
                .pos(x + 76, y + 124)
                .size(70, 20)
                .build());
    }

    private EditBox addField(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(this.font, x, y, width, 18, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value == null ? "" : value);
        addRenderableWidget(field);
        return field;
    }

    private void saveTransmitter() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ModelUrl", firstField.getValue());
        if (!secondField.getValue().isBlank()) {
            tag.putString("ApiKey", secondField.getValue());
        }
        tag.putString("AiName", thirdField.getValue());
        tag.putString("SystemPrompt", fourthField.getValue());
        send(tag);
        statusMessage = tr("screen.llmera.status.transmitter_saved");
    }

    private void saveTool(boolean trigger) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ToolName", firstField.getValue());
        tag.putString("ToolDescription", secondField.getValue());
        tag.putString("ToolType", toolType);
        tag.putBoolean("Enabled", enabled);
        tag.putInt("DefaultPulseTicks", parseInt(thirdField.getValue(), 20));
        tag.putInt("RotationDirection", rotationDirection);
        tag.putInt("RotationAngle", parseInt(fourthField.getValue(), 90));
        tag.putInt("RotationSpeed", parseInt(fifthField.getValue(), 64));
        if (trigger) {
            tag.putBoolean("TriggerTool", true);
            tag.putInt("RequestedPulseTicks", parseInt(thirdField.getValue(), 20));
        }
        send(tag);
        statusMessage = trigger ? tr("screen.llmera.status.trigger_sent") : tr("screen.llmera.status.tool_saved");
    }

    private void saveSkillBoard() {
        CompoundTag tag = new CompoundTag();
        tag.putString("SkillName", firstField.getValue());
        tag.putString("SkillDescription", secondField.getValue());
        tag.putString("SkillSteps", thirdField.getValue());
        tag.putBoolean("Enabled", enabled);
        send(tag);
        statusMessage = tr("screen.llmera.status.skill_saved");
    }

    private void send(CompoundTag tag) {
        PacketDistributor.sendToServer(new UpdateBlockConfigPayload(menu.pos, tag));
    }

    private void cycleToolType() {
        String[] allowed = allowedToolTypes();
        int index = 0;
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(toolType)) {
                index = i;
                break;
            }
        }
        toolType = allowed[(index + 1) % allowed.length];
        typeButton.setMessage(typeLabel());
    }

    private String[] allowedToolTypes() {
        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof ToolLinkStationBlockEntity station) {
            return station.getAllowedToolTypes();
        }
        return new String[] { "switch", "pulse" };
    }

    private void toggleEnabled() {
        enabled = !enabled;
        if (enabledButton != null) {
            enabledButton.setMessage(enabledLabel());
        }
    }

    private void toggleDirection() {
        rotationDirection = rotationDirection < 0 ? 1 : -1;
        if (directionButton != null) {
            directionButton.setMessage(directionLabel());
        }
    }

    private Component typeLabel() {
        return switch (toolType) {
            case "pulse" -> tr("screen.llmera.tool_type.pulse");
            case "get" -> tr("screen.llmera.tool_type.get");
            case "program" -> tr("screen.llmera.tool_type.program");
            default -> tr("screen.llmera.tool_type.switch");
        };
    }

    private Component enabledLabel() {
        return enabled ? tr("screen.llmera.value.enabled") : tr("screen.llmera.value.disabled");
    }

    private Component directionLabel() {
        return rotationDirection < 0 ? tr("screen.llmera.value.reverse") : tr("screen.llmera.value.forward");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderText(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xEE10151C);
        guiGraphics.fill(x, y, x + imageWidth, y + 1, 0xFF6EA8FE);
        guiGraphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF6EA8FE);
        guiGraphics.fill(x, y, x + 1, y + imageHeight, 0xFF6EA8FE);
        guiGraphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF6EA8FE);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private void renderText(GuiGraphics guiGraphics) {
        int x = this.leftPos + 12;
        int y = this.topPos;
        int color = 0xE8EEF7;
        guiGraphics.drawString(font, titleForScreen(), x, y + 8, color, false);
        if (!statusMessage.getString().isBlank()) {
            guiGraphics.drawString(font, statusMessage, x, y + 216, 0x9BE89B, false);
        }

        switch (screenKind) {
            case "transmitter" -> renderTransmitterText(guiGraphics, x, y, color);
            case "tool" -> renderToolText(guiGraphics, x, y, color);
            case "skill" -> renderSkillText(guiGraphics, x, y, color);
            default -> guiGraphics.drawString(font, tr("screen.llmera.error.block_entity_missing"), x, y + 28, 0xFF7777, false);
        }
    }

    private void renderTransmitterText(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.drawString(font, tr("screen.llmera.label.model_url"), x, y + 24, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.api_key"), x, y + 54, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.ai_name"), x, y + 84, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.system_prompt"), x, y + 114, color, false);

        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof IntelligentTransmitterBlockEntity transmitter) {
            int infoX = x + 160;
            guiGraphics.drawString(font, tr("screen.llmera.label.network_id", transmitter.getNetworkId()), infoX, y + 34, 0xAFC8FF, false);
            guiGraphics.drawString(font, transmitter.hasBlazeBurner() ? tr("screen.llmera.label.blaze_online") : tr("screen.llmera.label.blaze_missing"), infoX, y + 48, transmitter.hasBlazeBurner() ? 0x9BE89B : 0xFFAA66, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.bound_entries"), infoX, y + 68, color, false);
            int lineY = y + 82;
            for (Component line : transmitter.getNetworkStatusLines()) {
                guiGraphics.drawString(font, Component.literal(trim(line.getString(), 27)), infoX, lineY, 0xD9E2F2, false);
                lineY += 12;
            }
        }
    }

    private void renderToolText(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.drawString(font, tr("screen.llmera.label.tool_name"), x, y + 24, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.tool_description"), x, y + 54, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.pulse_ticks"), x, y + 84, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.rotation_angle"), x, y + 114, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.rotation_speed"), x, y + 144, color, false);

        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof ToolLinkStationBlockEntity station) {
            int infoX = x + 160;
            guiGraphics.drawString(font, tr("screen.llmera.label.target", trim(station.getTargetDescription(), 18)), infoX, y + 34, 0xAFC8FF, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.kind", targetKindLabel(station.getTargetKind())), infoX, y + 48, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.signal", station.getRedstoneSignal()), infoX, y + 62, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.network", station.getNetworkPos() == null ? tr("screen.llmera.value.unbound") : Component.literal(station.getNetworkPos().toShortString())), infoX, y + 76, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.last"), infoX, y + 98, color, false);
            guiGraphics.drawString(font, trim(station.getLastResult(), 28), infoX, y + 112, 0xD9E2F2, false);
        }
    }

    private void renderSkillText(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.drawString(font, tr("screen.llmera.label.skill_name"), x, y + 24, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.skill_description"), x, y + 54, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.skill_steps"), x, y + 84, color, false);
        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof SkillBoardBlockEntity board) {
            guiGraphics.drawString(font, tr("screen.llmera.label.network", board.getNetworkPos() == null ? tr("screen.llmera.value.unbound") : Component.literal(board.getNetworkPos().toShortString())), x, y + 154, color, false);
        }
    }

    private Component titleForScreen() {
        return switch (screenKind) {
            case "transmitter" -> tr("screen.llmera.title.transmitter");
            case "tool" -> tr("screen.llmera.title.tool_link_station");
            case "skill" -> tr("screen.llmera.title.skill_board");
            default -> tr("screen.llmera.title.config");
        };
    }

    private Component targetKindLabel(String targetKind) {
        return switch (targetKind) {
            case "container" -> tr("screen.llmera.target_kind.container");
            case "programmable" -> tr("screen.llmera.target_kind.programmable");
            default -> tr("screen.llmera.target_kind.normal");
        };
    }

    private BlockEntity getBlockEntity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        BlockPos pos = menu.pos;
        return minecraft.level.getBlockEntity(pos);
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }
}
