<template>
  <div class="article page-shell">
    <button class="back" type="button" @click="$router.push('/knowledge')">← 回到读一读</button>
    <article class="surface-card paper">
      <p class="meta">
        <el-tag effect="plain" round>{{ articleDetail.categoryName }}</el-tag>
        <span>{{ dayjs(articleDetail.updatedAt).format('YYYY年M月D日') }}</span>
      </p>
      <h1>{{ articleDetail.title }}</h1>
      <p v-if="articleDetail.summary" class="summary">{{ articleDetail.summary }}</p>
      <p class="byline">{{ articleDetail.authorName }} · {{ articleDetail.readCount }} 人轻轻读过</p>
      <div class="body" v-html="formatContent(articleDetail.content)"></div>
      <div v-if="articleDetail.tagArray?.length" class="tags">
        <el-tag v-for="tag in articleDetail.tagArray" :key="tag" effect="plain" round>{{ tag }}</el-tag>
      </div>
      <div class="after">
        <p>读完了的话，可以把这篇放心里，去和小光聊聊。</p>
        <el-button type="primary" round @click="$router.push('/consultation')">去找小光</el-button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeDetail } from '@/api/frontend'
import { dayjs } from 'element-plus'
import { sanitizeHtml } from '@/utils/sanitize'

const props = defineProps({
  id: String,
})

const articleDetail = ref({})

const formatContent = (content) => {
  if (!content) return ''
  return sanitizeHtml(content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>'))
}

onMounted(() => {
  getKnowledgeDetail(props.id).then((res) => {
    articleDetail.value = res
  })
})
</script>

<style scoped lang="scss">
.back {
  border: 0;
  background: transparent;
  color: var(--muted);
  margin-bottom: 16px;
  cursor: pointer;
}

.paper {
  padding: 36px 40px 32px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--muted);
  font-size: 13px;
}

h1 {
  margin: 18px 0 12px;
  font-family: "Fraunces", Georgia, serif;
  font-size: clamp(28px, 4vw, 36px);
  line-height: 1.35;
}

.summary {
  padding: 14px 16px;
  border-left: 3px solid var(--glow);
  background: rgba(244, 226, 192, 0.45);
  border-radius: 0 12px 12px 0;
  line-height: 1.8;
  color: #5c5346;
}

.byline {
  margin: 16px 0 24px;
  color: var(--muted);
  font-size: 13px;
}

.body {
  font-size: 16px;
  line-height: 1.9;
  color: #314039;

  :deep(p) {
    margin-bottom: 16px;
  }

  :deep(h2),
  :deep(h3) {
    margin: 24px 0 10px;
    font-family: "Fraunces", Georgia, serif;
  }
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 28px;
}

.after {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 28px;
  padding-top: 18px;
  border-top: 1px dashed var(--line);
  color: var(--muted);
}

@media (max-width: 720px) {
  .paper {
    padding: 22px;
  }

  .after {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
