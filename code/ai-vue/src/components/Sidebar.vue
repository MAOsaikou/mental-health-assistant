<template>
  <el-aside :width="isCollapse ? '72px' : '248px'" class="sidebar">
    <el-menu
      router
      :collapse="isCollapse"
      :collapse-transition="false"
      :default-active="activeMenu"
      class="menu-style"
      @open="handleOpen"
      @close="handleClose"
    >
      <div class="brand">
        <div class="brand-mark">光</div>
        <div v-show="!isCollapse" class="brand-text">
          <h1>小光心理助手</h1>
          <p>管理后台</p>
        </div>
      </div>

      <el-menu-item
        v-for="item in menus"
        :key="item.path"
        :index="`/back/${item.path}`"
      >
        <el-icon>
          <component :is="item.meta.icon" />
        </el-icon>
        <span>{{ item.meta.title }}</span>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const { isCollapse } = storeToRefs(useAdminStore())

const activeMenu = computed(() => route.path)

const menus = computed(() => {
  const backRoute = router.options.routes.find((item) => item.path === '/back')
  return backRoute?.children || []
})

const handleOpen = (key, keyPath) => {
  console.log(key, keyPath)
}

const handleClose = (key, keyPath) => {
  console.log(key, keyPath)
}
</script>

<style lang="scss" scoped>
.sidebar {
  transition: width 0.2s ease;
  background: rgba(255, 255, 255, 0.72);
  border-right: 1px solid var(--line);
  backdrop-filter: blur(10px);
}

.menu-style {
  height: 100%;
  border-right: none;
  background: transparent;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px;
  padding: 14px 12px;
  border-radius: 14px;
  background: linear-gradient(145deg, #2f7a6b, #3f9584);
  color: #fff;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.18);
  font-family: "Fraunces", Georgia, serif;
  font-size: 20px;
  font-weight: 600;
}

.brand-text {
  h1 {
    font-size: 16px;
    font-weight: 700;
    line-height: 1.2;
  }

  p {
    margin-top: 4px;
    font-size: 12px;
    opacity: 0.82;
  }
}

:deep(.el-menu-item) {
  margin: 4px 10px;
  border-radius: 12px;
  color: var(--muted);
}

:deep(.el-menu-item.is-active) {
  background: rgba(47, 122, 107, 0.12) !important;
  color: var(--primary) !important;
  font-weight: 600;
}

:deep(.el-menu-item:hover) {
  background: rgba(47, 122, 107, 0.08) !important;
}
</style>
