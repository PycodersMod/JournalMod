package com.pycoder.journalmod.item;

import com.pycoder.journalmod.config.ModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JournalPageItem extends Item {
    
    private static final String PAGE_ID_TAG = "PageId";

    public JournalPageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pageStack = player.getItemInHand(hand);
        int pageId = getPageId(pageStack);
        
        ItemStack journalBook = findJournalBook(player);
        
        if (journalBook.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.journalmod.no_journal_book"), true);
            }
            return InteractionResultHolder.fail(pageStack);
        }
        
        if (JournalBookItem.hasPage(journalBook, pageId)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.journalmod.page_already_added"), true);
            }
            return InteractionResultHolder.fail(pageStack);
        }
        
        if (!level.isClientSide) {
            boolean added = JournalBookItem.addPage(journalBook, pageId);
            if (added) {
                player.displayClientMessage(
                        Component.translatable("message.journalmod.page_added"), true);
                
                pageStack.shrink(1);
                
                level.playSound(null, player.blockPosition(),
                        SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }
        
        return InteractionResultHolder.sidedSuccess(pageStack, level.isClientSide());
    }

    private ItemStack findJournalBook(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof JournalBookItem) {
            return offhand;
        }
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof JournalBookItem) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }

    public static int getPageId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(PAGE_ID_TAG)) {
            return tag.getInt(PAGE_ID_TAG);
        }
        return 1;
    }

    public static void setPageId(ItemStack stack, int pageId) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(PAGE_ID_TAG, pageId);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level level, 
                                List<Component> tooltip, TooltipFlag flag) {
        int pageId = getPageId(stack);
        tooltip.add(Component.translatable("item.journalmod.journal_page.tooltip.id", pageId));
        tooltip.add(Component.literal("§7" + ModConfig.getPageName(pageId)));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.journalmod.journal_page.tooltip.hint"));
        
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        int pageId = getPageId(stack);
        return Component.literal(ModConfig.getPageName(pageId));
    }
}
