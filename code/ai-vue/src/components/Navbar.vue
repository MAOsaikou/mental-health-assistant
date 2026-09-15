<template>
  <div class="navbar">
    <div class="flex-box">
      <el-button class="collapse-btn" @click="handleCollapse">
        <el-icon><Expand /></el-icon>
      </el-button>
      <div class="page-meta">
        <p class="eyebrow">后台管理</p>
        <p class="page-title">{{ route.meta.title || '控制台' }}</p>
      </div>
    </div>

    <div class="flex-box">
      <el-dropdown @command="handleCommand">
        <div class="user-box">
          <el-avatar :size="36" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
          <span class="user-name">admin</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAdminStore } from '@/stores/admin'
import { logout } from '@/api/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()

const handleCollapse = () => {
  adminStore.toggleCollapse()
}

const handleCommand = (command) => {
  if (command !== 'logout') return
  ElMessageBox.confirm('确定退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    logout().finally(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('tokenExpiresIn')
      localStorage.removeItem('tokenExpiresAt')
      router.push('/auth/login')
    })
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.78);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.flex-box {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
}

.page-meta {
  .eyebrow {
    font-size: 12px;
    color: var(--muted);
    letter-spacing: 0.08em;
  }

  .page-title {
    margin-top: 2px;
    font-size: 22px;
    font-weight: 700;
  }
}

.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #fff;
  cursor: pointer;
  outline: none;
}

.user-name {
  font-size: 14px;
  color: var(--ink);
}
</style>
