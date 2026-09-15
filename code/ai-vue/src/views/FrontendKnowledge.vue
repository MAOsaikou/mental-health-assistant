<template>
  <div class="knowledge page-shell">
    <header class="intro">
      <p class="page-kicker">慢慢看就好</p>
      <h1 class="page-title">读一点，心里会松一点</h1>
      <p class="page-desc">不必一次读完。选一篇对上此刻心情的，就够了。</p>
    </header>

    <div class="layout">
      <aside class="surface-card recommend">
        <p class="side-label">大家常看</p>
        <button
          v-for="(item, index) in recommendList"
          :key="item.id"
          type="button"
          class="rec-item"
          @click="goToArticle(item.id)"
        >
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
          <div>
            <strong>{{ item.title }}</strong>
            <em>读过 {{ item.readCount }}</em>
          </div>
        </button>
      </aside>

      <div class="list">
        <article
          v-for="item in articleList"
          :key="item.id"
          class="surface-card article"
          @click="goToArticle(item.id)"
        >
          <el-image class="cover" :src="getImage(item.coverImage)" fit="cover" />
          <div class="info">
            <div class="title-row">
              <h3>{{ item.title }}</h3>
              <el-tag effect="plain" round>{{ item.categoryName }}</el-tag>
            </div>
            <p>{{ item.authorName }} · {{ dayjs(item.updatedAt).format('YYYY年M月D日') }} · {{ item.readCount }} 人读过</p>
          </div>
        </article>
        <el-pagination
          class="pager"
          :page-size="pagination.size"
          layout="prev, pager, next"
          :total="pagination.total"
          @change="handleChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { dayjs } from 'element-plus'
import { ref, reactive, onMounted } from 'vue'
import { getKnowledgeList } from '@/api/frontend'
import { useRouter } from 'vue-router'
import { fileBaseUrl } from '@/config/index.js'
import iconUrl from '@/assets/images/book.svg'

const router = useRouter()
const recommendList = ref([])
const articleList = ref([])
const pagination = reactive({
  currentPage: 1,
  size: 10,
  total: 0,
})

const getPageList = () => {
  getKnowledgeList({
    sortField: 'publishedAt',
    sortDirection: 'desc',
    ...pagination,
  }).then((res) => {
    articleList.value = res.records
    pagination.total = res.total
  })
}

const getImage = (url) => (url ? fileBaseUrl + url : iconUrl)

const handleChange = (page) => {
  pagination.currentPage = page
  getPageList()
}

const goToArticle = (id) => {
  router.push(`/knowledge/article/${id}`)
}

onMounted(() => {
  getPageList()
  getKnowledgeList({
    sortField: 'readCount',
    sortDirection: 'desc',
    currentPage: 1,
    size: 5,
  }).then((res) => {
    recommendList.value = res.records
  })
})
</script>

<style scoped lang="scss">
.intro {
  margin-bottom: 28px;
}

.layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 20px;
}

.recommend {
  padding: 18px;
  height: fit-content;
}

.side-label {
  color: var(--muted);
  font-size: 13px;
  letter-spacing: 0.08em;
  margin-bottom: 12px;
}

.rec-item {
  width: 100%;
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: inherit;

  span {
    color: var(--glow);
    font-family: "Fraunces", Georgia, serif;
  }

  strong {
    display: block;
    font-size: 14px;
    line-height: 1.5;
  }

  em {
    font-style: normal;
    color: var(--muted);
    font-size: 12px;
  }
}

.article {
  display: flex;
  gap: 18px;
  padding: 16px;
  margin-bottom: 16px;
  cursor: pointer;
}

.cover {
  width: 180px;
  height: 120px;
  border-radius: 16px;
  flex-shrink: 0;
  background: var(--paper-deep);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;

  h3 {
    font-family: "Fraunces", Georgia, serif;
    font-size: 20px;
  }
}

.info p {
  margin-top: 10px;
  color: var(--muted);
  font-size: 13px;
}

.pager {
  justify-content: center;
  margin-top: 8px;
}

@media (max-width: 860px) {
  .layout,
  .article {
    grid-template-columns: 1fr;
    display: block;
  }

  .cover {
    width: 100%;
    height: 160px;
    margin-bottom: 12px;
  }
}
</style>
