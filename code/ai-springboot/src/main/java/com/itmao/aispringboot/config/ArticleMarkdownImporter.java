package com.itmao.aispringboot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itmao.aispringboot.entity.KnowledgeArticle;
import com.itmao.aispringboot.entity.KnowledgeCategory;
import com.itmao.aispringboot.mapper.KnowledgeArticleMapper;
import com.itmao.aispringboot.mapper.KnowledgeCategoryMapper;
import com.itmao.aispringboot.util.HtmlContentSanitizer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 启动时把 classpath:articles/*.md 里的文章同步进 knowledge_article 表。
 * 以后新增文章只需要在 resources/articles/ 下放一个带 front-matter 的 md 文件并重启。
 * 按 slug 生成确定性 ID 做幂等 upsert:内容变了才更新,阅读量和发布时间不受影响。
 */
@Slf4j
@Component
public class ArticleMarkdownImporter implements ApplicationRunner {

    /** 早期 SQL 种子里的 7 篇占位科普文,由真实内容替代后删除 */
    private static final List<String> PLACEHOLDER_ARTICLE_IDS = List.of(
            "550e8400-e29b-41d4-a716-446655440001",
            "550e8400-e29b-41d4-a716-446655440002",
            "550e8400-e29b-41d4-a716-446655440003",
            "550e8400-e29b-41d4-a716-446655440004",
            "550e8400-e29b-41d4-a716-446655440005",
            "550e8400-e29b-41d4-a716-446655440006",
            "550e8400-e29b-41d4-a716-446655440007");

    private static final long DEFAULT_AUTHOR_ID = 1L;
    private static final DateTimeFormatter PUBLISHED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Resource
    private KnowledgeArticleMapper knowledgeArticleMapper;
    @Resource
    private KnowledgeCategoryMapper knowledgeCategoryMapper;

    @Value("${app.articles.import-enabled:true}")
    private boolean importEnabled;
    @Value("${app.articles.remove-placeholder:true}")
    private boolean removePlaceholder;

    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    @Override
    public void run(ApplicationArguments args) {
        if (!importEnabled) {
            return;
        }
        try {
            importArticles();
            if (removePlaceholder) {
                removePlaceholderArticles();
            }
        } catch (Exception e) {
            // 导入失败不应阻断应用启动
            log.error("知识文章导入失败,本次跳过", e);
        }
    }

    private void importArticles() throws Exception {
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath:articles/*.md");
        if (resources.length == 0) {
            log.info("未找到 articles/*.md,跳过知识文章导入");
            return;
        }
        Map<String, KnowledgeCategory> categoryCache = new HashMap<>();
        knowledgeCategoryMapper.selectList(null)
                .forEach(c -> categoryCache.put(c.getCategoryName(), c));

        int created = 0, updated = 0, unchanged = 0;
        for (var resource : resources) {
            String raw;
            try (InputStream in = resource.getInputStream()) {
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            ParsedArticle parsed = parse(raw, resource.getFilename());
            if (parsed == null) {
                continue;
            }
            KnowledgeCategory category = resolveCategory(parsed, categoryCache);
            switch (upsert(parsed, category)) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case UNCHANGED -> unchanged++;
            }
        }
        log.info("知识文章导入完成:新增 {} 篇,更新 {} 篇,未变化 {} 篇", created, updated, unchanged);
    }

    private ParsedArticle parse(String raw, String filename) {
        String content = raw.replace("\r\n", "\n").trim();
        if (!content.startsWith("---")) {
            log.warn("文章 {} 缺少 front-matter,已跳过", filename);
            return null;
        }
        int end = content.indexOf("\n---", 3);
        if (end < 0) {
            log.warn("文章 {} 的 front-matter 未闭合,已跳过", filename);
            return null;
        }
        Map<String, Object> meta = new Yaml().load(content.substring(3, end));
        String body = content.substring(content.indexOf('\n', end + 1) + 1);

        ParsedArticle article = new ParsedArticle();
        article.slug = str(meta, "slug");
        article.title = str(meta, "title");
        article.category = str(meta, "category");
        article.categoryDescription = str(meta, "categoryDescription");
        article.summary = str(meta, "summary");
        article.tags = str(meta, "tags");
        article.initialReadCount = meta.get("readCount") instanceof Number n ? n.intValue() : 0;
        String publishedAt = str(meta, "publishedAt");
        article.publishedAt = publishedAt == null
                ? LocalDateTime.now()
                : LocalDateTime.parse(publishedAt, PUBLISHED_AT_FORMAT);
        if (article.slug == null || article.title == null || article.category == null) {
            log.warn("文章 {} 缺少 slug/title/category,已跳过", filename);
            return null;
        }
        article.contentHtml = HtmlContentSanitizer.sanitize(htmlRenderer.render(markdownParser.parse(body)));
        return article;
    }

    private KnowledgeCategory resolveCategory(ParsedArticle parsed, Map<String, KnowledgeCategory> cache) {
        KnowledgeCategory existing = cache.get(parsed.category);
        if (existing != null) {
            return existing;
        }
        int maxSort = cache.values().stream()
                .map(KnowledgeCategory::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        LocalDateTime now = LocalDateTime.now();
        KnowledgeCategory category = KnowledgeCategory.builder()
                .parentId(0L)
                .categoryName(parsed.category)
                .description(parsed.categoryDescription)
                .sortOrder(maxSort + 10)
                .status(1)
                .createdAt(now)
                .updatedAt(now)
                .build();
        knowledgeCategoryMapper.insert(category);
        cache.put(parsed.category, category);
        log.info("自动创建知识分类:{}", parsed.category);
        return category;
    }

    private UpsertResult upsert(ParsedArticle parsed, KnowledgeCategory category) {
        String id = UUID.nameUUIDFromBytes(("knowledge-article:" + parsed.slug).getBytes(StandardCharsets.UTF_8)).toString();
        KnowledgeArticle existing = knowledgeArticleMapper.selectById(id);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            KnowledgeArticle article = KnowledgeArticle.builder()
                    .id(id)
                    .categoryId(category.getId())
                    .title(parsed.title)
                    .summary(parsed.summary)
                    .content(parsed.contentHtml)
                    .coverImage("")
                    .tags(parsed.tags)
                    .authorId(DEFAULT_AUTHOR_ID)
                    .readCount(parsed.initialReadCount)
                    .status(1)
                    .publishedAt(parsed.publishedAt)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            knowledgeArticleMapper.insert(article);
            return UpsertResult.CREATED;
        }
        boolean changed = !Objects.equals(existing.getTitle(), parsed.title)
                || !Objects.equals(existing.getSummary(), parsed.summary)
                || !Objects.equals(existing.getContent(), parsed.contentHtml)
                || !Objects.equals(existing.getTags(), parsed.tags)
                || !Objects.equals(existing.getCategoryId(), category.getId());
        if (!changed) {
            return UpsertResult.UNCHANGED;
        }
        existing.setTitle(parsed.title);
        existing.setSummary(parsed.summary);
        existing.setContent(parsed.contentHtml);
        existing.setTags(parsed.tags);
        existing.setCategoryId(category.getId());
        existing.setUpdatedAt(now);
        knowledgeArticleMapper.updateById(existing);
        return UpsertResult.UPDATED;
    }

    private void removePlaceholderArticles() {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(KnowledgeArticle::getId, PLACEHOLDER_ARTICLE_IDS);
        Long count = knowledgeArticleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            knowledgeArticleMapper.delete(wrapper);
            log.info("已清理早期占位科普文 {} 篇", count);
        }
    }

    private static String str(Map<String, Object> meta, String key) {
        Object value = meta.get(key);
        return value == null ? null : value.toString().trim();
    }

    private enum UpsertResult { CREATED, UPDATED, UNCHANGED }

    private static class ParsedArticle {
        String slug;
        String title;
        String category;
        String categoryDescription;
        String summary;
        String tags;
        String contentHtml;
        Integer initialReadCount;
        LocalDateTime publishedAt;
    }
}
