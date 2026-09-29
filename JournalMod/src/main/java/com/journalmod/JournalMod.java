package com.journalmod;

import com.journalmod.config.ModConfig;
import com.journalmod.config.ConfigFileFormatter;
import com.journalmod.item.ModCreativeTabs;
import com.journalmod.item.ModItems;
import com.journalmod.network.ModMessages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JournalMod.MOD_ID)
public class JournalMod {
    public static final String MOD_ID = "journalmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public JournalMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC, "journalmod-common.toml");
        
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        
        modEventBus.addListener(this::commonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ConfigFileFormatter.formatPageChapters();
            ModMessages.register();
            LOGGER.info("Journal Mod initialized successfully!");
        });
    }
}
