package com.itmao.aispringboot.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itmao.aispringboot.DTO.command.ArticleCreateDTO;
import com.itmao.aispringboot.DTO.query.ArticleListQueryDTO;
import com.itmao.aispringboot.DTO.response.ArticleResponseDTO;
import com.itmao.aispringboot.entity.KnowledgeArticle;
import com.itmao.aispringboot.entity.KnowledgeCategory;
import com.itmao.aispringboot.entity.User;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.KnowledgeArticleMapper;
import com.itmao.aispringboot.mapper.UserMapper;
import com.itmao.aispringboot.util.HtmlContentSanitizer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeArticleService {
    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;
    @Resource
    private KnowledgeCategoryService knowledgeCategoryService;
    @Resource
    private UserMapper userMapper;

    public Page<ArticleResponseDTO> page(ArticleListQueryDTO query, boolean publishedOnly) {
        Page<KnowledgeArticle> page = new Page<>(query.resolveCurrent(), query.resolveSize());
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(query.getTitle()), KnowledgeArticle::getTitle, query.getTitle())
                .eq(query.getCategoryId() != null, KnowledgeArticle::getCategoryId, query.getCategoryId());
        if (publishedOnly) {
            wrapper.eq(KnowledgeArticle::getStatus, 1);
        } else if (query.getStatus() != null) {
            wrapper.eq(KnowledgeArticle::getStatus, query.getStatus());
        }
        if ("readCount".equals(query.getSortField())) {
            wrapper.orderBy(true, !"desc".equalsIgnoreCase(query.getSortDirection()), KnowledgeArticle::getReadCount);
        } else {
            wrapper.orderByDesc(KnowledgeArticle::getPublishedAt).orderByDesc(KnowledgeArticle::getUpdatedAt);
        }
        Page<KnowledgeArticle> entityPage = knowledgeArticleMapper.selectPage(page, wrapper);
        Page<ArticleResponseDTO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toResponse).toList());
        return result;
    }

    public ArticleResponseDTO detail(String id, boolean publishedOnly, boolean increaseRead) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        boolean published = Integer.valueOf(1).equals(article.getStatus());
        if (publishedOnly && !published) {
            throw new BusinessException("文章不存在");
        }
        if (increaseRead && published) {
            article.setReadCount((article.getReadCount() == null ? 0 : article.getReadCount()) + 1);
            knowledgeArticleMapper.updateById(article);
        }
        return toResponse(article);
    }

    public void create(Long authorId, ArticleCreateDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(StrUtil.isBlank(dto.getId()) ? UUID.randomUUID().toString() : dto.getId())
                .categoryId(dto.getCategoryId())
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .content(HtmlContentSanitizer.sanitize(dto.getContent()))
                .coverImage(dto.getCoverImage())
                .tags(dto.getTags())
                .authorId(authorId)
                .readCount(0)
                .status(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        knowledgeArticleMapper.insert(article);
    }

    public void update(String id, ArticleCreateDTO dto) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setCategoryId(dto.getCategoryId());
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(HtmlContentSanitizer.sanitize(dto.getContent()));
        article.setCoverImage(dto.getCoverImage());
        article.setTags(dto.getTags());
        article.setUpdatedAt(LocalDateTime.now());
        knowledgeArticleMapper.updateById(article);
    }

    public void updateStatus(String id, Integer status) {
        KnowledgeArticle article = knowledgeArticleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        article.setStatus(status);
        article.setUpdatedAt(LocalDateTime.now());
        if (status != null && status == 1) {
            article.setPublishedAt(LocalDateTime.now());
        }
        knowledgeArticleMapper.updateById(article);
    }

    public void delete(String id) {
        if (knowledgeArticleMapper.selectById(id) == null) {
            throw new BusinessException("文章不存在");
        }
        knowledgeArticleMapper.deleteById(id);
    }

    private ArticleResponseDTO toResponse(KnowledgeArticle article) {
        KnowledgeCategory category = knowledgeCategoryService.getById(article.getCategoryId());
        User author = article.getAuthorId() != null ? userMapper.selectById(article.getAuthorId()) : null;
        List<String> tagArray = StrUtil.isBlank(article.getTags())
                ? List.of()
                : Arrays.stream(article.getTags().split(",")).map(String::trim).filter(StrUtil::isNotBlank).toList();
        return ArticleResponseDTO.builder()
                .id(article.getId())
                .categoryId(article.getCategoryId())
                .categoryName(category != null ? category.getCategoryName() : "")
                .title(article.getTitle())
                .summary(article.getSummary())
                // 兼容数据库中的历史内容：返回前再清洗一次，避免旧脏数据直接到达客户端。
                .content(HtmlContentSanitizer.sanitize(article.getContent()))
                .coverImage(article.getCoverImage())
                .tags(article.getTags())
                .tagArray(tagArray)
                .authorId(article.getAuthorId())
                .authorName(author != null ? author.getDisplayName() : "")
                .readCount(article.getReadCount())
                .status(article.getStatus())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
