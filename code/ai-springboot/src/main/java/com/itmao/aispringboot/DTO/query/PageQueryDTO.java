package com.itmao.aispringboot.DTO.query;

import lombok.Data;

@Data
public class PageQueryDTO {
    private Integer currentPage;
    private Integer current;
    private Integer pageNum;
    private Integer size;
    private Integer pageSize;

    public int resolveCurrent() {
        if (currentPage != null && currentPage > 0) {
            return currentPage;
        }
        if (current != null && current > 0) {
            return current;
        }
        if (pageNum != null && pageNum > 0) {
            return pageNum;
        }
        return 1;
    }

    public int resolveSize() {
        if (size != null && size > 0) {
            return Math.min(size, 100);
        }
        if (pageSize != null && pageSize > 0) {
            return Math.min(pageSize, 100);
        }
        return 10;
    }
}
