package com.journalmod.item;

import com.journalmod.config.ModConfig;
import com.journalmod.network.ModMessages;
import com.journalmod.network.OpenJournalBookPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JournalBookItem extends Item {
    
    private static final String PAGES_TAG = "CollectedPages";

    public JournalBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            openBookGUI((ServerPlayer) player, stack);
        } else {
            level.playSound(player, player.blockPosition(), 
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void openBookGUI(ServerPlayer player, ItemStack stack) {
        ModMessages.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenJournalBookPacket(stack)
        );
    }

    public static List<Integer> getCollectedPages(ItemStack stack) {
        List<Integer> pages = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        
        if (tag != null && tag.contains(PAGES_TAG, Tag.TAG_LIST)) {
            ListTag listTag = tag.getList(PAGES_TAG, Tag.TAG_INT);
            for (int i = 0; i < listTag.size(); i++) {
                pages.add(listTag.getInt(i));
            }
        }
        
        Collections.sort(pages);
        return pages;
    }

    public static boolean hasPage(ItemStack stack, int pageId) {
        return getCollectedPages(stack).contains(pageId);
    }

    public static boolean addPage(ItemStack stack, int pageId) {
        if (hasPage(stack, pageId)) {
            return false;
        }
        
        CompoundTag tag = stack.getOrCreateTag();
        ListTag listTag;
        
        if (tag.contains(PAGES_TAG, Tag.TAG_LIST)) {
            listTag = tag.getList(PAGES_TAG, Tag.TAG_INT);
        } else {
            listTag = new ListTag();
        }
        
        listTag.add(net.minecraft.nbt.IntTag.valueOf(pageId));
        tag.put(PAGES_TAG, listTag);
        
        return true;
    }

    public static int getCollectedPageCount(ItemStack stack) {
        return getCollectedPages(stack).size();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                                List<Component> tooltip, TooltipFlag flag) {
        int collected = getCollectedPageCount(stack);
        
        if (Screen.hasShiftDown() && collected > 0) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("item.journalmod.journal_book.tooltip.collected"));
            
            List<Integer> pages = getCollectedPages(stack);
            for (int i = 0; i < Math.min(pages.size(), 5); i++) {
                int pageId = pages.get(i);
                tooltip.add(Component.literal("  §7• " + ModConfig.getPageName(pageId)));
            }
            
            if (pages.size() > 5) {
                tooltip.add(Component.translatable("item.journalmod.journal_book.tooltip.more", 
                        pages.size() - 5));
            }
        } else if (collected > 0) {
            tooltip.add(Component.translatable("item.journalmod.journal_book.tooltip.shift"));
        }
        
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(ModConfig.getJournalBookName());
    }
}
