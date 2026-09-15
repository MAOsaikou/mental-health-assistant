package com.itmao.aispringboot.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleCreateDTO {
    private String id;
    @NotBlank(message = "文章标题不能为空")
    private String title;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    private String summary;
    @NotBlank(message = "文章内容不能为空")
    private String content;
    private String coverImage;
    private String tags;
}
