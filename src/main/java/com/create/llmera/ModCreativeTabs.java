package com.create.llmera;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LLMEraMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENDERCRAFT_TAB = CREATIVE_MODE_TABS.register(
            "llmera",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.llmera"))
                    .icon(() -> new ItemStack(ModItems.INTELLIGENT_TRANSMITTER_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.INTELLIGENT_TRANSMITTER_ITEM.get());
                        output.accept(ModItems.TOOL_LINK_STATION_ITEM.get());
                        output.accept(ModItems.SKILL_BOARD_ITEM.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }
}
