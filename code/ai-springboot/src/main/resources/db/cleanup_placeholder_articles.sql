-- 清理早期 SQL 种子里的 7 篇占位科普文。
-- 正常情况下不需要手动执行:后端启动时 ArticleMarkdownImporter 会自动清理
-- (由 app.articles.remove-placeholder 控制,默认开启)。
-- 此脚本仅供不启动后端、直接操作数据库时使用。
-- user_favorite 对 knowledge_article 是 ON DELETE CASCADE,相关收藏会一并删除。

DELETE FROM `knowledge_article` WHERE `id` IN (
  '550e8400-e29b-41d4-a716-446655440001',
  '550e8400-e29b-41d4-a716-446655440002',
  '550e8400-e29b-41d4-a716-446655440003',
  '550e8400-e29b-41d4-a716-446655440004',
  '550e8400-e29b-41d4-a716-446655440005',
  '550e8400-e29b-41d4-a716-446655440006',
  '550e8400-e29b-41d4-a716-446655440007'
);

-- 可选:旧的 4 个分类(心理健康基础/情绪管理/压力缓解/人际关系)清空后如不再使用,
-- 可执行下面的语句下架(status=0 不会在前台分类树中显示)。默认保留,不影响功能。
-- UPDATE `knowledge_category` SET `status` = 0 WHERE `id` IN (1, 2, 3, 4);
