package com.create.llmera;

import com.create.llmera.network.ModNetworking;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(LLMEraMod.MODID)
public class LLMEraMod {
    public static final String MODID = "llmera";

    public LLMEraMod() {
        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModNetworking.register(modEventBus);
    }
}
