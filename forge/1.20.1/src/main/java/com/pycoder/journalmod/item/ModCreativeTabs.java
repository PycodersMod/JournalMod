package com.pycoder.journalmod.item;

import com.pycoder.journalmod.JournalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JournalMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> JOURNAL_TAB = CREATIVE_MODE_TABS.register("journal_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.JOURNAL_BOOK.get()))
                    .title(Component.translatable("creativemodetab.journal_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.JOURNAL_BOOK.get());
                        pOutput.accept(ModItems.JOURNAL_PAGE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
