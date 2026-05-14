package com.create.llmera;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.create.llmera.blockentity.IntelligentTransmitterBlockEntity;
import com.create.llmera.blockentity.ToolLinkStationBlockEntity;
import com.create.llmera.blockentity.SkillBoardBlockEntity;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LLMEraMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntelligentTransmitterBlockEntity>> INTELLIGENT_TRANSMITTER_BE = BLOCK_ENTITY_TYPES.register("intelligent_transmitter",
            () -> BlockEntityType.Builder.of(IntelligentTransmitterBlockEntity::new, ModBlocks.INTELLIGENT_TRANSMITTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolLinkStationBlockEntity>> TOOL_LINK_STATION_BE = BLOCK_ENTITY_TYPES.register("tool_link_station",
            () -> BlockEntityType.Builder.of(ToolLinkStationBlockEntity::new, ModBlocks.TOOL_LINK_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SkillBoardBlockEntity>> SKILL_BOARD_BE = BLOCK_ENTITY_TYPES.register("skill_board",
            () -> BlockEntityType.Builder.of(SkillBoardBlockEntity::new, ModBlocks.SKILL_BOARD.get()).build(null));
}
