package com.itmao.aispringboot.controller;

import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.entity.KnowledgeCategory;
import com.itmao.aispringboot.service.KnowledgeCategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/category")
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService knowledgeCategoryService;

    public KnowledgeCategoryController(KnowledgeCategoryService knowledgeCategoryService) {
        this.knowledgeCategoryService = knowledgeCategoryService;
    }

    @GetMapping("/tree")
    public Result<List<KnowledgeCategory>> tree() {
        return Result.success(knowledgeCategoryService.tree());
    }
}
