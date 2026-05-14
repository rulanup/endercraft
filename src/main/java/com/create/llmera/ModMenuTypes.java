package com.create.llmera;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import com.create.llmera.menu.ConfigMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, LLMEraMod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ConfigMenu>> CONFIG_MENU =
            MENU_TYPES.register("config_menu", () -> IMenuTypeExtension.create(ConfigMenu::new));
}