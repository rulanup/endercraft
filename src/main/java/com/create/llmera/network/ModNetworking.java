package com.create.llmera.network;

import com.create.llmera.blockentity.IntelligentTransmitterBlockEntity;
import com.create.llmera.blockentity.SkillBoardBlockEntity;
import com.create.llmera.blockentity.ToolLinkStationBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(UpdateBlockConfigPayload.TYPE, UpdateBlockConfigPayload.STREAM_CODEC, ModNetworking::handleUpdateBlockConfig);
    }

    private static void handleUpdateBlockConfig(UpdateBlockConfigPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!serverPlayer.level().hasChunkAt(payload.pos())) {
            return;
        }
        if (serverPlayer.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0D) {
            return;
        }

        BlockEntity blockEntity = serverPlayer.level().getBlockEntity(payload.pos());
        CompoundTag data = payload.data();
        if (blockEntity instanceof IntelligentTransmitterBlockEntity transmitter) {
            transmitter.applyClientData(data);
        } else if (blockEntity instanceof ToolLinkStationBlockEntity station) {
            station.applyClientData(data);
            if (data.getBoolean("TriggerTool")) {
                String result = station.invokeFromUi(data.getInt("RequestedPulseTicks"));
                serverPlayer.sendSystemMessage(Component.literal(result));
            }
        } else if (blockEntity instanceof SkillBoardBlockEntity skillBoard) {
            skillBoard.applyClientData(data);
        }
    }
}
