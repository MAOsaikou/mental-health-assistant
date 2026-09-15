package com.itmao.aispringboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itmao.aispringboot.DTO.command.ArticleCreateDTO;
import com.itmao.aispringboot.DTO.command.ArticleStatusUpdateDTO;
import com.itmao.aispringboot.DTO.query.ArticleListQueryDTO;
import com.itmao.aispringboot.DTO.response.ArticleResponseDTO;
import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.service.KnowledgeArticleService;
import com.itmao.aispringboot.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge/article")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    public KnowledgeArticleController(KnowledgeArticleService knowledgeArticleService) {
        this.knowledgeArticleService = knowledgeArticleService;
    }

    @GetMapping("/page")
    public Result<Page<ArticleResponseDTO>> page(ArticleListQueryDTO query) {
        boolean publishedOnly = !JwtTokenUtil.isCurrentAdmin();
        return Result.success(knowledgeArticleService.page(query, publishedOnly));
    }

    @GetMapping("/{id}")
    public Result<ArticleResponseDTO> detail(@PathVariable String id) {
        boolean publishedOnly = !JwtTokenUtil.isCurrentAdmin();
        return Result.success(knowledgeArticleService.detail(id, publishedOnly, publishedOnly));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ArticleCreateDTO dto) {
        knowledgeArticleService.create(JwtTokenUtil.getCurrentUserId(), dto);
        return Result.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @Valid @RequestBody ArticleCreateDTO dto) {
        knowledgeArticleService.update(id, dto);
        return Result.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable String id, @Valid @RequestBody ArticleStatusUpdateDTO dto) {
        knowledgeArticleService.updateStatus(id, dto.getStatus());
        return Result.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeArticleService.delete(id);
        return Result.success();
    }
}
