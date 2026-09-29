package com.pycoder.journalmod.client.gui;

import com.pycoder.journalmod.JournalMod;
import com.pycoder.journalmod.config.ModConfig;
import com.pycoder.journalmod.item.JournalBookItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JournalBookScreen extends Screen {

    public static final ResourceLocation BOOK_TEXTURE =
            new ResourceLocation(JournalMod.MOD_ID, "textures/gui/journal_book.png");

    private static final int BOOK_WIDTH = 256;
    private static final int BOOK_HEIGHT = 160;
    private static final int PAGE_WIDTH = 110;
    private static final int PAGE_HEIGHT = 130;
    private static final int LEFT_PAGE_X = 10;
    private static final int RIGHT_PAGE_X = 134;
    private static final int PAGE_Y = 10;
    private static final int PAGE_TEXT_X = 8;
    private static final int PAGE_TEXT_WIDTH = 92;
    private static final int TITLE_Y = 8;
    private static final int RULE_Y = 24;
    private static final int BODY_START_Y = 35;
    private static final int BODY_END_Y = 116;
    private static final int PAGE_NUMBER_Y = 121;
    private static final int LINE_STEP = 10;
    private static final int MENU_ROW_STEP = 12;
    private static final int MENU_ROWS_PER_PAGE = 7;
    private static final int BODY_LINES_PER_PAGE = ((BODY_END_Y - BODY_START_Y) / LINE_STEP) + 1;

    private float openProgress = 0.0f;
    private float openProgressO = 0.0f;
    private boolean closing = false;

    private final ItemStack bookStack;
    private final List<Integer> collectedPages;
    private List<DisplayPage> displayPages = List.of();
    private int currentSpread = 0;
    private int totalSpreads = 0;

    private Button forwardButton;
    private Button backButton;
    private Button closeButton;
    private Button menuButton;

    public JournalBookScreen(ItemStack bookStack) {
        super(GameNarrator.NO_TITLE);
        this.bookStack = bookStack;
        this.collectedPages = new ArrayList<>(JournalBookItem.getCollectedPages(bookStack));
    }

    private void calculateTotalSpreads() {
        this.totalSpreads = Math.max(1, (this.displayPages.size() + 1) / 2);
    }

    private List<DisplayPage> buildDisplayPages() {
        List<NumberedPageSource> numberedSources = buildNumberedPageSources();
        List<DisplayPage> numberedPages = buildNumberedPages(numberedSources);
        List<MenuEntry> menuEntries = buildMenuEntries(numberedSources);
        List<DisplayPage> coverPages = paginateTextPage(PageType.COVER, ModConfig.getCoverTitle(), "Adventure Log\n\nTurn the page >", false, 0, -1);
        List<DisplayPage> introductionPages = paginateTextPage(PageType.INTRODUCTION, "Introduction", ModConfig.getIntroduction(), false, 0, -1);
        int menuPageCount = Math.max(1, (menuEntries.size() + MENU_ROWS_PER_PAGE - 1) / MENU_ROWS_PER_PAGE);
        int numberedStartIndex = coverPages.size() + introductionPages.size() + menuPageCount;
        List<DisplayPage> menuPages = buildMenuPages(menuEntries, numberedStartIndex);

        List<DisplayPage> pages = new ArrayList<>();
        pages.addAll(coverPages);
        pages.addAll(introductionPages);
        pages.addAll(menuPages);
        pages.addAll(numberedPages);
        return pages;
    }

    private List<NumberedPageSource> buildNumberedPageSources() {
        List<NumberedPageSource> sources = new ArrayList<>();
        Set<Integer> collectedPageIds = new HashSet<>(this.collectedPages);

        for (ModConfig.ChapterEntry chapter : ModConfig.getChapters()) {
            List<ModConfig.PageEntry> collectedChapterPages = new ArrayList<>();
            for (ModConfig.PageEntry page : chapter.pages()) {
                if (collectedPageIds.contains(page.id())) {
                    collectedChapterPages.add(page);
                }
            }

            if (!collectedChapterPages.isEmpty()) {
                sources.add(new NumberedPageSource(PageType.CHAPTER, chapter.name(), chapter.introduction(), true));
                for (ModConfig.PageEntry page : collectedChapterPages) {
                    sources.add(new NumberedPageSource(PageType.CONTENT, page.name(), page.content(), false));
                }
            }
        }

        return sources;
    }

    private List<DisplayPage> buildNumberedPages(List<NumberedPageSource> sources) {
        List<DisplayPage> pages = new ArrayList<>();
        int pageNumber = 1;
        for (NumberedPageSource source : sources) {
            List<DisplayPage> sourcePages = paginateTextPage(source.type(), source.title(), source.content(), true, pageNumber, pages.size());
            pages.addAll(sourcePages);
            pageNumber += sourcePages.size();
        }
        return pages;
    }

    private List<MenuEntry> buildMenuEntries(List<NumberedPageSource> sources) {
        List<MenuEntry> entries = new ArrayList<>();
        int pageNumber = 1;
        int displayPageIndex = 0;
        for (NumberedPageSource source : sources) {
            int pageCount = paginateBodyLines(source.content()).size();
            entries.add(new MenuEntry(source.title(), source.chapter(), pageNumber, displayPageIndex));
            pageNumber += pageCount;
            displayPageIndex += pageCount;
        }
        return entries;
    }

    private List<DisplayPage> buildMenuPages(List<MenuEntry> entries, int numberedStartIndex) {
        List<DisplayPage> pages = new ArrayList<>();
        if (entries.isEmpty()) {
            pages.add(new DisplayPage(PageType.MENU, Component.translatable("gui.journalmod.menu"), List.of(), List.of(), false, 0));
            return pages;
        }

        for (int start = 0; start < entries.size(); start += MENU_ROWS_PER_PAGE) {
            List<MenuRow> rows = new ArrayList<>();
            int end = Math.min(entries.size(), start + MENU_ROWS_PER_PAGE);
            for (int i = start; i < end; i++) {
                MenuEntry entry = entries.get(i);
                int targetPageIndex = numberedStartIndex + entry.displayPageIndex();
                rows.add(new MenuRow(entry.title(), entry.chapter(), entry.pageNumber(), targetPageIndex / 2));
            }
            pages.add(new DisplayPage(PageType.MENU, Component.translatable("gui.journalmod.menu"), List.of(), rows, false, 0));
        }
        return pages;
    }

    private List<DisplayPage> paginateTextPage(PageType type, String title, String content, boolean numbered, int firstPageNumber, int sourcePageIndex) {
        List<List<FormattedCharSequence>> pages = paginateBodyLines(content);
        List<DisplayPage> displayPages = new ArrayList<>();
        Component titleComponent = type == PageType.CHAPTER
                ? Component.literal(title).withStyle(ChatFormatting.BOLD)
                : Component.literal(title);

        for (int i = 0; i < pages.size(); i++) {
            int pageNumber = numbered ? firstPageNumber + i : 0;
            Component pageTitle = i == 0 ? titleComponent : Component.literal(title + " " + (i + 1));
            displayPages.add(new DisplayPage(type, pageTitle, pages.get(i), List.of(), numbered, pageNumber));
        }
        return displayPages;
    }

    private List<List<FormattedCharSequence>> paginateBodyLines(String content) {
        List<FormattedCharSequence> lines = wrapText(content);
        List<List<FormattedCharSequence>> pages = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += BODY_LINES_PER_PAGE) {
            pages.add(new ArrayList<>(lines.subList(start, Math.min(lines.size(), start + BODY_LINES_PER_PAGE))));
        }
        if (pages.isEmpty()) {
            pages.add(List.of());
        }
        return pages;
    }

    private List<FormattedCharSequence> wrapText(String content) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String normalized = content.replace("\\n", "\n");
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(this.font.split(Component.literal(paragraph), PAGE_TEXT_WIDTH));
        }
        return lines;
    }

    @Override
    protected void init() {
        super.init();

        this.displayPages = buildDisplayPages();
        calculateTotalSpreads();

        int centerX = (this.width - BOOK_WIDTH) / 2;
        int centerY = (this.height - BOOK_HEIGHT) / 2;

        this.forwardButton = this.addRenderableWidget(
                Button.builder(Component.literal(">"), button -> pageForward())
                        .pos(centerX + BOOK_WIDTH - 26, centerY + BOOK_HEIGHT - 22)
                        .size(20, 20)
                        .build()
        );

        this.backButton = this.addRenderableWidget(
                Button.builder(Component.literal("<"), button -> pageBack())
                        .pos(centerX + 6, centerY + BOOK_HEIGHT - 22)
                        .size(20, 20)
                        .build()
        );

        this.closeButton = this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> closeBook())
                        .pos(this.width / 2 - 50, centerY + BOOK_HEIGHT + 8)
                        .size(100, 20)
                        .build()
        );

        this.menuButton = this.addRenderableWidget(
                Button.builder(Component.translatable("gui.journalmod.menu"), button -> goToFirstMenuPage())
                        .pos(centerX + BOOK_WIDTH / 2 - 20, centerY + 4)
                        .size(40, 16)
                        .build()
        );

        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        this.backButton.visible = this.currentSpread > 0;
        this.forwardButton.visible = this.currentSpread < this.totalSpreads - 1;
        this.menuButton.visible = currentSpreadHasReturnToMenuPage();
    }

    private boolean currentSpreadHasReturnToMenuPage() {
        DisplayPage leftPage = getPageAt(this.currentSpread * 2);
        DisplayPage rightPage = getPageAt(this.currentSpread * 2 + 1);
        return (leftPage != null && leftPage.showReturnToMenu()) || (rightPage != null && rightPage.showReturnToMenu());
    }

    private DisplayPage getPageAt(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= this.displayPages.size()) {
            return null;
        }
        return this.displayPages.get(pageIndex);
    }

    private void pageForward() {
        if (this.currentSpread < this.totalSpreads - 1) {
            this.currentSpread++;
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            updateButtonVisibility();
        }
    }

    private void pageBack() {
        if (this.currentSpread > 0) {
            this.currentSpread--;
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            updateButtonVisibility();
        }
    }

    private void goToFirstMenuPage() {
        for (int i = 0; i < this.displayPages.size(); i++) {
            if (this.displayPages.get(i).type() == PageType.MENU) {
                goToSpread(i / 2);
                return;
            }
        }
    }

    private void goToSpread(int spread) {
        if (spread >= 0 && spread < this.totalSpreads) {
            this.currentSpread = spread;
            this.minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            updateButtonVisibility();
        }
    }

    private void closeBook() {
        this.closing = true;
        this.minecraft.player.playSound(SoundEvents.BOOK_PUT, 1.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();

        this.openProgressO = this.openProgress;

        if (this.closing) {
            this.openProgress = Mth.clamp(this.openProgress - 0.1f, 0.0f, 1.0f);
            if (this.openProgress <= 0.0f) {
                this.minecraft.setScreen(null);
            }
        } else {
            this.openProgress = Mth.clamp(this.openProgress + 0.1f, 0.0f, 1.0f);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        renderBook(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderBook(GuiGraphics guiGraphics) {

        int centerX = (this.width - BOOK_WIDTH) / 2;
        int centerY = (this.height - BOOK_HEIGHT) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_TEXTURE);

        guiGraphics.blit(BOOK_TEXTURE, centerX, centerY, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);

        int leftPageIndex = this.currentSpread * 2;
        renderDisplayPage(guiGraphics, centerX + LEFT_PAGE_X, centerY + PAGE_Y, getPageAt(leftPageIndex));
        renderDisplayPage(guiGraphics, centerX + RIGHT_PAGE_X, centerY + PAGE_Y, getPageAt(leftPageIndex + 1));
    }

    private void renderDisplayPage(GuiGraphics guiGraphics, int x, int y, DisplayPage page) {
        if (page == null) {
            return;
        }

        renderTitle(guiGraphics, x, y, page.title(), page.type() == PageType.CHAPTER);
        if (page.type() == PageType.MENU) {
            renderMenuRows(guiGraphics, x, y, page.menuRows());
        } else {
            renderBodyLines(guiGraphics, x, y, page.lines());
        }
        if (page.numbered()) {
            renderPageNumber(guiGraphics, x, y, page.pageNumber());
        }
    }

    private void renderTitle(GuiGraphics guiGraphics, int x, int y, Component title, boolean bold) {
        Component renderedTitle = bold ? title.copy().withStyle(ChatFormatting.BOLD) : title;
        String titleText = fitText(renderedTitle.getString(), PAGE_TEXT_WIDTH);
        Component fittedTitle = bold ? Component.literal(titleText).withStyle(ChatFormatting.BOLD) : Component.literal(titleText);
        int titleWidth = this.font.width(fittedTitle);
        guiGraphics.drawString(this.font, fittedTitle,
                x + (PAGE_WIDTH - titleWidth) / 2, y + TITLE_Y, 0x8B4513, false);
        guiGraphics.hLine(x + PAGE_TEXT_X, x + PAGE_WIDTH - PAGE_TEXT_X, y + RULE_Y, 0x8B4513);
    }

    private void renderBodyLines(GuiGraphics guiGraphics, int x, int y, List<FormattedCharSequence> lines) {
        int lineY = y + BODY_START_Y;
        for (FormattedCharSequence line : lines) {
            if (lineY > y + BODY_END_Y) {
                break;
            }
            guiGraphics.drawString(this.font, line, x + PAGE_TEXT_X, lineY, 0x3E2723, false);
            lineY += LINE_STEP;
        }
    }

    private void renderMenuRows(GuiGraphics guiGraphics, int x, int y, List<MenuRow> rows) {
        int lineY = y + BODY_START_Y;
        for (MenuRow row : rows) {
            Component pageName = row.chapter()
                    ? Component.literal(fitText(row.title(), 68)).withStyle(ChatFormatting.BOLD)
                    : Component.literal("    " + fitText(row.title(), 54));
            String pageNumber = String.valueOf(row.pageNumber());
            guiGraphics.drawString(this.font, pageName, x + PAGE_TEXT_X, lineY, 0x5D4037, false);
            guiGraphics.drawString(this.font, pageNumber,
                    x + PAGE_WIDTH - PAGE_TEXT_X - this.font.width(pageNumber), lineY, 0x5D4037, false);
            lineY += MENU_ROW_STEP;
        }
    }

    private void renderPageNumber(GuiGraphics guiGraphics, int x, int y, int pageNumber) {
        String pageNumberText = String.valueOf(pageNumber);
        guiGraphics.drawString(this.font, pageNumberText,
                x + (PAGE_WIDTH - this.font.width(pageNumberText)) / 2,
                y + PAGE_NUMBER_Y, 0x5D4037, false);
    }

    private String fitText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "..";
        int length = text.length();
        while (length > 0 && this.font.width(text.substring(0, length) + ellipsis) > maxWidth) {
            length--;
        }
        return text.substring(0, length) + ellipsis;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
    
        int centerX = (this.width - BOOK_WIDTH) / 2;
            int centerY = (this.height - BOOK_HEIGHT) / 2;
            if (clickMenuPage(mouseX, mouseY, centerX + LEFT_PAGE_X, centerY + PAGE_Y, getPageAt(this.currentSpread * 2))) {
                return true;
            }
            if (clickMenuPage(mouseX, mouseY, centerX + RIGHT_PAGE_X, centerY + PAGE_Y, getPageAt(this.currentSpread * 2 + 1))) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickMenuPage(double mouseX, double mouseY, int pageX, int pageY, DisplayPage page) {
        if (page == null || page.type() != PageType.MENU) {
            return false;
        }

        int lineY = pageY + BODY_START_Y;
        for (MenuRow row : page.menuRows()) {
            if (mouseX >= pageX + PAGE_TEXT_X && mouseX <= pageX + PAGE_WIDTH - PAGE_TEXT_X
                    && mouseY >= lineY && mouseY <= lineY + 10) {
                goToSpread(row.targetSpread());
                return true;
            }
            lineY += MENU_ROW_STEP;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263) {
            pageBack();
            return true;
        } else if (keyCode == 262) {
            pageForward();
            return true;
        } else if (keyCode == 256) {
            closeBook();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum PageType {
        COVER,
        INTRODUCTION,
        MENU,
        CHAPTER,
        CONTENT
    }

    private record NumberedPageSource(PageType type, String title, String content, boolean chapter) {}

    private record MenuEntry(String title, boolean chapter, int pageNumber, int displayPageIndex) {}

    private record MenuRow(String title, boolean chapter, int pageNumber, int targetSpread) {}

    private record DisplayPage(PageType type, Component title, List<FormattedCharSequence> lines,
                               List<MenuRow> menuRows, boolean numbered, int pageNumber) {
        private boolean showReturnToMenu() {
            return this.type == PageType.CHAPTER || this.type == PageType.CONTENT;
        }
    }
}
