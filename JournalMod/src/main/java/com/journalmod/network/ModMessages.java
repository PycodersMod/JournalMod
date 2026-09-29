package com.journalmod.network;

import com.journalmod.JournalMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(JournalMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(OpenJournalBookPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenJournalBookPacket::new)
                .encoder(OpenJournalBookPacket::toBytes)
                .consumerMainThread(OpenJournalBookPacket::handle)
                .add();
    }
}
