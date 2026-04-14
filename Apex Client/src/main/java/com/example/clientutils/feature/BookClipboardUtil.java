package com.example.clientutils.feature;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public final class BookClipboardUtil {
    private static final int MAX_PAGES = 100;
    private static final int MAX_CHARS_PER_PAGE = 1024;

    private BookClipboardUtil() {
    }

    public static List<String> parseClipboardToPages(String clipboard) {
        if (clipboard == null || clipboard.isBlank()) {
            return List.of("");
        }

        List<String> parsed = parseJsonArray(clipboard);
        if (parsed != null && !parsed.isEmpty()) {
            return trimToLimits(parsed);
        }

        if (clipboard.contains("\f")) {
            String[] split = clipboard.split("\\f", -1);
            List<String> pages = new ArrayList<>(split.length);
            for (String entry : split) {
                pages.add(entry);
            }
            return trimToLimits(pages);
        }

        return chunkIntoPages(clipboard);
    }

    private static List<String> parseJsonArray(String clipboard) {
        try {
            JsonElement root = new Gson().fromJson(clipboard, JsonElement.class);
            if (root == null || !root.isJsonArray()) {
                return null;
            }

            JsonArray array = root.getAsJsonArray();
            List<String> pages = new ArrayList<>(array.size());
            for (JsonElement page : array) {
                pages.add(page.isJsonNull() ? "" : page.getAsString());
            }
            return pages;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<String> chunkIntoPages(String text) {
        List<String> pages = new ArrayList<>();
        int idx = 0;

        while (idx < text.length() && pages.size() < MAX_PAGES) {
            int end = Math.min(idx + MAX_CHARS_PER_PAGE, text.length());
            pages.add(text.substring(idx, end));
            idx = end;
        }

        if (pages.isEmpty()) {
            pages.add("");
        }

        return pages;
    }

    private static List<String> trimToLimits(List<String> pages) {
        List<String> out = new ArrayList<>();
        for (String page : pages) {
            if (out.size() >= MAX_PAGES) {
                break;
            }

            String safe = page == null ? "" : page;
            while (safe.length() > MAX_CHARS_PER_PAGE && out.size() < MAX_PAGES) {
                out.add(safe.substring(0, MAX_CHARS_PER_PAGE));
                safe = safe.substring(MAX_CHARS_PER_PAGE);
            }

            if (out.size() < MAX_PAGES) {
                out.add(safe);
            }
        }

        if (out.isEmpty()) {
            out.add("");
        }

        return out;
    }
}
