package com.kangyoon.community.global.common;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private boolean first;
    private boolean last;
    private int pageNumber;

    public static <T> PageResponse<T> from(Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.content = page.getContent();
        response.totalPages = page.getTotalPages();
        response.totalElements = page.getTotalElements();
        response.first = page.isFirst();
        response.last = page.isLast();
        response.pageNumber = page.getNumber();
        return response;
    }

}
