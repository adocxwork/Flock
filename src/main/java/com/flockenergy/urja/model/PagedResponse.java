package com.flockenergy.urja.model;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast
) {
    public static <T> PagedResponse<T> of(List<T> allItems, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int total = allItems.size();
        int totalPages = (int) Math.ceil((double) total / safeSize);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = (safePage - 1) * safeSize;
        if (fromIndex >= total) {
            return new PagedResponse<>(List.of(), safePage, safeSize, total, totalPages, safePage == 1, true);
        }

        int toIndex = Math.min(fromIndex + safeSize, total);
        List<T> pageContent = allItems.subList(fromIndex, toIndex);
        boolean isFirst = (safePage == 1);
        boolean isLast = (safePage >= totalPages);

        return new PagedResponse<>(pageContent, safePage, safeSize, total, totalPages, isFirst, isLast);
    }
}
