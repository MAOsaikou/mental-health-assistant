<template>
  <div class="auth-card">
    <router-link class="back" to="/">← 先回去看看</router-link>
    <h2>欢迎回来</h2>
    <p>你来了，就继续被好好对待。</p>
    <el-form ref="ruleFormRef" :model="formData" :rules="rules" label-position="top">
      <el-form-item label="用户名或邮箱" prop="username">
        <el-input v-model="formData.username" size="large" placeholder="用户名或邮箱" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="formData.password" size="large" placeholder="只有你知道的密码" type="password" show-password />
      </el-form-item>
      <el-button class="btn" size="large" round type="primary" @click="submitForm(ruleFormRef)">轻轻进来</el-button>
    </el-form>
    <p class="foot">还没有角落？<router-link to="/auth/register">来留一个</router-link></p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin'

const router = useRouter()
const ruleFormRef = ref()
const formData = reactive({
  username: '',
  password: '',
})
const rules = reactive({
  username: [{ required: true, message: '写下你的名字就好', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

const submitForm = async (formEl) => {
  if (!formEl) return
  await formEl.validate((valid) => {
    if (!valid) return
    login(formData).then((data) => {
      if (!data?.token) {
        ElMessage.error('账号或密码好像不对，再试一次也没关系。')
        return
      }
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
      localStorage.setItem('tokenExpiresAt', String(Date.now() + Number(data.expiresIn || 0)))
      if (data.userInfo.userType === 2) {
        router.push('/back/dashboard')
      } else {
        router.push('/')
      }
    })
  })
}
</script>

<style scoped lang="scss">
.auth-card {
  width: min(400px, 100%);
  padding: 32px 28px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 24px;
  box-shadow: var(--shadow);
}

.back {
  display: inline-block;
  margin-bottom: 28px;
  color: var(--muted);
  font-size: 14px;
}

h2 {
  font-family: "Fraunces", Georgia, serif;
  font-size: 32px;
}

p {
  margin: 8px 0 24px;
  color: var(--muted);
}

.btn {
  width: 100%;
  margin-top: 8px;
}

.foot {
  margin: 20px 0 0;
  text-align: center;

  a {
    color: var(--primary);
  }
}
</style>
