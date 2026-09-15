<template>
  <div class="home">
    <section class="hero page-shell">
      <div class="hero-copy">
        <p class="page-kicker">一盏不催你的小灯</p>
        <h2 class="page-title">
          今天不容易，<br />
          <em>也没关系。</em>
        </h2>
        <p v-if="greeting" class="hello">{{ greeting }}</p>
        <p class="page-desc">
          小光会慢慢听。不必讲清楚，也不必马上好起来。先坐下，说一句心里的话就够了。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="goTalk">找小光聊聊</el-button>
          <el-button size="large" round class="soft-btn" @click="goDiary">记一笔心情</el-button>
        </div>
      </div>

      <div class="hero-light" aria-hidden="true">
        <div class="orb">
          <span>光</span>
        </div>
        <p>我在，不急。</p>
      </div>
    </section>

    <section v-if="token" class="mood-week page-shell">
      <div class="mood-card">
        <p class="page-kicker">这几天的心情</p>
        <h3>不是成绩，只是轻轻看一眼</h3>
        <div class="mood-days">
          <div v-for="point in trend" :key="point.date" class="mood-day">
            <span class="bar" :style="{ height: barHeight(point.moodScore) }"></span>
            <strong>{{ point.moodScore || '·' }}</strong>
            <em>{{ formatDay(point.date) }}</em>
          </div>
        </div>
        <p v-if="trend.length && !trend.some((item) => item.moodScore)" class="mood-empty">还没有记下过。想写的时候，去「心情」页就好。</p>
      </div>
    </section>

    <section class="invites page-shell">
      <article class="invite" @click="goTalk">
        <span class="invite-no">01</span>
        <h3>找人说说</h3>
        <p>像对一个温和的朋友讲话。没有标准答案，也不会催你振作。</p>
      </article>
      <article class="invite" @click="goDiary">
        <span class="invite-no">02</span>
        <h3>把心情放下</h3>
        <p>乱写也没关系。看见自己今天的情绪，本身就是一种照顾。</p>
      </article>
      <article class="invite" @click="$router.push('/knowledge')">
        <span class="invite-no">03</span>
        <h3>慢慢读一读</h3>
        <p>几段好懂的文字，陪你把心里那团雾，摊开一点点。</p>
      </article>
    </section>

    <section class="hold page-shell">
      <div class="hold-card">
        <p class="hold-title">在这里，你被允许慢慢来</p>
        <ul>
          <li>感受没有对错，低落也值得被看见</li>
          <li>小光是陪伴，不是诊断，更不会替你做决定</li>
          <li>若此刻很危险，请先求助：120 或 110；需要心理支持可拨 12356</li>
        </ul>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDiaryTrend } from '@/api/frontend'

const router = useRouter()
const token = computed(() => Boolean(localStorage.getItem('token')))
const trend = ref([])

const greeting = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const name = info.displayName || info.nickname || info.username
    if (!name) return ''
    const hour = new Date().getHours()
    const hello = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
    return `${hello}，${name}。你来了，就很好。`
  } catch {
    return ''
  }
})

const ensureAuth = (path) => {
  if (localStorage.getItem('token')) {
    router.push(path)
    return
  }
  router.push('/auth/login')
}

const goTalk = () => ensureAuth('/consultation')
const goDiary = () => ensureAuth('/emotion-diary')

const formatDay = (value) => {
  const date = new Date(value)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const barHeight = (score) => {
  if (!score) return '8px'
  return `${12 + score * 8}px`
}

onMounted(() => {
  if (!token.value) return
  getDiaryTrend(7).then((rows) => {
    trend.value = rows || []
  }).catch(() => {
    trend.value = []
  })
})
</script>

<style scoped lang="scss">
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(240px, 0.85fr);
  align-items: center;
  gap: 48px;
  padding-top: 48px;
}

.page-title em {
  color: var(--primary);
  font-style: italic;
  font-family: "Fraunces", Georgia, serif;
}

.hello {
  margin-top: 18px;
  color: var(--primary);
  font-size: 16px;
  font-weight: 600;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.soft-btn {
  background: var(--surface);
  border-color: var(--line);
  color: var(--ink);
}

.hero-light {
  display: grid;
  justify-items: center;
  gap: 16px;
  color: var(--muted);
  font-size: 14px;
}

.orb {
  width: 220px;
  height: 220px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 38% 32%, #fff6e4 0%, #f0d7a0 28%, #d4a056 58%, #2c6a5b 100%);
  box-shadow:
    0 30px 60px rgba(212, 160, 86, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  animation: glow-shift 6s ease-in-out infinite;

  span {
    color: #fffaf2;
    font-family: "Fraunces", Georgia, serif;
    font-size: 64px;
    text-shadow: 0 8px 24px rgba(36, 49, 44, 0.2);
  }
}

.invites {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  padding-top: 8px;
}

.invite {
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 24px;
  box-shadow: var(--shadow);
  cursor: pointer;
  transition: transform 0.25s ease, border-color 0.25s ease;

  &:hover {
    transform: translateY(-3px);
    border-color: #d8c7a8;
  }

  h3 {
    margin: 12px 0 8px;
    font-family: "Fraunces", Georgia, serif;
    font-size: 22px;
    font-weight: 600;
  }

  p {
    color: var(--muted);
    line-height: 1.75;
    font-size: 14px;
  }
}

.invite-no {
  color: var(--glow);
  font-size: 12px;
  letter-spacing: 0.16em;
}

.mood-card {
  padding: 24px 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 24px;
  box-shadow: var(--shadow);
}

.mood-card h3 {
  margin: 8px 0 18px;
  font-family: "Fraunces", Georgia, serif;
  font-size: 22px;
}

.mood-days {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  align-items: end;
  min-height: 120px;
}

.mood-day {
  display: grid;
  justify-items: center;
  gap: 6px;
  color: var(--muted);
  font-size: 12px;
}

.mood-day .bar {
  width: 14px;
  border-radius: 999px;
  background: var(--primary-soft);
  border: 1px solid var(--primary);
}

.mood-day strong {
  color: var(--ink);
  font-size: 13px;
}

.mood-day em {
  font-style: normal;
}

.mood-empty {
  margin-top: 12px;
  color: var(--muted);
  font-size: 14px;
}

.hold {
  padding-top: 8px;
}

.hold-card {
  padding: 28px 32px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(215, 235, 227, 0.7), rgba(244, 226, 192, 0.55));
  border: 1px solid rgba(228, 217, 200, 0.9);
}

.hold-title {
  font-family: "Fraunces", Georgia, serif;
  font-size: 22px;
  margin-bottom: 14px;
}

.hold-card li {
  color: var(--muted);
  line-height: 1.9;
  padding-left: 18px;
  position: relative;
}

.hold-card li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0.7em;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
}

@media (max-width: 860px) {
  .hero,
  .invites {
    grid-template-columns: 1fr;
  }

  .orb {
    width: 160px;
    height: 160px;

    span {
      font-size: 48px;
    }
  }
}
</style>
