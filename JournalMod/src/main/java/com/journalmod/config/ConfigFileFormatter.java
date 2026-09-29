package com.journalmod.config;

import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigFileFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileFormatter.class);
    private static final String CONFIG_FILE_NAME = "journalmod-common.toml";
    private static final String PAGE_CHAPTERS_KEY = "pageChapters";
    private static final String INDENT = "    ";

    private ConfigFileFormatter() {}

    public static void formatPageChapters() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        try {
            String content = Files.readString(configPath, StandardCharsets.UTF_8);
            String formatted = formatPageChapters(content);
            if (!content.equals(formatted)) {
                Files.writeString(configPath, formatted, StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to format journal mod config file", exception);
        }
    }

    static String formatPageChapters(String content) {
        int keyStart = content.indexOf(PAGE_CHAPTERS_KEY);
        if (keyStart < 0) {
            return content;
        }

        int valueStart = content.indexOf('[', keyStart);
        if (valueStart < 0) {
            return content;
        }

        int valueEnd = findMatchingBracket(content, valueStart);
        if (valueEnd < 0) {
            return content;
        }

        String formattedValue = formatArray(content.substring(valueStart, valueEnd + 1));
        return content.substring(0, valueStart) + formattedValue + content.substring(valueEnd + 1);
    }

    private static int findMatchingBracket(String content, int start) {
        boolean inQuotes = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
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
            if (inQuotes) {
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private static String formatArray(String value) {
        StringBuilder formatted = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;
        int depth = 0;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (escaped) {
                formatted.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                formatted.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                formatted.append(c);
                inQuotes = !inQuotes;
                continue;
            }
            if (inQuotes) {
                formatted.append(c);
                continue;
            }

            if (c == '[') {
                depth++;
                formatted.append('[').append(System.lineSeparator()).append(indent(depth));
            } else if (c == ']') {
                trimTrailingWhitespace(formatted);
                depth--;
                formatted.append(System.lineSeparator()).append(indent(depth)).append(']');
            } else if (c == ',') {
                formatted.append(',').append(System.lineSeparator()).append(indent(depth));
            } else if (!Character.isWhitespace(c)) {
                formatted.append(c);
            }
        }

        return formatted.toString();
    }

    private static String indent(int depth) {
        return INDENT.repeat(Math.max(0, depth + 1));
    }

    private static void trimTrailingWhitespace(StringBuilder builder) {
        while (!builder.isEmpty() && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.setLength(builder.length() - 1);
        }
    }
}
