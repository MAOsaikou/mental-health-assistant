<template>
  <div class="backend-layout">
    <el-container class="main-container">
      <Sidebar />
      <el-container>
        <el-header>
          <Navbar />
        </el-header>
        <el-main class="main-content">
          <router-view v-slot="{ Component, route: currentRoute }">
            <component :is="Component" :key="currentRoute.fullPath" class="content-panel" />
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import Sidebar from '@/components/Sidebar.vue'
import Navbar from '@/components/Navbar.vue'
</script>

<style lang="scss" scoped>
.backend-layout {
  height: 100vh;
}

.main-container {
  height: 100%;
}

.main-content {
  padding: 20px;
  background: transparent;
}

.content-panel {
  min-height: calc(100% - 8px);
  padding: 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  animation: rise 0.45s ease;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
