<template>
  <div class="auth-card">
    <h2>先给自己留一个角落</h2>
    <p>一分钟就好。这里会把你的隐私好好收着。</p>
    <el-form ref="submitFormRef" label-position="top" :model="formData" :rules="rules">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="formData.username" placeholder="起个好记的名字" size="large" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="formData.email" placeholder="用来找回这个角落" size="large" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="formData.nickname" placeholder="小光可以这样叫你（可选）" size="large" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="formData.phone" placeholder="不想填也可以" size="large" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="formData.password" placeholder="设一个只有你知道的密码" size="large" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="formData.confirmPassword" placeholder="再输一遍，确认一下" size="large" type="password" show-password />
      </el-form-item>
      <el-button class="btn" type="primary" round size="large" @click="submitForm(submitFormRef)">留下这个角落</el-button>
    </el-form>
    <p class="foot">已经来过？<router-link to="/auth/login">直接进来</router-link></p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/frontend'
import { login } from '@/api/admin'

const router = useRouter()
const submitFormRef = ref(null)
const formData = reactive({
  username: '',
  email: '',
  nickname: '',
  phone: '',
  password: '',
  confirmPassword: '',
  gender: 0,
})
const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请再确认一次', trigger: 'blur' }],
})

const submitForm = (formEl) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (!valid) return
    if (formData.password !== formData.confirmPassword) {
      ElMessage.error('两次密码不一致')
      return
    }
    register(formData).then(() => {
      return login({
        username: formData.username,
        password: formData.password,
      })
    }).then((data) => {
      if (!data?.token) {
        ElMessage.success('角落留好了，先登录一下吧')
        router.push('/auth/login')
        return
      }
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
      localStorage.setItem('tokenExpiresAt', String(Date.now() + Number(data.expiresIn || 0)))
      ElMessage.success('来了就好，先不用想太多')
      router.push('/consultation')
    }).catch((err) => {
      if (err?.code === 'BUSINESS_ERROR') {
        ElMessage.error(err.message || '这次没成功，再试一次也没关系。')
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

h2 {
  font-family: "Fraunces", Georgia, serif;
  font-size: 28px;
}

p {
  margin: 8px 0 20px;
  color: var(--muted);
}

.btn {
  width: 100%;
}

.foot {
  margin: 16px 0 0;
  text-align: center;

  a {
    color: var(--primary);
  }
}
</style>
