<template>
  <div class="profile page-shell">
    <header class="intro">
      <el-avatar :size="72" :src="avatarUrl">{{ displayInitial }}</el-avatar>
      <div>
        <p class="page-kicker">只属于你的角落</p>
        <h1 class="page-title">{{ profile.displayName || '你好，这里很安静' }}</h1>
        <p class="page-desc">{{ quotaText }}</p>
      </div>
    </header>

    <section class="surface-card block">
      <h2>我想被怎样称呼</h2>
      <el-form label-position="top">
        <div class="avatar-row">
          <el-upload :show-file-list="false" accept="image/*" :http-request="handleAvatarUpload">
            <el-avatar :size="72" :src="avatarUrl">{{ displayInitial }}</el-avatar>
          </el-upload>
          <span>点头像换一张更让自己安心的照片</span>
        </div>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="小光可以这样轻轻叫你" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="用来找回这个角落" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="不想填也可以" />
        </el-form-item>
        <div class="two-col">
          <el-form-item label="性别">
            <el-select v-model="form.gender" placeholder="不想说也行" style="width: 100%">
              <el-option label="不想说" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker v-model="form.birthday" type="date" placeholder="选一天" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>
        <el-button type="primary" round :loading="savingProfile" @click="saveProfile">记下这些</el-button>
      </el-form>
    </section>

    <section class="surface-card block">
      <h2>换一把钥匙</h2>
      <p class="card-desc">改密码需要先确认现在这把。别人就算拿到登录状态，也不能直接换掉。</p>
      <el-form label-position="top">
        <el-form-item label="现在的密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="现在正在用的那把" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="再写一次新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="确认新密码" />
        </el-form-item>
        <el-button type="primary" round :loading="savingPassword" @click="savePassword">换好密码</el-button>
      </el-form>
    </section>

    <section class="surface-card block">
      <h2>继续聊下去</h2>
      <p class="card-desc">
        每人可以用小光的钥匙免费聊 {{ profile.freeLimit || 20 }} 次。用完之后，把你自己的 DeepSeek / OpenAI 兼容 Key 放在这里，就能继续被陪伴。
      </p>
      <el-form label-position="top">
        <el-form-item :label="profile.hasApiKey ? `当前 Key：${profile.apiKeyMasked}` : 'API Key'">
          <el-input v-model="apiForm.apiKey" type="password" show-password placeholder="sk- 开头的密钥，只存在你的账号里" />
        </el-form-item>
        <el-form-item label="接口地址">
          <el-input v-model="apiForm.apiBaseUrl" placeholder="默认 https://api.deepseek.com，只支持常见 https 地址" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="apiForm.apiModel" placeholder="默认 deepseek-chat" />
        </el-form-item>
        <div class="actions">
          <el-button type="primary" round :loading="savingApi" @click="saveApiKey">收好 Key</el-button>
          <el-button v-if="profile.hasApiKey" round @click="clearApiKey">去掉我的 Key</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { fileBaseUrl } from '@/config/index.js'
import { getProfile, updateApiKey, updateProfile, uploadAvatar, changePassword } from '@/api/frontend'

const savingProfile = ref(false)
const router = useRouter()
const savingApi = ref(false)
const savingPassword = ref(false)
const profile = ref({
  displayName: '',
  avatar: '',
  hasApiKey: false,
  apiKeyMasked: '',
  freeLimit: 20,
  usedCount: 0,
  remainingCount: 20,
  unlimited: false,
  platformKeyConfigured: true,
})

const form = reactive({
  nickname: '',
  email: '',
  phone: '',
  gender: 0,
  birthday: '',
  avatar: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const apiForm = reactive({
  apiKey: '',
  apiBaseUrl: '',
  apiModel: '',
})

const avatarUrl = computed(() => {
  if (!form.avatar) return ''
  if (form.avatar.startsWith('http')) return form.avatar
  return fileBaseUrl + form.avatar
})

const displayInitial = computed(() => (profile.value.displayName || '光').slice(0, 1))

const quotaText = computed(() => {
  if (profile.value.unlimited) {
    return profile.value.hasApiKey ? '正在用你自己的 Key，次数不限。' : '这里可以一直聊下去。'
  }
  if (!profile.value.platformKeyConfigured && !profile.value.hasApiKey) {
    return '免费额度暂时不可用。把你自己的 DeepSeek Key 填在下面，就能继续聊。'
  }
  return `免费额度还剩 ${profile.value.remainingCount} / ${profile.value.freeLimit} 次，用完也不代表你被丢掉。`
})

const applyProfile = (data) => {
  profile.value = data
  form.nickname = data.nickname || ''
  form.email = data.email || ''
  form.phone = data.phone || ''
  form.gender = data.gender ?? 0
  form.birthday = data.birthday || ''
  form.avatar = data.avatar || ''
  apiForm.apiKey = ''
  apiForm.apiBaseUrl = data.apiBaseUrl || ''
  apiForm.apiModel = data.apiModel || ''
  const stored = JSON.parse(localStorage.getItem('userInfo') || '{}')
  localStorage.setItem('userInfo', JSON.stringify({
    ...stored,
    nickname: data.nickname,
    displayName: data.displayName,
    avatar: data.avatar,
    email: data.email,
  }))
}

const loadProfile = () => {
  getProfile().then(applyProfile)
}

const handleAvatarUpload = ({ file }) => {
  uploadAvatar(file).then((res) => {
    form.avatar = res.filePath
    return updateProfile({ avatar: res.filePath })
  }).then((data) => {
    applyProfile(data)
    ElMessage.success('头像换好了')
  })
}

const saveProfile = () => {
  savingProfile.value = true
  const payload = {
    nickname: form.nickname,
    email: form.email,
    phone: form.phone,
    gender: form.gender,
    birthday: form.birthday || null,
    avatar: form.avatar,
  }
  updateProfile(payload).then((data) => {
    applyProfile(data)
    ElMessage.success('记下了')
  }).finally(() => {
    savingProfile.value = false
  })
}

const savePassword = () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.error('现在的密码和新密码都要填')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次新密码不太一样')
    return
  }
  savingPassword.value = true
  changePassword({
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword,
    confirmPassword: passwordForm.confirmPassword,
  }).then(() => {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('tokenExpiresIn')
    localStorage.removeItem('tokenExpiresAt')
    ElMessage.success('密码换好了，请用新密码重新登录')
    router.push('/auth/login')
  }).finally(() => {
    savingPassword.value = false
  })
}

const saveApiKey = () => {
  if (!apiForm.apiKey.trim() && !profile.value.hasApiKey) {
    ElMessage.error('先把 API Key 填上吧')
    return
  }
  savingApi.value = true
  updateApiKey({
    apiKey: apiForm.apiKey.trim() || undefined,
    apiBaseUrl: apiForm.apiBaseUrl,
    apiModel: apiForm.apiModel,
  }).then((data) => {
    applyProfile(data)
    ElMessage.success('Key 收好了，可以继续找小光聊聊')
  }).finally(() => {
    savingApi.value = false
  })
}

const clearApiKey = () => {
  savingApi.value = true
  updateApiKey({ clearKey: true }).then((data) => {
    applyProfile(data)
    ElMessage.success('已经去掉你的 Key，会重新走免费次数')
  }).finally(() => {
    savingApi.value = false
  })
}

onMounted(loadProfile)
</script>

<style scoped lang="scss">
.intro {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 24px;
}

.block {
  padding: 28px;
  margin-bottom: 18px;
  max-width: 720px;

  h2 {
    font-family: "Fraunces", Georgia, serif;
    font-size: 22px;
    margin-bottom: 8px;
  }
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--muted);
  font-size: 13px;
}

.two-col,
.actions {
  display: flex;
  gap: 12px;
}

.two-col > * {
  flex: 1;
}

.card-desc {
  color: var(--muted);
  line-height: 1.8;
  margin-bottom: 12px;
}

@media (max-width: 640px) {
  .intro,
  .two-col {
    display: block;
  }
}
</style>
