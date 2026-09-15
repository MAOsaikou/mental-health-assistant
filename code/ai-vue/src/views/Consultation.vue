<template>
  <div class="consultation">
    <aside class="side">
      <div class="presence surface-card">
        <div class="breathing-circle">光</div>
        <h3>小光</h3>
        <p class="online"><span></span>我在听，不着急</p>
      </div>

      <div class="garden surface-card">
        <p class="side-label">此刻的感觉</p>
        <div class="emotion-orb">
          <strong>{{ currentEmotion.primaryEmotion || '平静' }}</strong>
          <span>被看见就好</span>
        </div>
        <p class="emotion-status">
          今天感觉
          <em>{{ currentEmotion.isNegative ? '需要被轻轻接住' : '还算安稳' }}</em>
        </p>
        <div class="intensity">
          <span v-for="dot in 3" :key="dot" class="dot" :class="{ active: getIntensityClass(currentEmotion.emotionScore) >= dot }"></span>
          <span>{{ getRiskText(currentEmotion.riskLevel) }}</span>
        </div>
        <div v-if="currentEmotion.suggestion" class="tip">
          <p>给你的一句</p>
          <span>{{ currentEmotion.suggestion }}</span>
        </div>
        <div v-if="currentEmotion.improvementSuggestions?.length" class="actions">
          <p>可以试试</p>
          <div v-for="action in currentEmotion.improvementSuggestions" :key="action" class="action">{{ action }}</div>
        </div>
        <div v-if="currentEmotion.isNegative && currentEmotion.riskLevel > 1" class="risk">
          <p>我想陪你先安全</p>
          <span>{{ currentEmotion.riskDescription }}</span>
          <strong>全国心理援助热线 12356 · 有立即危险请打 120 或 110</strong>
        </div>
      </div>

      <div class="history surface-card">
        <p class="side-label">以前的聊天</p>
        <p v-if="sessionList.length === 0" class="empty">还没有聊过。从一句「今天有点累」开始也很好。</p>
        <button
          v-for="session in sessionList"
          :key="session.id"
          type="button"
          class="session"
          :class="{ active: isActiveSession(session) }"
          @click="handleSessionClick(session)"
        >
          <div class="session-top">
            <strong>{{ session.sessionTitle }}</strong>
            <el-button text type="danger" @click.stop="handleDeleteSession(session.id)">
              <el-icon><DeleteFilled /></el-icon>
            </el-button>
          </div>
          <span class="meta">{{ session.startedAt }}</span>
          <span class="preview">{{ session.lastMessageContent }}</span>
        </button>
      </div>
    </aside>

    <section class="chat surface-card">
      <header class="chat-head">
        <div>
          <h2>和小光说说话</h2>
          <p>陪你把心里那句话，轻轻放下来</p>
        </div>
        <div class="head-actions">
          <router-link v-if="needOwnKey" class="quota warn" to="/profile">
            {{ quota.value.remainingCount <= 0 && quota.value.platformKeyConfigured ? '免费次数用完了，去填 Key' : '去填自己的 API Key 才能对话' }}
          </router-link>
          <span v-else class="quota">{{ quotaLabel }}</span>
          <el-button round @click="createNewFrontendSession">再开一段</el-button>
        </div>
      </header>

      <div ref="chatMessagesRef" class="messages">
        <div v-if="messages.length === 0" class="message ai">
          <div class="avatar">光</div>
          <div class="bubble-wrap">
            <div class="bubble">嗨，我是小光。不急着讲大事，今天过得怎么样？</div>
            <time>刚刚</time>
          </div>
        </div>
        <div v-if="messages.length === 0 && !isAiTyping" class="quick-starts">
          <button v-for="chip in quickStarts" :key="chip" type="button" @click="sendQuickStart(chip)">{{ chip }}</button>
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message"
          :class="msg.senderType === 1 ? 'user' : 'ai'"
        >
          <div class="avatar">{{ msg.senderType === 1 ? '我' : '光' }}</div>
          <div class="bubble-wrap">
            <div class="bubble">
              <div v-if="msg.senderType === 2 && isAiTyping && !msg.content" class="typing">
                <i></i><i></i><i></i>
              </div>
              <div v-else-if="msg.isError" class="error">{{ msg.content }}</div>
              <MarkdownRenderer v-else-if="msg.senderType === 2 && !msg.isError" :content="msg.content" :is-ai-message="true" />
              <p v-else-if="msg.content" class="user-text">{{ msg.content }}</p>
            </div>
            <time>{{ msg.senderType === 2 && isAiTyping ? '小光正在想…' : msg.createdAt }}</time>
          </div>
        </div>
      </div>

      <div class="composer">
        <el-input
          v-model="userMessage"
          placeholder="想说什么都可以，比如「今天有点累」"
          type="textarea"
          :rows="3"
          :disabled="isAiTyping || isStartingSession"
          @keydown="handleKeyDown"
        />
        <div class="composer-foot">
          <span>Enter 发送，Shift+Enter 换行 · {{ userMessage.length }}/500</span>
          <el-button
            type="primary"
            round
            :loading="isStartingSession"
            :disabled="!userMessage.trim() || userMessage.length > 500 || isAiTyping"
            @click="sendMessage"
          >
            轻轻发出去
          </el-button>
        </div>
      </div>
      <p class="disclaimer">小光是陪伴，不是诊疗，也不能替代专业心理咨询。需要支持可拨 12356；有立即危险请拨 120 或 110。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { startSession, getSessionList, deleteSession, getSessionDetail, getSessionEmotion, getProfile } from '@/api/frontend'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DeleteFilled } from '@element-plus/icons-vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { ensureFreshAccessToken } from '@/utils/request'

const quickStarts = ['今天有点累', '晚上睡不着', '说不上来为什么难受', '就想随便聊聊']
const chatMessagesRef = ref(null)

const sendQuickStart = (text) => {
  userMessage.value = text
  sendMessage()
}

const createNewFrontendSession = () => {
  currentSession.value = {
    sessionId: `temp_${Date.now()}`,
    status: 'TEMP',
    sessionTitle: '再开一段',
  }
  messages.value = []
}

const currentSession = ref(null)
const sessionList = ref([])
const messages = ref([])
const userMessage = ref('')
const isAiTyping = ref(false)
const isStartingSession = ref(false)

const currentEmotion = ref({
  primaryEmotion: '平静',
  emotionScore: 50,
  isNegative: false,
  riskLevel: 0,
  suggestion: '慢慢来就好，小光在。',
  improvementSuggestions: [],
})

const quota = ref({
  remainingCount: 20,
  freeLimit: 20,
  unlimited: false,
  hasApiKey: false,
  platformKeyConfigured: true,
})

const needOwnKey = computed(() => {
  if (quota.value.unlimited || quota.value.hasApiKey) return false
  if (!quota.value.platformKeyConfigured) return true
  return quota.value.remainingCount <= 0
})

const quotaLabel = computed(() => {
  if (quota.value.unlimited) {
    return quota.value.hasApiKey ? '自己的 Key · 不限次数' : '次数不限'
  }
  if (!quota.value.platformKeyConfigured) {
    return '需要自己的 API Key'
  }
  return `免费还剩 ${quota.value.remainingCount}/${quota.value.freeLimit} 次`
})

const isActiveSession = (session) => {
  if (!currentSession.value) return false
  return currentSession.value.sessionId === `session_${session.id}` || currentSession.value.sessionId === String(session.id)
}

const loadQuota = () => {
  getProfile().then((data) => {
    quota.value = {
      remainingCount: data.remainingCount,
      freeLimit: data.freeLimit,
      unlimited: data.unlimited,
      hasApiKey: data.hasApiKey,
      platformKeyConfigured: data.platformKeyConfigured !== false,
    }
  }).catch(() => {})
}

const loadSessionEmotion = (sessionId) => {
  const id = sessionId.toString().startsWith('session_') ? sessionId : `session_${sessionId}`
  getSessionEmotion(id).then((res) => {
    currentEmotion.value = {
      ...currentEmotion.value,
      ...res,
      improvementSuggestions: res.improvementSuggestions || [],
    }
  })
}

const getIntensityClass = (score) => {
  if (score >= 61) return 3
  if (score >= 31) return 2
  return 1
}

const getRiskText = (level) => {
  switch (level) {
    case 0: return '还好'
    case 1: return '有点累'
    case 2: return '需要被看见'
    case 3: return '请先求助'
    default: return '还好'
  }
}

const handleKeyDown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const sendMessage = () => {
  if (!userMessage.value.trim()) return
  if (isAiTyping.value || isStartingSession.value) {
    ElMessage.error('小光正在想怎么回你…')
    return
  }
  const message = userMessage.value.trim()
  userMessage.value = ''
  if (!currentSession.value || currentSession.value.status === 'TEMP') {
    startNewSession(message)
  } else {
    messages.value.push({
      id: Date.now(),
      senderType: 1,
      content: message,
      createAt: new Date().toISOString(),
    })
    startAIResponse(currentSession.value.sessionId, message)
  }
}

const startNewSession = (message) => {
  isStartingSession.value = true
  const sessionParams = {
    initialMessage: message,
  }
  if (!currentSession.value || currentSession.value.sessionTitle === '再开一段') {
    sessionParams.sessionTitle = `和小光的聊天 - ${new Date().toLocaleString()}`
  } else {
    sessionParams.sessionTitle = currentSession.value.sessionTitle
  }
  startSession(sessionParams).then((res) => {
    const sessionData = {
      sessionId: res.sessionId,
      status: res.status,
      sessionTitle: sessionParams.sessionTitle,
    }
    if (currentSession.value && currentSession.value.status === 'TEMP') {
      Object.assign(currentSession.value, sessionData)
    } else {
      currentSession.value = sessionData
    }
    getSessionPage()
    messages.value.push({
      id: Date.now(),
      senderType: 1,
      content: message,
      createAt: new Date().toISOString(),
    })
    startAIResponse(currentSession.value.sessionId, message)
  }).catch(() => {
    if (!userMessage.value.trim()) {
      userMessage.value = message
    }
  }).finally(() => {
    isStartingSession.value = false
  })
}

const startAIResponse = async (sessionId, nextMessage) => {
  if (isAiTyping.value) {
    ElMessage.error('小光正在想怎么回你…')
    return
  }
  let accessToken
  try {
    accessToken = await ensureFreshAccessToken()
  } catch (error) {
    ElMessage.error(error?.message || '登录已过期，请重新登录')
    return
  }
  isAiTyping.value = true
  messages.value.push({
    id: `ai_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    senderType: 2,
    content: '',
    createAt: new Date().toISOString(),
  })
  const ctrl = new AbortController()
  fetchEventSource('/api/psychological-chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({
      sessionId,
      userMessage: nextMessage,
    }),
    signal: ctrl.signal,
    credentials: 'include',
    onopen: (response) => {
      const contentType = response.headers.get('Content-Type') || ''
      if (!response.ok || !contentType.startsWith('text/event-stream')) {
        throw new Error('刚才信号不好，你再说一遍也没关系。')
      }
    },
    onmessage: (event) => {
      const raw = event.data.trim()
      if (!raw) return
      const eventName = event.event
      const aiMessage = messages.value[messages.value.length - 1]
      if (eventName === 'done') {
        isAiTyping.value = false
        ctrl.abort()
        loadSessionEmotion(currentSession.value.sessionId)
        loadQuota()
        return
      }
      if (eventName === 'error') {
        const payload = JSON.parse(raw)
        handleError(payload.msg || payload.message || '刚才信号不好，你再说一遍也没关系。')
        ctrl.abort()
        return
      }
      const payload = JSON.parse(raw)
      const ok = String(payload.code) === '200'
      if (ok && payload.data && payload.data.content) {
        aiMessage.content += payload.data.content
      } else if (!ok) {
        handleError(payload.message || '刚才信号不好，你再说一遍也没关系。')
      }
    },
    onerror: (err) => {
      handleError(err || '刚才信号不好，你再说一遍也没关系。')
      throw err
    },
    onclose: () => {
      isAiTyping.value = false
      loadSessionEmotion(currentSession.value.sessionId)
      loadQuota()
    },
  })
}

const handleError = (err) => {
  const text = typeof err === 'string' && err
    ? err
    : (err?.message || '刚才信号不好，你再说一遍也没关系。')
  const aiMessage = messages.value[messages.value.length - 1]
  if (aiMessage) {
    aiMessage.isError = true
    aiMessage.content = text
  }
  isAiTyping.value = false
  ElMessage.error(text)
}

const getSessionPage = () => {
  getSessionList({ pageNum: 1, pageSize: 10 }).then((res) => {
    sessionList.value = res.records
  })
}

const handleSessionClick = (session) => {
  getSessionDetail(session.id).then((res) => {
    messages.value = res
  })
  loadSessionEmotion(session.id)
  currentSession.value = {
    sessionId: 'session_' + session.id,
    status: 'ACTIVE',
    sessionTitle: session.sessionTitle,
  }
}

const handleDeleteSession = (sessionId) => {
  ElMessageBox.confirm('这段对话会从列表里拿掉，确定吗？', '先确认一下', {
    confirmButtonText: '删掉吧',
    cancelButtonText: '先留着',
    type: 'warning',
  }).then(() => {
    deleteSession(sessionId).then(() => {
      ElMessage.success('这段对话已轻轻收起')
      getSessionPage()
    })
  }).catch(() => {})
}

watch(messages, async () => {
  await nextTick()
  const el = chatMessagesRef.value
  if (el) el.scrollTop = el.scrollHeight
}, { deep: true })

onMounted(() => {
  getSessionPage()
  createNewFrontendSession()
  loadQuota()
})
</script>

<style scoped lang="scss">
.consultation {
  width: min(1180px, calc(100% - 24px));
  margin: 16px auto 28px;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 18px;
  min-height: calc(100vh - 180px);
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.presence,
.garden,
.history {
  padding: 18px;
}

.presence {
  text-align: center;
}

.breathing-circle {
  width: 64px;
  height: 64px;
  margin: 0 auto 10px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fffaf2;
  background: radial-gradient(circle at 35% 30%, #f6e2b8, #d4a056 55%, #2c6a5b 120%);
  animation: breathing 4.5s ease-in-out infinite;
  font-family: "Fraunces", Georgia, serif;
  font-size: 24px;
}

.presence h3 {
  font-family: "Fraunces", Georgia, serif;
  font-size: 20px;
}

.online {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
  color: var(--primary);
  font-size: 13px;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #4a9a7b;
    animation: pulse 2s infinite;
  }
}

.side-label {
  font-size: 13px;
  color: var(--muted);
  margin-bottom: 12px;
  letter-spacing: 0.08em;
}

.emotion-orb {
  width: 96px;
  height: 96px;
  margin: 0 auto 14px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  text-align: center;
  color: #5b4630;
  background: radial-gradient(circle at 40% 30%, #fff6e8, #f3d7a4 70%);
  box-shadow: 0 10px 24px rgba(212, 160, 86, 0.18);

  strong {
    font-size: 16px;
  }

  span {
    font-size: 11px;
    opacity: 0.75;
  }
}

.emotion-status {
  text-align: center;
  color: var(--muted);
  font-size: 14px;

  em {
    margin-left: 6px;
    font-style: normal;
    color: var(--primary);
    font-weight: 600;
  }
}

.intensity {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 12px 0 16px;
  color: var(--muted);
  font-size: 12px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e4d9c8;

  &.active {
    background: var(--glow);
  }
}

.tip, .risk {
  padding: 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 10px;

  p {
    font-weight: 600;
    margin-bottom: 4px;
  }
}

.risk {
  background: #fff4e4;
  color: #8a5a1b;

  strong {
    display: block;
    margin-top: 8px;
  }
}

.actions p {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.action {
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  line-height: 1.5;
  color: var(--muted);
}

.empty {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
}

.session {
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  background: transparent;
  border-radius: 14px;
  padding: 10px;
  cursor: pointer;
  margin-bottom: 6px;

  &:hover,
  &.active {
    background: #f7f1e6;
    border-color: var(--line);
  }
}

.session-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  strong {
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.meta, .preview {
  display: block;
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history {
  max-height: 280px;
  overflow: auto;
}

.chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.chat-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--line);

  h2 {
    font-family: "Fraunces", Georgia, serif;
    font-size: 22px;
  }

  p {
    margin-top: 4px;
    color: var(--muted);
    font-size: 13px;
  }
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quota {
  font-size: 12px;
  color: var(--muted);
  background: var(--paper);
  padding: 6px 10px;
  border-radius: 999px;

  &.warn {
    color: #8a5a1b;
    background: #fff4e4;
  }
}

.messages {
  flex: 1;
  min-height: 360px;
  max-height: calc(100vh - 340px);
  overflow: auto;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 10px;
  max-width: 80%;

  &.user {
    align-self: flex-end;
    flex-direction: row-reverse;

    .avatar {
      background: #dce8e3;
      color: var(--primary);
    }

    .bubble {
      background: var(--primary);
      color: #fffaf2;
      border-bottom-right-radius: 6px;
    }

    time {
      text-align: right;
    }
  }

  &.ai .avatar {
    background: radial-gradient(circle at 35% 30%, #f6e2b8, #d4a056 70%);
    color: #fffaf2;
  }

  &.ai .bubble {
    background: #fff;
    border: 1px solid var(--line);
    border-bottom-left-radius: 6px;
  }
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  font-size: 13px;
  font-family: "Fraunces", Georgia, serif;
}

.bubble {
  padding: 12px 14px;
  border-radius: 18px;
  line-height: 1.7;
  font-size: 14px;
  animation: fadeInUp 0.35s ease;
}

.user-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.typing {
  display: flex;
  gap: 4px;
  padding: 4px 0;

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #cbbda8;
    animation: typing 1.4s infinite;

    &:nth-child(2) { animation-delay: 0.15s; }
    &:nth-child(3) { animation-delay: 0.3s; }
  }
}

time {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-size: 12px;
}

.quick-starts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: 44px;

  button {
    border: 1px solid var(--line);
    background: #fff;
    color: var(--primary);
    border-radius: 999px;
    padding: 6px 12px;
    cursor: pointer;
  }
}

.composer {
  padding: 16px 22px 8px;
  border-top: 1px solid var(--line);
}

.composer-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  color: var(--muted);
  font-size: 12px;
}

.disclaimer {
  padding: 0 22px 16px;
  color: #a09078;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .consultation {
    grid-template-columns: 1fr;
  }

  .side {
    order: 2;
  }
}
</style>
