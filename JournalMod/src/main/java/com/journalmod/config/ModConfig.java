package com.journalmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ConfigValue<String> JOURNAL_BOOK_NAME;
    public static final ConfigValue<String> COVER_TITLE;
    public static final ConfigValue<String> INTRODUCTION_TEXT;
    public static final ConfigValue<List<? extends Object>> PAGE_CHAPTERS;

    static {
        BUILDER.push("General Settings");

        JOURNAL_BOOK_NAME = BUILDER
                .comment("Default name of the journal book in game")
                .define("journalBookName", "Journal Book");

        COVER_TITLE = BUILDER
                .comment("Title shown on the journal book cover")
                .define("coverTitle", "Adventure Log");

        INTRODUCTION_TEXT = BUILDER
                .comment("Journal introduction text (use \\n for line breaks)")
                .define("introductionText", "This is a journal that records your adventures.\\nCollect the scattered pages found across the world to unlock more content.\\n\\nGood luck, adventurer!");

        BUILDER.pop();

        BUILDER.push("Page Settings");

        List<Object> defaultChapters = new ArrayList<>();

        List<String> chapterOne = new ArrayList<>();
        chapterOne.add("\"Departure\":\"Your adventure begins in a small village.\\nWith hope for the future, you set out on an unknown journey.\\nWhat awaits you ahead?\"");
        chapterOne.add("\"Exploration\":\"You crossed dense forests and climbed steep mountains.\\nThis world is far larger than you imagined.\\nEvery corner may hide a surprise.\"");
        defaultChapters.add("\"Beginnings\":\"Every journey starts with a single step.\\nThis chapter records the first traces of your adventure.\"");
        defaultChapters.add(chapterOne);

        List<String> chapterTwo = new ArrayList<>();
        chapterTwo.add("\"Discovery\":\"During an unexpected exploration, you discovered an ancient ruin.\\nThe murals on the walls tell stories from long ago.\\nWhat once happened here?\"");
        chapterTwo.add("\"Challenge\":\"The road of adventure is not always smooth.\\nYou faced powerful enemies and endured difficult battles.\\nBut these trials made you stronger.\"");
        chapterTwo.add("\"Growth\":\"After countless hardships, you are no longer the inexperienced novice you once were.\\nYour skills have grown steadily,\\nand your name has begun to spread far and wide.\"");
        defaultChapters.add("\"Trials\":\"The world begins to reveal its dangers and secrets.\\nThese pages record the challenges that shaped you.\"");
        defaultChapters.add(chapterTwo);

        List<String> chapterThree = new ArrayList<>();
        chapterThree.add("\"Secrets\":\"In a hidden corner of the world, you uncovered an astonishing secret.\\nThis secret may change everything.\\nAre you ready to face the truth?\"");
        chapterThree.add("\"Legend\":\"An ancient legend spoke of a relic.\\nIt is said to hold the power to change the world.\\nYou decide to search for this legendary treasure.\"");
        chapterThree.add("\"Finale\":\"Your adventure has finally reached its end.\\nBut this is not the end; it is a new beginning.\\nBecause a true adventure never stops.\"");
        defaultChapters.add("\"Legends\":\"Old stories point toward a truth hidden beyond the horizon.\\nThis chapter gathers the final pieces of the tale.\"");
        defaultChapters.add(chapterThree);

        PAGE_CHAPTERS = BUILDER
                .comment("Journal pages grouped by chapter.",
                        "The outer list alternates between chapter info and chapter page lists.",
                        "Each chapter info entry uses the format \"Chapter Name\":\"Chapter Introduction\".",
                        "Each following inner list contains the pages for that chapter.",
                        "Each page entry uses the format \"Page Name\":\"Page Content\".",
                        "Use \\n for line breaks in chapter introductions and page content.")
                .defineList("pageChapters", defaultChapters, ModConfig::isChapterEntry);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static int getPageCount() {
        return getPages().size();
    }

    public static String getPageName(int id) {
        PageEntry page = getPage(id);
        if (page != null) {
            return page.name();
        }
        return "Unknown Page #" + id;
    }

    public static String getPageContent(int id) {
        PageEntry page = getPage(id);
        if (page != null) {
            return page.content().replace("\\n", "\n");
        }
        return "The contents of this page have been lost...";
    }

    public static List<ChapterEntry> getChapters() {
        List<ChapterEntry> chapters = new ArrayList<>();
        ParsedPage currentChapter = null;
        int pageId = 1;

        for (Object chapterConfig : PAGE_CHAPTERS.get()) {
            if (chapterConfig instanceof String chapterInfo) {
                currentChapter = parsePageConfig(chapterInfo);
                continue;
            }
            if (!(chapterConfig instanceof List<?> pageConfigs)) {
                continue;
            }

            List<PageEntry> pages = new ArrayList<>();
            for (Object pageConfig : pageConfigs) {
                if (!(pageConfig instanceof String pageText)) {
                    continue;
                }
                ParsedPage parsed = parsePageConfig(pageText);
                pages.add(new PageEntry(pageId, parsed.name(), parsed.content()));
                pageId++;
            }

            String chapterName = currentChapter != null ? currentChapter.name() : "Chapter " + (chapters.size() + 1);
            String chapterIntroduction = currentChapter != null ? currentChapter.content() : "";
            chapters.add(new ChapterEntry(chapters.size() + 1, chapterName, chapterIntroduction, pages));
            currentChapter = null;
        }

        return chapters;
    }

    public static List<PageEntry> getPages() {
        List<PageEntry> pages = new ArrayList<>();
        for (ChapterEntry chapter : getChapters()) {
            pages.addAll(chapter.pages());
        }
        return pages;
    }

    public static int getChapterNumberForPage(int pageId) {
        for (ChapterEntry chapter : getChapters()) {
            for (PageEntry page : chapter.pages()) {
                if (page.id() == pageId) {
                    return chapter.number();
                }
            }
        }
        return 1;
    }

    public static String getIntroduction() {
        return INTRODUCTION_TEXT.get().replace("\\n", "\n");
    }

    public static String getCoverTitle() {
        return COVER_TITLE.get();
    }

    public static String getJournalBookName() {
        return JOURNAL_BOOK_NAME.get();
    }

    private static PageEntry getPage(int id) {
        for (PageEntry page : getPages()) {
            if (page.id() == id) {
                return page;
            }
        }
        return null;
    }

    private static boolean isChapterEntry(Object obj) {
        if (obj instanceof String) {
            return true;
        }
        if (!(obj instanceof List<?> list)) {
            return false;
        }
        for (Object entry : list) {
            if (!(entry instanceof String)) {
                return false;
            }
        }
        return true;
    }

    private static ParsedPage parsePageConfig(String pageConfig) {
        String entry = pageConfig.trim();
        int separator = findEntrySeparator(entry);
        if (separator < 0) {
            return new ParsedPage(entry, "");
        }

        String name = unquote(entry.substring(0, separator).trim());
        String content = unquote(entry.substring(separator + 1).trim());
        return new ParsedPage(name, content);
    }

    private static int findEntrySeparator(String entry) {
        boolean inQuotes = false;
        boolean escaped = false;

        for (int i = 0; i < entry.length(); i++) {
            char c = entry.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (c == ':' && !inQuotes) {
                return i;
            }
        }

        return -1;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    public record ChapterEntry(int number, String name, String introduction, List<PageEntry> pages) {}

    public record PageEntry(int id, String name, String content) {}

    private record ParsedPage(String name, String content) {}
}
