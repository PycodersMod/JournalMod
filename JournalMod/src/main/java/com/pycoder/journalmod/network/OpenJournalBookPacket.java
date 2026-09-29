package com.pycoder.journalmod.network;

import com.pycoder.journalmod.client.gui.JournalBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenJournalBookPacket {
    private final ItemStack bookStack;

    public OpenJournalBookPacket(ItemStack bookStack) {
        this.bookStack = bookStack.copy();
    }

    public OpenJournalBookPacket(FriendlyByteBuf buf) {
        this.bookStack = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItem(bookStack);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new JournalBookScreen(bookStack));
            });
        });
        context.setPacketHandled(true);
    }
}
