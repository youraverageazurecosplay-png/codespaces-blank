package com.example.clientutils.feature;

import java.util.ArrayList;
import java.util.List;

public final class BookbanWriterUtil {
    private static final int PAGE_COUNT = 100;
    private static final int CHARS_PER_PAGE = 1024;

    private BookbanWriterUtil() {
    }

    public static List<String> generateHeavyPages() {
        List<String> pages = new ArrayList<>(PAGE_COUNT);
        for (int i = 0; i < PAGE_COUNT; i++) {
            StringBuilder builder = new StringBuilder(CHARS_PER_PAGE);
            int color = i % 10;
            while (builder.length() < CHARS_PER_PAGE - 2) {
                builder.append('\u00a7').append(color);
                builder.append((char) ('a' + (builder.length() % 26)));
            }
            pages.add(builder.substring(0, CHARS_PER_PAGE));
        }
        return pages;
    }
}

