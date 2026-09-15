<template>
  <div class="frontend-layout">
    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark" aria-hidden="true">光</span>
          <span class="brand-copy">
            <strong>小光</strong>
            <em>心理助手</em>
          </span>
        </router-link>

        <button class="menu-toggle" type="button" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen">
          {{ menuOpen ? '收起' : '菜单' }}
        </button>

        <nav class="nav" :class="{ open: menuOpen }">
          <p v-if="isLoggedIn && greetingName" class="greeting">{{ timeGreeting }}，{{ greetingName }}</p>
          <router-link to="/" exact-active-class="is-active" class="nav-link" @click="menuOpen = false">首页</router-link>
          <router-link v-if="isLoggedIn" to="/consultation" class="nav-link" @click="menuOpen = false">聊聊</router-link>
          <router-link v-if="isLoggedIn" to="/emotion-diary" class="nav-link" @click="menuOpen = false">心情</router-link>
          <router-link to="/knowledge" class="nav-link" @click="menuOpen = false">读一读</router-link>
          <router-link v-if="isLoggedIn" to="/profile" class="nav-link" @click="menuOpen = false">我的</router-link>
          <el-button v-if="isLoggedIn" class="ghost-btn" @click="handleLogout">先走了</el-button>
          <template v-else>
            <router-link to="/auth/login" class="nav-link" @click="menuOpen = false">登录</router-link>
            <router-link to="/auth/register" @click="menuOpen = false">
              <el-button type="primary" round>留下一个角落</el-button>
            </router-link>
          </template>
        </nav>
      </div>
    </header>

    <main class="main-content">
      <router-view v-slot="{ Component, route: currentRoute }">
        <component :is="Component" :key="currentRoute.fullPath" />
      </router-view>
    </main>

    <footer class="site-footer">
      <div class="footer-inner">
        <p class="footer-line">小光在，不急着好起来。</p>
        <p class="footer-note">这里是陪伴，不是诊疗。需要支持可拨 12356；有立即危险请拨 120 或 110。</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { logout } from '@/api/admin'

const router = useRouter()
const route = useRoute()
const isLoggedIn = ref(false)
const greetingName = ref('')
const menuOpen = ref(false)

const timeGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const handleLogout = () => {
  menuOpen.value = false
  ElMessageBox.confirm('今天先到这里也很好。小光等你下次来。', '先走一步', {
    confirmButtonText: '好',
    cancelButtonText: '再待一会',
    type: 'info',
  }).then(() => {
    logout().finally(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('tokenExpiresIn')
      localStorage.removeItem('tokenExpiresAt')
      isLoggedIn.value = false
      greetingName.value = ''
      router.push('/auth/login')
    })
  }).catch(() => {})
}

const loadGreeting = () => {
  isLoggedIn.value = localStorage.getItem('token') !== null
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    greetingName.value = info.displayName || info.nickname || info.username || ''
  } catch {
    greetingName.value = ''
  }
}

onMounted(loadGreeting)
watch(() => route.path, () => {
  loadGreeting()
  menuOpen.value = false
})
</script>

<style scoped lang="scss">
.frontend-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(244, 239, 230, 0.82);
  border-bottom: 1px solid rgba(228, 217, 200, 0.8);
  backdrop-filter: blur(16px);
}

.topbar-inner {
  width: min(1120px, calc(100% - 32px));
  margin: 0 auto;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fffaf2;
  background:
    radial-gradient(circle at 35% 30%, #f6e2b8, #d4a056 58%, #2c6a5b 120%);
  box-shadow: 0 8px 20px rgba(212, 160, 86, 0.28);
  font-family: "Fraunces", Georgia, serif;
  font-size: 20px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.15;

  strong {
    font-family: "Fraunces", Georgia, serif;
    font-size: 20px;
    font-weight: 600;
  }

  em {
    color: var(--muted);
    font-size: 12px;
    font-style: normal;
    letter-spacing: 0.12em;
  }
}

.menu-toggle {
  display: none;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--ink);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
}

.nav {
  display: flex;
  align-items: center;
  gap: 8px 22px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.greeting {
  color: var(--primary);
  font-size: 13px;
  font-weight: 600;
  margin-right: 4px;
}

.nav-link {
  position: relative;
  color: var(--muted);
  font-size: 15px;
  font-weight: 500;
  padding: 6px 0;

  &:hover,
  &.is-active,
  &.router-link-active:not([href="/"]) {
    color: var(--primary);
  }

  &.is-active::after,
  &.router-link-active:not([href="/"])::after {
    content: "";
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 2px;
    border-radius: 999px;
    background: var(--glow);
  }
}

.ghost-btn {
  border-radius: 999px;
  border-color: var(--line);
  background: transparent;
  color: var(--muted);
}

.main-content {
  flex: 1;
}

.site-footer {
  margin-top: auto;
  padding: 22px 16px;
  background: #2a3732;
  color: rgba(255, 250, 242, 0.82);
}

.footer-inner {
  width: min(1120px, 100%);
  margin: 0 auto;
  text-align: center;
}

.footer-line {
  font-family: "Fraunces", Georgia, serif;
  font-size: 16px;
}

.footer-note {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: rgba(255, 250, 242, 0.58);
}

@media (max-width: 860px) {
  .menu-toggle {
    display: inline-flex;
  }

  .nav {
    display: none;
    position: absolute;
    top: 72px;
    left: 16px;
    right: 16px;
    padding: 16px;
    flex-direction: column;
    align-items: stretch;
    background: var(--surface);
    border: 1px solid var(--line);
    border-radius: 18px;
    box-shadow: var(--shadow);

    &.open {
      display: flex;
    }
  }
}
</style>
