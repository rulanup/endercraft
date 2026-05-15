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
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends AbstractContainerScreen<ConfigMenu> {
    private static final int NETWORK_SCAN_XZ = 24;
    private static final int NETWORK_SCAN_Y = 12;
    private static final int CONVERSATION_VISIBLE_LINES = 10;
    private static final int CONVERSATION_LINE_HEIGHT = 11;

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
    private String toolTargetKind = "normal";
    private boolean enabled = true;
    private int rotationDirection = 1;
    private int conversationScroll;
    private int conversationMaxScroll;
    private Component statusMessage = Component.empty();

    public ConfigScreen(ConfigMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 356;
        this.imageHeight = 250;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + 12;
        int y = this.topPos;
        BlockEntity blockEntity = getBlockEntity();

        if (blockEntity instanceof IntelligentTransmitterBlockEntity transmitter) {
            if (menu.conversationMode) {
                initConversation(x, y, transmitter);
            } else {
                initTransmitter(x, y, transmitter);
            }
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
        secondField = addField(x, y + 64, 146, transmitter.getApiKey(), 512);
        thirdField = addField(x, y + 94, 146, transmitter.getModelName(), 128);
        fourthField = addField(x, y + 124, 146, transmitter.getAiName(), 64);
        fifthField = addField(x, y + 154, 146, transmitter.getSystemPrompt(), 1024);
        addRenderableWidget(Button.builder(tr("screen.llmera.button.save"), button -> saveTransmitter(false))
                .pos(x, y + 184)
                .size(70, 20)
                .build());
        addRenderableWidget(Button.builder(tr("screen.llmera.button.fetch_models"), button -> saveTransmitter(true))
                .pos(x + 76, y + 184)
                .size(70, 20)
                .build());
        addNetworkControls(x + 164, y + 108, transmitter.getBlockPos());
    }

    private void initConversation(int x, int y, IntelligentTransmitterBlockEntity transmitter) {
        screenKind = "conversation";
        addRenderableWidget(Button.builder(tr("screen.llmera.button.new_conversation"), button -> newConversation())
                .pos(x + 250, y + 24)
                .size(70, 20)
                .build());
        firstField = addField(x, y + 210, 256, transmitter.getConversationDraft(), 1024);
        addRenderableWidget(Button.builder(tr("screen.llmera.button.send"), button -> sendConversation())
                .pos(x + 262, y + 210)
                .size(58, 20)
                .build());
    }

    private void initToolLinkStation(int x, int y, ToolLinkStationBlockEntity station) {
        screenKind = "tool";
        toolType = station.getToolType();
        toolTargetKind = station.getTargetKind();
        rotationDirection = station.getRotationDirection();
        firstField = addField(x, y + 34, 146, station.getToolName(), 64);
        secondField = addField(x, y + 64, 146, station.getToolDescription(), 256);

        typeButton = addRenderableWidget(Button.builder(typeLabel(), button -> cycleToolType())
                .pos(x, y + 94)
                .size(146, 20)
                .build());
        thirdField = addField(x, y + 124, 70, Integer.toString(station.getRotationAngle()), 8);
        directionButton = addRenderableWidget(Button.builder(directionLabel(), button -> toggleDirection())
                .pos(x + 84, y + 124)
                .size(62, 20)
                .build());
        fourthField = addField(x, y + 154, 70, Integer.toString(station.getRotationSpeed()), 8);
        updateToolModeWidgets();

        addRenderableWidget(Button.builder(tr("screen.llmera.button.save"), button -> saveTool())
                .pos(x, y + 184)
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

    private void addNetworkControls(int x, int y, BlockPos networkPos) {
        int rowY = y;
        for (NetworkEntry entry : collectNetworkEntries(networkPos)) {
            addRenderableWidget(Button.builder(entry.enabled() ? tr("screen.llmera.button.disable") : tr("screen.llmera.button.enable"), button -> toggleNetworkEntry(entry))
                    .pos(x + 76, rowY)
                    .size(42, 18)
                    .build());
            if (entry.triggerable()) {
                addRenderableWidget(Button.builder(tr("screen.llmera.button.trigger"), button -> triggerNetworkTool(entry))
                        .pos(x + 122, rowY)
                        .size(44, 18)
                        .build());
            }
            rowY += 22;
        }
    }

    private EditBox addField(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(this.font, x, y, width, 18, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value == null ? "" : value);
        addRenderableWidget(field);
        return field;
    }

    private void saveTransmitter(boolean fetchModels) {
        CompoundTag tag = new CompoundTag();
        tag.putString("ModelUrl", firstField.getValue());
        tag.putString("ApiKey", secondField.getValue());
        tag.putString("ModelName", thirdField.getValue());
        tag.putString("AiName", fourthField.getValue());
        tag.putString("SystemPrompt", fifthField.getValue());
        if (fetchModels) {
            tag.putBoolean("FetchModels", true);
        }
        send(menu.pos, tag);
        statusMessage = fetchModels ? tr("screen.llmera.status.fetch_models_sent") : tr("screen.llmera.status.transmitter_saved");
    }

    private void sendConversation() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ChatPrompt", firstField.getValue());
        tag.putString("ConversationDraft", "");
        send(menu.pos, tag);
        firstField.setValue("");
        statusMessage = tr("screen.llmera.status.chat_sent");
    }

    private void newConversation() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ClearConversation", true);
        tag.putString("ConversationDraft", "");
        send(menu.pos, tag);
        firstField.setValue("");
        conversationScroll = 0;
        statusMessage = tr("screen.llmera.status.chat_cleared");
    }

    private void saveTool() {
        CompoundTag tag = new CompoundTag();
        tag.putString("ToolName", firstField.getValue());
        tag.putString("ToolDescription", secondField.getValue());
        tag.putString("ToolType", toolType);
        if (isProgramMode()) {
            tag.putInt("RotationDirection", rotationDirection);
            tag.putInt("RotationAngle", parseInt(thirdField.getValue(), 90));
            tag.putInt("RotationSpeed", parseInt(fourthField.getValue(), 64));
        }
        send(menu.pos, tag);
        statusMessage = tr("screen.llmera.status.tool_saved");
    }

    private void saveSkillBoard() {
        CompoundTag tag = new CompoundTag();
        tag.putString("SkillName", firstField.getValue());
        tag.putString("SkillDescription", secondField.getValue());
        tag.putString("SkillSteps", thirdField.getValue());
        tag.putBoolean("Enabled", enabled);
        send(menu.pos, tag);
        statusMessage = tr("screen.llmera.status.skill_saved");
    }

    private void toggleNetworkEntry(NetworkEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Enabled", !entry.enabled());
        send(entry.pos(), tag);
        statusMessage = tr("screen.llmera.status.network_entry_updated");
    }

    private void triggerNetworkTool(NetworkEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("TriggerTool", true);
        tag.putInt("RequestedPulseTicks", entry.defaultPulseTicks());
        send(entry.pos(), tag);
        statusMessage = tr("screen.llmera.status.trigger_sent");
    }

    private void send(BlockPos pos, CompoundTag tag) {
        PacketDistributor.sendToServer(new UpdateBlockConfigPayload(pos, tag));
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
        updateToolModeWidgets();
    }

    private String[] allowedToolTypes() {
        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof ToolLinkStationBlockEntity station) {
            return station.getAllowedToolTypes();
        }
        return new String[] { "switch", "pulse" };
    }

    private void updateToolModeWidgets() {
        boolean showProgramFields = isProgramMode();
        setFieldVisible(thirdField, showProgramFields);
        setFieldVisible(fourthField, showProgramFields);
        if (directionButton != null) {
            directionButton.visible = showProgramFields;
            directionButton.active = showProgramFields;
        }
    }

    private static void setFieldVisible(EditBox field, boolean visible) {
        if (field != null) {
            field.visible = visible;
            field.active = visible;
        }
    }

    private boolean isProgramMode() {
        return "programmable".equals(toolTargetKind) && "program".equals(toolType);
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
        return toolTypeLabel(toolType);
    }

    private Component toolTypeLabel(String type) {
        return switch (type) {
            case "pulse" -> tr("screen.llmera.tool_type.pulse");
            case "get" -> tr("screen.llmera.tool_type.get");
            case "program" -> tr("screen.llmera.tool_type.program");
            case "skill" -> tr("screen.llmera.tool_type.skill");
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if ("conversation".equals(screenKind) && conversationMaxScroll > 0) {
            int previousScroll = conversationScroll;
            if (scrollY > 0) {
                conversationScroll--;
            } else if (scrollY < 0) {
                conversationScroll++;
            }
            conversationScroll = clampConversationScroll(conversationScroll);
            if (previousScroll != conversationScroll) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        GuiEventListener focused = getFocused();
        if (focused instanceof EditBox editBox && editBox.isFocused()) {
            if ("conversation".equals(screenKind) && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
                sendConversation();
                return true;
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                editBox.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveConversationDraft();
        super.onClose();
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
            guiGraphics.drawString(font, statusMessage, x, y + 232, 0x9BE89B, false);
        }

        switch (screenKind) {
            case "conversation" -> renderConversationText(guiGraphics, x, y, color);
            case "transmitter" -> renderTransmitterText(guiGraphics, x, y, color);
            case "tool" -> renderToolText(guiGraphics, x, y, color);
            case "skill" -> renderSkillText(guiGraphics, x, y, color);
            default -> guiGraphics.drawString(font, tr("screen.llmera.error.block_entity_missing"), x, y + 28, 0xFF7777, false);
        }
    }

    private void renderConversationText(GuiGraphics guiGraphics, int x, int y, int color) {
        BlockEntity blockEntity = getBlockEntity();
        if (!(blockEntity instanceof IntelligentTransmitterBlockEntity transmitter)) {
            return;
        }

        guiGraphics.drawString(font, transmitter.isOnline() ? tr("screen.llmera.label.blaze_online") : tr("screen.llmera.label.blaze_offline"), x, y + 28, transmitter.isOnline() ? 0x9BE89B : 0xFFAA66, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.model_name_value", blankValue(transmitter.getModelName())), x, y + 42, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.ai_response"), x, y + 62, color, false);
        renderScrollableConversation(guiGraphics, conversationText(transmitter), x, y + 78, imageWidth - 36, 0xD9E2F2);
        guiGraphics.drawString(font, tr("screen.llmera.label.chat_input"), x, y + 198, color, false);
    }

    private Component conversationText(IntelligentTransmitterBlockEntity transmitter) {
        if (transmitter.getLastConversationInput().isBlank() && transmitter.getLastConversationResponse().isBlank()) {
            return tr("screen.llmera.conversation.empty");
        }

        StringBuilder builder = new StringBuilder();
        if (!transmitter.getLastConversationInput().isBlank()) {
            builder.append(playerName()).append('\n').append(transmitter.getLastConversationInput()).append("\n\n");
        }
        if (!transmitter.getLastConversationResponse().isBlank()) {
            builder.append(blankValue(transmitter.getAiName())).append('\n').append(transmitter.getLastConversationResponse());
        }
        return Component.literal(builder.toString());
    }

    private void renderTransmitterText(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.drawString(font, tr("screen.llmera.label.model_url"), x, y + 24, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.api_key"), x, y + 54, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.model_name"), x, y + 84, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.ai_name"), x, y + 114, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.system_prompt"), x, y + 144, color, false);

        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof IntelligentTransmitterBlockEntity transmitter) {
            int infoX = x + 164;
            guiGraphics.drawString(font, tr("screen.llmera.label.network_id", transmitter.getNetworkId()), infoX, y + 34, 0xAFC8FF, false);
            guiGraphics.drawString(font, transmitter.isOnline() ? tr("screen.llmera.label.blaze_online") : tr("screen.llmera.label.blaze_offline"), infoX, y + 48, transmitter.isOnline() ? 0x9BE89B : 0xFFAA66, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.model_list"), infoX, y + 64, color, false);
            guiGraphics.drawString(font, trim(blankValue(transmitter.getLastModelList()), 30), infoX, y + 78, 0xD9E2F2, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.bound_entries"), infoX, y + 96, color, false);
            int rowY = y + 113;
            for (NetworkEntry entry : collectNetworkEntries(transmitter.getBlockPos())) {
                String line = trim(entry.name(), 9) + " " + toolTypeLabel(entry.type()).getString();
                guiGraphics.drawString(font, Component.literal(line), infoX, rowY, entry.enabled() ? 0xD9E2F2 : 0x888888, false);
                rowY += 22;
            }
        }
    }

    private void renderToolText(GuiGraphics guiGraphics, int x, int y, int color) {
        guiGraphics.drawString(font, tr("screen.llmera.label.tool_name"), x, y + 24, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.tool_description"), x, y + 54, color, false);
        guiGraphics.drawString(font, tr("screen.llmera.label.tool_mode"), x, y + 84, color, false);
        if (isProgramMode()) {
            guiGraphics.drawString(font, tr("screen.llmera.label.rotation_angle"), x, y + 114, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.rotation_direction"), x + 84, y + 114, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.rotation_speed"), x, y + 144, color, false);
        }

        BlockEntity blockEntity = getBlockEntity();
        if (blockEntity instanceof ToolLinkStationBlockEntity station) {
            int infoX = x + 164;
            guiGraphics.drawString(font, tr("screen.llmera.label.target", trim(station.getTargetDescription(), 20)), infoX, y + 34, 0xAFC8FF, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.kind", targetKindLabel(station.getTargetKind())), infoX, y + 48, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.signal", station.getRedstoneSignal()), infoX, y + 62, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.network", station.getNetworkPos() == null ? tr("screen.llmera.value.unbound") : Component.literal(station.getNetworkPos().toShortString())), infoX, y + 76, color, false);
            guiGraphics.drawString(font, tr("screen.llmera.label.last"), infoX, y + 98, color, false);
            renderWrapped(guiGraphics, Component.literal(station.getLastResult()), infoX, y + 112, 160, 0xD9E2F2, 5);
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

    private List<NetworkEntry> collectNetworkEntries(BlockPos networkPos) {
        Minecraft minecraft = Minecraft.getInstance();
        List<NetworkEntry> entries = new ArrayList<>();
        if (minecraft.level == null) {
            return entries;
        }

        BlockPos min = networkPos.offset(-NETWORK_SCAN_XZ, -NETWORK_SCAN_Y, -NETWORK_SCAN_XZ);
        BlockPos max = networkPos.offset(NETWORK_SCAN_XZ, NETWORK_SCAN_Y, NETWORK_SCAN_XZ);
        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            if (entries.size() >= 5) {
                return;
            }
            BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
            if (blockEntity instanceof ToolLinkStationBlockEntity station && networkPos.equals(station.getNetworkPos())) {
                entries.add(new NetworkEntry(pos.immutable(), station.getDisplayNameForNetwork(), station.getToolType(), station.isEnabled(), true, station.getDefaultPulseTicks()));
            } else if (blockEntity instanceof SkillBoardBlockEntity board && networkPos.equals(board.getNetworkPos())) {
                entries.add(new NetworkEntry(pos.immutable(), board.getDisplayNameForNetwork(), "skill", board.isEnabled(), false, 0));
            }
        });
        return entries;
    }

    private void renderWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * 11, color, false);
        }
    }

    private void renderScrollableConversation(GuiGraphics guiGraphics, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        conversationMaxScroll = Math.max(0, lines.size() - CONVERSATION_VISIBLE_LINES);
        conversationScroll = clampConversationScroll(conversationScroll);
        for (int i = 0; i < CONVERSATION_VISIBLE_LINES && i + conversationScroll < lines.size(); i++) {
            guiGraphics.drawString(font, lines.get(i + conversationScroll), x, y + i * CONVERSATION_LINE_HEIGHT, color, false);
        }
        if (conversationMaxScroll > 0) {
            int trackX = x + width + 5;
            int trackHeight = CONVERSATION_VISIBLE_LINES * CONVERSATION_LINE_HEIGHT - 1;
            int thumbHeight = Math.max(12, trackHeight * CONVERSATION_VISIBLE_LINES / lines.size());
            int thumbY = y + (trackHeight - thumbHeight) * conversationScroll / conversationMaxScroll;
            guiGraphics.fill(trackX, y, trackX + 2, y + trackHeight, 0x665B6B82);
            guiGraphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF9FB6D8);
        }
    }

    private int clampConversationScroll(int scroll) {
        return Math.max(0, Math.min(scroll, conversationMaxScroll));
    }

    private void saveConversationDraft() {
        if (!"conversation".equals(screenKind) || firstField == null) {
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("ConversationDraft", firstField.getValue());
        send(menu.pos, tag);
    }

    private Component titleForScreen() {
        return switch (screenKind) {
            case "conversation" -> tr("screen.llmera.transmitter.conversation");
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

    private static String blankValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String playerName() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? "Player" : minecraft.player.getName().getString();
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

    private record NetworkEntry(BlockPos pos, String name, String type, boolean enabled, boolean triggerable, int defaultPulseTicks) {
    }
}
