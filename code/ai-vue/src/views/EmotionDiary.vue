<template>
  <div class="diary page-shell">
    <header class="intro">
      <p class="page-kicker">没有对错的一页</p>
      <h1 class="page-title">把今天的心情，轻轻放下</h1>
      <p class="page-desc">{{ todayText }}。分数不是成绩，情绪也不是需要被改正的东西。</p>
    </header>

    <template v-if="!todayEntry">
    <section class="surface-card block">
      <h2>今天整体感觉怎么样？</h2>
      <p class="hint">1 到 10，低一些也完全可以。</p>
      <el-rate
        v-model="diaryForm.moodScore"
        :texts="emotionStatus"
        show-texts
        :max="10"
        size="large"
      />
    </section>

    <section class="surface-card block">
      <h2>更接近哪一种？</h2>
      <div class="emotion-grid">
        <button
          v-for="emotion in emotionOptions"
          :key="emotion.name"
          type="button"
          class="emotion"
          :class="{ selected: emotion.name === diaryForm.dominantEmotion }"
          @click="selectEmotion(emotion.name)"
        >
          <el-image :src="emotion.url" class="face" />
          <span>{{ emotion.name }}</span>
        </button>
      </div>
    </section>

    <section class="surface-card block">
      <h2>如果愿意，再写一点</h2>
      <label>可能触动你的事</label>
      <el-input v-model="diaryForm.emotionTriggers" placeholder="今天什么事情，轻轻碰了一下你的心情？" type="textarea" :rows="3" maxlength="1000" show-word-limit />
      <label>此刻想留下的话</label>
      <el-input v-model="diaryForm.diaryContent" placeholder="随便写，乱也没关系。心里是什么味道，都可以放在这里。" type="textarea" :rows="5" maxlength="2000" show-word-limit />
      <div class="life">
        <div>
          <label>睡眠</label>
          <el-select v-model="diaryForm.sleepQuality" placeholder="昨晚睡得怎样" style="width: 100%">
            <el-option label="很差" :value="1" />
            <el-option label="较差" :value="2" />
            <el-option label="一般" :value="3" />
            <el-option label="良好" :value="4" />
            <el-option label="很好" :value="5" />
          </el-select>
        </div>
        <div>
          <label>压力</label>
          <el-select v-model="diaryForm.stressLevel" placeholder="今天压得重不重" style="width: 100%">
            <el-option label="很轻" :value="1" />
            <el-option label="较轻" :value="2" />
            <el-option label="中等" :value="3" />
            <el-option label="较重" :value="4" />
            <el-option label="很重" :value="5" />
          </el-select>
        </div>
      </div>
      <div class="actions">
        <el-button round @click="resetForm">清空重写</el-button>
        <el-button type="primary" round @click="submit">记下今天</el-button>
      </div>
    </section>
    </template>

    <section v-else class="surface-card block today-done">
      <h2>今天已经轻轻放下过一笔</h2>
      <p class="hint">{{ formatDate(todayEntry.diaryDate) }} · {{ todayEntry.dominantEmotion || '心情' }} · {{ todayEntry.moodScore }} 分</p>
      <p v-if="todayEntry.diaryContent" class="today-text">{{ todayEntry.diaryContent }}</p>
      <p v-else class="hint">那天只留下了分数，也很好。</p>
      <el-button round @click="$router.push('/consultation')">想多说几句，去找小光</el-button>
    </section>

    <section v-if="history.length" class="surface-card block">
      <h2>以前放下的心情</h2>
      <p class="hint">不是成绩单，只是你走过的日子。</p>
      <button
        v-for="item in history"
        :key="item.id"
        type="button"
        class="history-item"
        :class="{ open: openId === item.id }"
        @click="openId = openId === item.id ? null : item.id"
      >
        <div class="history-top">
          <strong>{{ formatDate(item.diaryDate) }}</strong>
          <span>{{ item.dominantEmotion || '心情' }} · {{ item.moodScore }} 分</span>
        </div>
        <p class="preview">{{ item.diaryContent || item.emotionTriggers || '只留下了分数' }}</p>
        <div v-if="openId === item.id" class="history-detail">
          <p v-if="item.emotionTriggers"><em>触动你的事</em>{{ item.emotionTriggers }}</p>
          <p v-if="item.sleepQuality">睡眠 {{ item.sleepQuality }}/5 · 压力 {{ item.stressLevel || '-' }}/5</p>
        </div>
      </button>
    </section>
  </div>
</template>

<script setup>
import { dayjs, ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { addEmotionDiary, getMyDiaries } from '@/api/frontend'

const emotionStatus = ['很沉', '很低落', '有些烦', '不太舒服', '还平静', '还轻松', '挺舒服', '很满足', '很高兴', '很明亮']
const todayText = dayjs().format('M月D日')

const emotionOptions = [
  { name: '开心', url: new URL('@/assets/images/开心.svg', import.meta.url).href },
  { name: '平静', url: new URL('@/assets/images/平静.svg', import.meta.url).href },
  { name: '焦虑', url: new URL('@/assets/images/焦虑.svg', import.meta.url).href },
  { name: '悲伤', url: new URL('@/assets/images/悲伤.svg', import.meta.url).href },
  { name: '兴奋', url: new URL('@/assets/images/兴奋.svg', import.meta.url).href },
  { name: '疲惫', url: new URL('@/assets/images/疲惫.svg', import.meta.url).href },
  { name: '惊讶', url: new URL('@/assets/images/惊讶.svg', import.meta.url).href },
  { name: '困惑', url: new URL('@/assets/images/困惑.svg', import.meta.url).href },
]

const todayIso = dayjs().format('YYYY-MM-DD')
const history = ref([])
const openId = ref(null)

const diaryForm = reactive({
  diaryDate: todayIso,
  moodScore: null,
  dominantEmotion: '',
  emotionTriggers: '',
  diaryContent: '',
  sleepQuality: null,
  stressLevel: null,
})

const todayEntry = computed(() =>
  history.value.find((item) => String(item.diaryDate).slice(0, 10) === todayIso),
)

const formatDate = (value) => dayjs(value).format('M月D日')

const loadHistory = () => {
  getMyDiaries().then((rows) => {
    history.value = rows || []
  })
}

const selectEmotion = (emotion) => {
  diaryForm.dominantEmotion = emotion
}

const resetForm = () => {
  Object.assign(diaryForm, {
    diaryDate: todayIso,
    moodScore: null,
    dominantEmotion: '',
    emotionTriggers: '',
    diaryContent: '',
    sleepQuality: null,
    stressLevel: null,
  })
}

const submit = () => {
  if (!diaryForm.moodScore) {
    ElMessage.error('先轻轻点一下今天的心情分数吧')
    return
  }
  addEmotionDiary(diaryForm).then(() => {
    ElMessage.success('记下了。今天你也辛苦了。')
    resetForm()
    loadHistory()
  })
}

onMounted(loadHistory)
</script>

<style scoped lang="scss">
.intro {
  margin-bottom: 24px;
}

.block {
  padding: 24px;
  margin-bottom: 18px;

  h2 {
    font-family: "Fraunces", Georgia, serif;
    font-size: 22px;
    margin-bottom: 8px;
  }
}

.hint {
  color: var(--muted);
  margin-bottom: 16px;
  font-size: 14px;
}

.emotion-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.emotion {
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 16px 8px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fff;
  cursor: pointer;
  color: var(--ink);

  &.selected {
    border-color: var(--primary);
    background: var(--primary-soft);
  }
}

.face {
  width: 44px;
  height: 44px;
}

label {
  display: block;
  margin: 16px 0 8px;
  color: var(--ink);
  font-size: 14px;
}

.life {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 8px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 24px;
}

.today-text {
  line-height: 1.8;
  margin: 12px 0 18px;
  white-space: pre-wrap;
}

.history-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 14px 16px;
  margin-top: 10px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fff;
  color: var(--ink);
  cursor: pointer;
}

.history-item.open {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.history-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
}

.preview {
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-detail {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink);
}

.history-detail em {
  display: block;
  color: var(--muted);
  font-style: normal;
  margin-bottom: 4px;
}

@media (max-width: 720px) {
  .emotion-grid,
  .life {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
