package com.create.llmera;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.create.llmera.block.IntelligentTransmitterBlock;
import com.create.llmera.block.ToolLinkStationBlock;
import com.create.llmera.block.SkillBoardBlock;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LLMEraMod.MODID);

    public static final DeferredHolder<Block, Block> INTELLIGENT_TRANSMITTER = BLOCKS.register("intelligent_transmitter",
            () -> new IntelligentTransmitterBlock(machineProperties()));

    public static final DeferredHolder<Block, Block> TOOL_LINK_STATION = BLOCKS.register("tool_link_station",
            () -> new ToolLinkStationBlock(machineProperties()));

    public static final DeferredHolder<Block, Block> SKILL_BOARD = BLOCKS.register("skill_board",
            () -> new SkillBoardBlock(machineProperties()));

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(3.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }
}
