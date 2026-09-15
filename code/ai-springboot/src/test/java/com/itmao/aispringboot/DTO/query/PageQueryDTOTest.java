package com.itmao.aispringboot.DTO.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryDTOTest {

    @Test
    void capsPageSizeToPreventOversizedQueries() {
        PageQueryDTO query = new PageQueryDTO();
        query.setPageSize(10_000);

        assertEquals(100, query.resolveSize());
    }
}
