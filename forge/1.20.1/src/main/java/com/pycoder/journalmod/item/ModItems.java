package com.pycoder.journalmod.item;

import com.pycoder.journalmod.JournalMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
            DeferredRegister.create(ForgeRegistries.ITEMS, JournalMod.MOD_ID);

    public static final RegistryObject<Item> JOURNAL_BOOK = ITEMS.register("journal_book",
            () -> new JournalBookItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static final RegistryObject<Item> JOURNAL_PAGE = ITEMS.register("journal_page",
            () -> new JournalPageItem(new Item.Properties()
                    .stacksTo(1)
                    .fireResistant()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
