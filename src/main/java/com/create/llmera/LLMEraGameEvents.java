package com.create.llmera;

import com.create.llmera.block.IntelligentTransmitterBlock;
import com.create.llmera.blockentity.IntelligentTransmitterBlockEntity;
import com.create.llmera.util.NetworkBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

@EventBusSubscriber(modid = LLMEraMod.MODID)
public final class LLMEraGameEvents {
    private LLMEraGameEvents() {
    }

    @SubscribeEvent
    public static void migrateLegacyNetworkBindings(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        Inventory inventory = event.getEntity().getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.TOOL_LINK_STATION_ITEM.get()) || stack.is(ModItems.SKILL_BOARD_ITEM.get())) {
                NetworkBinding.readNetworkPos(stack);
            }
        }
    }

    @SubscribeEvent
    public static void openTransmitterFromBlazeBurner(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Level level = event.getLevel();
        BlockPos clickedPos = event.getPos();
        if (!IntelligentTransmitterBlock.isBlazeBurner(level, clickedPos)) {
            return;
        }

        Optional<BlockPos> transmitterPos = BlockPos.betweenClosedStream(clickedPos.offset(-1, -1, -1), clickedPos.offset(1, 1, 1))
                .filter(pos -> level.getBlockEntity(pos) instanceof IntelligentTransmitterBlockEntity)
                .findFirst();
        if (transmitterPos.isEmpty()) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (!level.isClientSide && event.getEntity() instanceof ServerPlayer serverPlayer) {
            IntelligentTransmitterBlock.openConfigMenu(serverPlayer, transmitterPos.get());
        }
    }
}
