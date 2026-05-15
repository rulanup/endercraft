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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntelligentTransmitterBlockEntity extends BlockEntity {
    private static final int NETWORK_SCAN_XZ = 24;
    private static final int NETWORK_SCAN_Y = 12;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Pattern MODEL_ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
    private static final Pattern MESSAGE_CONTENT_PATTERN = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"", Pattern.DOTALL);
    private static final Pattern PROGRAM_COMMAND_PATTERN = Pattern.compile("\\[\\[LLMERA_PROGRAM\\s+([^\\]]+)\\]\\]");
    private static final Pattern REDSTONE_COMMAND_PATTERN = Pattern.compile("\\[\\[LLMERA_REDSTONE\\s+([^\\]]+)\\]\\]");
    private static final Pattern NATURAL_REDSTONE_PATTERN = Pattern.compile("工具名称\\s*[:：]\\s*([^,，\\n]+)[,，\\s]*红石信号输出\\s*[:：]\\s*\\{?(-?\\d{1,2})\\}?");
    private static final Pattern COMMAND_ARG_PATTERN = Pattern.compile("([A-Za-z]+)\\s*=\\s*(?:\\\"([^\\\"]*)\\\"|([^\\s]+))");
    private static final int MAX_HTTP_ATTEMPTS = 3;

    private String modelUrl = "";
    private String apiKey = "";
    private String modelName = "";
    private String aiName = "末影助手";
    private String systemPrompt = "";
    private String lastModelList = "";
    private String lastConversationInput = "";
    private String lastConversationResponse = "";
    private String conversationDraft = "";

    public IntelligentTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.INTELLIGENT_TRANSMITTER_BE.get(), pos, state);
    }

    public String getModelUrl() {
        return modelUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public String getAiName() {
        return aiName;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getLastModelList() {
        return lastModelList;
    }

    public String getLastConversationInput() {
        return lastConversationInput;
    }

    public String getLastConversationResponse() {
        return lastConversationResponse;
    }

    public String getConversationDraft() {
        return conversationDraft;
    }

    public String getNetworkId() {
        return worldPosition.getX() + "," + worldPosition.getY() + "," + worldPosition.getZ();
    }

    public boolean hasBlazeBurner() {
        return level != null && IntelligentTransmitterBlock.hasBlazeBurnerAdjacent(level, worldPosition);
    }

    public boolean isOnline() {
        return level != null && IntelligentTransmitterBlock.hasActiveBlazeBurnerAdjacent(level, worldPosition);
    }

    public List<Component> getNetworkStatusLines() {
        List<Component> lines = new ArrayList<>();
        if (level == null) {
            return lines;
        }

        BlockPos min = worldPosition.offset(-NETWORK_SCAN_XZ, -NETWORK_SCAN_Y, -NETWORK_SCAN_XZ);
        BlockPos max = worldPosition.offset(NETWORK_SCAN_XZ, NETWORK_SCAN_Y, NETWORK_SCAN_XZ);
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
        modelName = getString(tag, "ModelName", "modelName");
        aiName = getString(tag, "AiName", "aiName");
        if (aiName.isBlank()) {
            aiName = "末影助手";
        }
        systemPrompt = getString(tag, "SystemPrompt", "systemPrompt");
        lastModelList = tag.getString("LastModelList");
        lastConversationInput = tag.getString("LastConversationInput");
        lastConversationResponse = tag.getString("LastConversationResponse");
        conversationDraft = tag.getString("ConversationDraft");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeData(tag, true);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeData(tag, true);
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
        if (tag.contains("ApiKey")) {
            apiKey = tag.getString("ApiKey").trim();
        }
        if (tag.contains("ModelName")) {
            modelName = tag.getString("ModelName").trim();
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
        if (tag.contains("ConversationDraft")) {
            conversationDraft = tag.getString("ConversationDraft");
        }
        sync();
    }

    public String fetchModelList() {
        lastModelList = requestModelList();
        sync();
        return lastModelList;
    }

    public String sendConversation(String prompt) {
        lastConversationInput = prompt == null ? "" : prompt.trim();
        conversationDraft = "";
        if (!isOnline()) {
            lastConversationResponse = "发报机离线：需要相邻且正在燃烧的烈焰人燃烧室";
        } else if (lastConversationInput.isBlank()) {
            lastConversationResponse = "请输入消息";
        } else {
            lastConversationResponse = requestChatCompletion(lastConversationInput);
        }
        sync();
        return lastConversationResponse;
    }

    public void clearConversation() {
        lastConversationInput = "";
        lastConversationResponse = "";
        conversationDraft = "";
        sync();
    }

    private void writeData(CompoundTag tag, boolean includeSecrets) {
        tag.putString("ModelUrl", modelUrl);
        if (includeSecrets) {
            tag.putString("ApiKey", apiKey);
        }
        tag.putString("ModelName", modelName);
        tag.putString("AiName", aiName);
        tag.putString("SystemPrompt", systemPrompt);
        tag.putString("LastModelList", lastModelList);
        tag.putString("LastConversationInput", lastConversationInput);
        tag.putString("LastConversationResponse", lastConversationResponse);
        tag.putString("ConversationDraft", conversationDraft);
    }

    private String requestModelList() {
        if (modelUrl.isBlank()) {
            return "请先填写模型地址";
        }

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(resolveModelsUrl(modelUrl)))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");
            addAuthorization(requestBuilder);
            HttpRequest request = requestBuilder.GET().build();
            HttpResponse<String> response = sendWith429Retry(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "模型列表请求失败：HTTP " + response.statusCode() + " " + trim(response.body(), 120);
            }

            List<String> models = new ArrayList<>();
            addJsonStringMatches(models, response.body(), MODEL_ID_PATTERN, 12);
            addJsonStringMatches(models, response.body(), MODEL_NAME_PATTERN, 12);
            return models.isEmpty() ? "没有解析到模型名称" : String.join(", ", models);
        } catch (IllegalArgumentException e) {
            return "模型地址无效：" + trim(e.getMessage(), 120);
        } catch (IOException e) {
            return "模型列表请求失败：" + trim(e.getMessage(), 120);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "模型列表请求被中断";
        }
    }

    private String requestChatCompletion(String prompt) {
        if (modelUrl.isBlank()) {
            return "请先填写模型地址";
        }
        if (modelName.isBlank()) {
            return "请先填写模型名称";
        }

        String body = "{"
                + "\"model\":\"" + escapeJson(modelName) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(buildSystemPrompt()) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}"
                + "],"
                + "\"stream\":false"
                + "}";

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(resolveChatUrl(modelUrl)))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            addAuthorization(requestBuilder);
            HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = sendWith429Retry(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "模型请求失败：HTTP " + response.statusCode() + " " + trim(response.body(), 160);
            }
            Matcher matcher = MESSAGE_CONTENT_PATTERN.matcher(response.body());
            String responseText = matcher.find() ? unescapeJson(matcher.group(1)) : trim(response.body(), 240);
            return applyAssistantCommands(responseText);
        } catch (IllegalArgumentException e) {
            return "模型地址无效：" + trim(e.getMessage(), 120);
        } catch (IOException e) {
            return "模型请求失败：" + trim(e.getMessage(), 120);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "模型请求被中断";
        }
    }

    private String buildSystemPrompt() {
        StringBuilder builder = new StringBuilder();
        if (!systemPrompt.isBlank()) {
            builder.append(systemPrompt.trim()).append("\n\n");
        }

        String networkContext = buildNetworkContext();
        if (!networkContext.isBlank()) {
            builder.append(networkContext);
        }
        return builder.toString();
    }

    private String buildNetworkContext() {
        if (level == null) {
            return "";
        }

        List<String> tools = new ArrayList<>();
        List<String> skills = new ArrayList<>();
        BlockPos min = worldPosition.offset(-NETWORK_SCAN_XZ, -NETWORK_SCAN_Y, -NETWORK_SCAN_XZ);
        BlockPos max = worldPosition.offset(NETWORK_SCAN_XZ, NETWORK_SCAN_Y, NETWORK_SCAN_XZ);
        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            if (tools.size() >= 16 && skills.size() >= 16) {
                return;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ToolLinkStationBlockEntity station && worldPosition.equals(station.getNetworkPos()) && station.isEnabled() && tools.size() < 16) {
                tools.add(describeTool(station));
            } else if (blockEntity instanceof SkillBoardBlockEntity board && worldPosition.equals(board.getNetworkPos()) && board.isEnabled() && skills.size() < 16) {
                skills.add(describeSkill(board));
            }
        });

        if (tools.isEmpty() && skills.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("智能网络上下文：\n");
        if (!tools.isEmpty()) {
            builder.append("可查看的工具：\n");
            for (String tool : tools) {
                builder.append("- ").append(tool).append('\n');
            }
            builder.append("可编程齿轮箱接口：如需修改齿轮箱配置，请在回答中包含 [[LLMERA_PROGRAM tool=\"工具名或目标方块ID\" mode=\"angle|distance|delay|await\" value=90 modifier=\"->|>>|<-|<<\"]]；同一次回答内多个 LLMERA_PROGRAM 会按顺序写入第0、1、2...步。\n");
            builder.append("红石输出接口：如需指定工具链接站输出红石信号，请回答 工具名称：工具名或目标方块ID，红石信号输出：0-15，或包含 [[LLMERA_REDSTONE tool=\"工具名或目标方块ID\" signal=15]]。\n");
        }
        if (!skills.isEmpty()) {
            builder.append("可参考的 skills：\n");
            for (String skill : skills) {
                builder.append("- ").append(skill).append('\n');
            }
        }
        return builder.toString();
    }

    private String applyAssistantCommands(String responseText) {
        List<String> results = new ArrayList<>();
        Set<ToolLinkStationBlockEntity> configuredProgramTools = Collections.newSetFromMap(new IdentityHashMap<>());
        Matcher matcher = PROGRAM_COMMAND_PATTERN.matcher(responseText);
        while (matcher.find()) {
            results.add(applyProgramCommand(matcher.group(1), configuredProgramTools));
        }
        matcher = REDSTONE_COMMAND_PATTERN.matcher(responseText);
        while (matcher.find()) {
            results.add(applyRedstoneCommand(matcher.group(1)));
        }
        matcher = NATURAL_REDSTONE_PATTERN.matcher(responseText);
        while (matcher.find()) {
            results.add(applyRedstoneCommand(matcher.group(1).trim(), matcher.group(2).trim()));
        }
        if (results.isEmpty()) {
            return responseText;
        }

        String visibleResponse = REDSTONE_COMMAND_PATTERN.matcher(PROGRAM_COMMAND_PATTERN.matcher(responseText).replaceAll("")).replaceAll("").trim();
        String resultText = String.join("\n", results);
        return visibleResponse.isBlank() ? resultText : visibleResponse + "\n\n" + resultText;
    }

    private String applyProgramCommand(String args, Set<ToolLinkStationBlockEntity> configuredProgramTools) {
        String tool = commandArg(args, "tool");
        if (tool.isBlank()) {
            return "可编程接口失败：缺少 tool";
        }
        ToolLinkStationBlockEntity station = findTool(tool);
        if (station == null) {
            return "可编程接口失败：未找到工具 " + tool;
        }

        String modifier = firstNonBlank(commandArg(args, "modifier"), commandArg(args, "speed"));
        int direction = parseDirection(firstNonBlank(commandArg(args, "direction"), modifier), station.getRotationDirection());
        int speedMultiplier = parseSpeedMultiplier(modifier);
        String mode = firstNonBlank(commandArg(args, "mode"), commandArg(args, "type"), commandArg(args, "instruction"));
        String value = firstNonBlank(commandArg(args, "value"), commandArg(args, "angle"), commandArg(args, "distance"), commandArg(args, "ticks"));
        if (mode.isBlank()) {
            mode = !commandArg(args, "distance").isBlank() ? "distance" : !commandArg(args, "ticks").isBlank() ? "delay" : "angle";
        }
        boolean append = !configuredProgramTools.add(station);
        return station.applyProgramFromAi(mode, parseInt(value, station.getRotationAngle()), direction, speedMultiplier, append);
    }

    private String applyRedstoneCommand(String args) {
        return applyRedstoneCommand(firstNonBlank(commandArg(args, "tool"), commandArg(args, "name")), firstNonBlank(commandArg(args, "signal"), commandArg(args, "redstone")));
    }

    private String applyRedstoneCommand(String tool, String signalValue) {
        if (tool.isBlank()) {
            return "红石接口失败：缺少工具名称";
        }
        ToolLinkStationBlockEntity station = findTool(tool);
        if (station == null) {
            return "红石接口失败：未找到工具 " + tool;
        }
        int signal = parseInt(signalValue, -1);
        if (signal < 0 || signal > 15) {
            return "红石接口失败：红石信号必须在 0-15 之间";
        }
        return station.setRedstoneSignalFromAi(signal);
    }

    private ToolLinkStationBlockEntity findTool(String name) {
        if (level == null) {
            return null;
        }
        BlockPos min = worldPosition.offset(-NETWORK_SCAN_XZ, -NETWORK_SCAN_Y, -NETWORK_SCAN_XZ);
        BlockPos max = worldPosition.offset(NETWORK_SCAN_XZ, NETWORK_SCAN_Y, NETWORK_SCAN_XZ);
        String expected = normalizeToolLookup(name);
        return BlockPos.betweenClosedStream(min, max)
                .map(level::getBlockEntity)
                .filter(blockEntity -> blockEntity instanceof ToolLinkStationBlockEntity station
                        && worldPosition.equals(station.getNetworkPos())
                        && matchesTool(station, expected))
                .map(ToolLinkStationBlockEntity.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static boolean matchesTool(ToolLinkStationBlockEntity station, String expected) {
        return expected.equals(normalizeToolLookup(station.getDisplayNameForNetwork()))
                || expected.equals(normalizeToolLookup(station.getToolName()))
                || expected.equals(normalizeToolLookup(station.getTargetDescription()));
    }

    private static String normalizeToolLookup(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String commandArg(String args, String key) {
        Matcher matcher = COMMAND_ARG_PATTERN.matcher(args);
        while (matcher.find()) {
            if (key.equalsIgnoreCase(matcher.group(1))) {
                String quoted = matcher.group(2);
                return quoted != null ? quoted : matcher.group(3);
            }
        }
        return "";
    }

    private static int parseDirection(String value, int fallback) {
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("reverse") || normalized.equals("backward") || normalized.equals("-1") || normalized.equals("反向") || normalized.equals("<-") || normalized.equals("<<")) {
            return -1;
        }
        if (normalized.equals("forward") || normalized.equals("1") || normalized.equals("正向") || normalized.equals("->") || normalized.equals(">>")) {
            return 1;
        }
        return fallback;
    }

    private static int parseSpeedMultiplier(String value) {
        String normalized = value.trim().toLowerCase();
        if (normalized.equals(">>") || normalized.equals("<<") || normalized.equals("2") || normalized.equals("2x") || normalized.equals("double") || normalized.equals("fast") || normalized.equals("两倍速")) {
            return 2;
        }
        return 1;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String describeTool(ToolLinkStationBlockEntity station) {
        StringBuilder builder = new StringBuilder("工具名称=").append(station.getDisplayNameForNetwork())
                .append("，类型=").append(station.getToolType())
                .append("，目标方块ID=").append(station.getTargetDescription());
        if (!station.getToolDescription().isBlank()) {
            builder.append("，说明=").append(station.getToolDescription().trim());
        }
        if ("container".equals(station.getTargetKind())) {
            builder.append("，容器NBT=").append(station.exportTargetInventory());
        }
        if (!station.getLastResult().isBlank()) {
            builder.append("，上次结果=").append(trim(station.getLastResult(), 160));
        }
        return builder.toString();
    }

    private String describeSkill(SkillBoardBlockEntity board) {
        StringBuilder builder = new StringBuilder(board.getDisplayNameForNetwork());
        if (!board.getSkillDescription().isBlank()) {
            builder.append("：").append(board.getSkillDescription().trim());
        }
        if (!board.getSkillSteps().isBlank()) {
            builder.append("；步骤=").append(board.getSkillSteps().replace('|', '→').trim());
        }
        return builder.toString();
    }

    private static String resolveModelsUrl(String url) {
        String trimmed = normalizeModelUrl(url);
        if (trimmed.endsWith("/api/chat")) {
            return trimmed.substring(0, trimmed.length() - "/api/chat".length()) + "/api/tags";
        }
        if (trimmed.endsWith("/api/generate")) {
            return trimmed.substring(0, trimmed.length() - "/api/generate".length()) + "/api/tags";
        }
        if (trimmed.endsWith("/api")) {
            return trimmed + "/tags";
        }
        if (trimmed.endsWith("/api/tags")) {
            return trimmed;
        }
        if (isLikelyOllamaRootUrl(trimmed)) {
            return trimmed + "/api/tags";
        }
        if (trimmed.endsWith("/models")) {
            return trimmed;
        }
        int chatIndex = trimmed.indexOf("/chat/completions");
        if (chatIndex >= 0) {
            return trimmed.substring(0, chatIndex) + "/models";
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/models";
        }
        int v1Index = trimmed.indexOf("/v1/");
        if (v1Index >= 0) {
            return trimmed.substring(0, v1Index) + "/v1/models";
        }
        return trimTrailingSlash(trimmed) + "/v1/models";
    }

    private static String resolveChatUrl(String url) {
        String trimmed = normalizeModelUrl(url);
        if (trimmed.endsWith("/api")) {
            return trimmed + "/chat";
        }
        if (trimmed.endsWith("/api/chat")) {
            return trimmed;
        }
        if (trimmed.endsWith("/api/generate")) {
            return trimmed.substring(0, trimmed.length() - "/api/generate".length()) + "/api/chat";
        }
        if (isLikelyOllamaRootUrl(trimmed)) {
            return trimmed + "/api/chat";
        }
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed;
        }
        if (trimmed.endsWith("/models")) {
            return trimmed.substring(0, trimmed.length() - "/models".length()) + "/chat/completions";
        }
        if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        }
        int v1Index = trimmed.indexOf("/v1/");
        if (v1Index >= 0) {
            return trimmed.substring(0, v1Index) + "/v1/chat/completions";
        }
        return trimTrailingSlash(trimmed) + "/v1/chat/completions";
    }

    private static String normalizeModelUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.contains("://")) {
            trimmed = "http://" + trimmed;
        }
        return trimTrailingSlash(trimmed);
    }

    private static boolean isLikelyOllamaRootUrl(String url) {
        return url.contains(":11434") && !url.contains("/v1") && !url.contains("/api");
    }

    private void addAuthorization(HttpRequest.Builder requestBuilder) {
        if (!apiKey.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }
    }

    private static HttpResponse<String> sendWith429Retry(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= MAX_HTTP_ATTEMPTS; attempt++) {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 429 || attempt == MAX_HTTP_ATTEMPTS) {
                return response;
            }
            Thread.sleep(retryDelayMillis(response, attempt));
        }
        return response;
    }

    private static long retryDelayMillis(HttpResponse<String> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("").trim();
        if (!retryAfter.isBlank()) {
            try {
                return Math.min(Long.parseLong(retryAfter) * 1000L, 10_000L);
            } catch (NumberFormatException ignored) {
            }
        }
        return Math.min(attempt * 1000L, 5_000L);
    }

    private static void addJsonStringMatches(List<String> values, String body, Pattern pattern, int limit) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find() && values.size() < limit) {
            String value = unescapeJson(matcher.group(1));
            if (!values.contains(value)) {
                values.add(value);
            }
        }
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String unescapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                builder.append(c);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (i + 4 <= value.length() - 1) {
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ignored) {
                            builder.append("\\u").append(hex);
                            i += 4;
                        }
                    } else {
                        builder.append("\\u");
                    }
                }
                default -> builder.append(next);
            }
        }
        return builder.toString();
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
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
