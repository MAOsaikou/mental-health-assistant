package com.itmao.aispringboot.DTO.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleListQueryDTO extends PageQueryDTO {
    private String title;
    private Long categoryId;
    private Integer status;
    private String sortField;
    private String sortDirection;
}
