package com.create.llmera.block;

import com.create.llmera.ModBlockEntityTypes;
import com.create.llmera.ModItems;
import com.create.llmera.blockentity.IntelligentTransmitterBlockEntity;
import com.create.llmera.menu.ConfigMenu;
import com.create.llmera.util.NetworkBinding;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class IntelligentTransmitterBlock extends Block implements EntityBlock {
    private static final ResourceLocation BLAZE_BURNER_ID = ResourceLocation.parse("create:blaze_burner");
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public IntelligentTransmitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntityTypes.INTELLIGENT_TRANSMITTER_BE.get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openConfigMenu(serverPlayer, pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ModItems.TOOL_LINK_STATION_ITEM.get()) || stack.is(ModItems.SKILL_BOARD_ITEM.get())) {
            if (!level.isClientSide) {
                NetworkBinding.bindStackToNetwork(stack, pos);
                player.sendSystemMessage(Component.translatable("message.llmera.network.bound", pos.toShortString()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new ConfigMenu(containerId, inventory, pos),
                Component.translatable("screen.llmera.transmitter.config")
        );
    }

    public static void openConfigMenu(ServerPlayer serverPlayer, BlockPos pos) {
        openConfigMenu(serverPlayer, pos, false);
    }

    public static void openConfigMenu(ServerPlayer serverPlayer, BlockPos pos, boolean conversationMode) {
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, player) -> new ConfigMenu(containerId, inventory, pos, conversationMode),
                Component.translatable(conversationMode ? "screen.llmera.transmitter.conversation" : "screen.llmera.transmitter.config")
        );
        ((IPlayerExtension) serverPlayer).openMenu(provider, buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeBoolean(conversationMode);
        });
    }

    public static boolean hasBlazeBurnerAdjacent(Level level, BlockPos center) {
        return BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 1, 1))
                .anyMatch(pos -> isBlazeBurner(level, pos));
    }

    public static boolean hasActiveBlazeBurnerAdjacent(Level level, BlockPos center) {
        return BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 1, 1))
                .anyMatch(pos -> isActiveBlazeBurner(level, pos));
    }

    public static boolean isBlazeBurner(Level level, BlockPos pos) {
        Registry<Block> blockRegistry = level.registryAccess().registryOrThrow(Registries.BLOCK);
        Optional<Block> blazeBurnerOptional = blockRegistry.getOptional(BLAZE_BURNER_ID);
        return blazeBurnerOptional.filter(block -> level.getBlockState(pos).is(block)).isPresent();
    }

    public static boolean isActiveBlazeBurner(Level level, BlockPos pos) {
        if (!isBlazeBurner(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
                && state.getValue(BlazeBurnerBlock.HEAT_LEVEL).isAtLeast(BlazeBurnerBlock.HeatLevel.FADING);
    }
}
