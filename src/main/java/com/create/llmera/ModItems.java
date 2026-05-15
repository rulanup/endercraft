package com.create.llmera;

import com.create.llmera.item.NetworkBindableBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LLMEraMod.MODID);

    public static final DeferredHolder<Item, Item> INTELLIGENT_TRANSMITTER_ITEM = ITEMS.register("intelligent_transmitter",
            () -> new BlockItem(ModBlocks.INTELLIGENT_TRANSMITTER.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> TOOL_LINK_STATION_ITEM = ITEMS.register("tool_link_station",
            () -> new NetworkBindableBlockItem(ModBlocks.TOOL_LINK_STATION.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> SKILL_BOARD_ITEM = ITEMS.register("skill_board",
            () -> new NetworkBindableBlockItem(ModBlocks.SKILL_BOARD.get(), new Item.Properties().stacksTo(64)));
}
