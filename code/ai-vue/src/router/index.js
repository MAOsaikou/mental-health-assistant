import { createRouter, createWebHistory } from 'vue-router'
import BackendLayout from '@/components/BackendLayout.vue'
import AuthLayout from '@/components/AuthLayout.vue'
import FrontendLayout from '@/components/FrontendLayout.vue'
import service from '@/utils/request'

const backendRoutes = [
  {
    path: '/back',
    redirect: '/back/dashboard',
    component: BackendLayout,
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据分析', icon: 'PieChart' },
      },
      {
        path: 'knowledge',
        component: () => import('@/views/Knowledge.vue'),
        meta: { title: '知识文章', icon: 'ChatLineSquare' },
      },
      {
        path: 'consultations',
        component: () => import('@/views/Consultations.vue'),
        meta: { title: '咨询记录', icon: 'Message' },
      },
      {
        path: 'emotional',
        component: () => import('@/views/Emotional.vue'),
        meta: { title: '情绪日志', icon: 'User' },
      },
    ],
  },
]

const authRoutes = [
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        component: () => import('@/views/Login.vue'),
        meta: { title: '登录' },
      },
      {
        path: 'register',
        component: () => import('@/views/Register.vue'),
        meta: { title: '注册' },
      },
    ],
  },
]

const frontendRoutes = [
  {
    path: '/',
    component: FrontendLayout,
    children: [
      {
        path: '',
        component: () => import('@/views/Home.vue'),
      },
      {
        path: 'consultation',
        component: () => import('@/views/Consultation.vue'),
      },
      {
        path: 'emotion-diary',
        component: () => import('@/views/EmotionDiary.vue'),
      },
      {
        path: 'knowledge',
        component: () => import('@/views/FrontendKnowledge.vue'),
      },
      {
        path: 'knowledge/article/:id',
        component: () => import('@/views/ArticleDetail.vue'),
        props: true,
      },
      {
        path: 'profile',
        component: () => import('@/views/Profile.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...backendRoutes, ...authRoutes, ...frontendRoutes],
})

const protectedFrontendPaths = new Set(['/consultation', '/emotion-diary', '/profile'])
const SESSION_VALIDATION_INTERVAL = 30_000
let validatedToken = ''
let lastValidationAt = 0
let validationPromise = null

const requiresAuthentication = (path) => path.startsWith('/back') || protectedFrontendPaths.has(path)

const readUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    localStorage.removeItem('userInfo')
    return {}
  }
}

const validateSession = async (token) => {
  if (validatedToken === token && Date.now() - lastValidationAt < SESSION_VALIDATION_INTERVAL) {
    return true
  }
  if (!validationPromise) {
    validationPromise = service.get('/user/current')
      .then((currentUser) => {
        if (currentUser) {
          localStorage.setItem('userInfo', JSON.stringify(currentUser))
        }
        validatedToken = localStorage.getItem('token') || ''
        lastValidationAt = Date.now()
        return true
      })
      .catch(() => {
        validatedToken = ''
        lastValidationAt = 0
        return false
      })
      .finally(() => {
        validationPromise = null
      })
  }
  return validationPromise
}

router.beforeEach(async (to) => {
  const token = localStorage.getItem('token')

  if (token) {
    if (requiresAuthentication(to.path) && !(await validateSession(token))) {
      return '/auth/login'
    }

    const userInfo = readUserInfo()
    if (userInfo.userType == 2) {
      if (to.path.startsWith('/back')) {
        return true
      } else {
        return '/back/dashboard'
      }
    } else if (userInfo.userType == 1) {
      if (to.path.startsWith('/back') || to.path.startsWith('/auth')) {
        return '/'
      } else {
        return true
      }
    } else {
      return requiresAuthentication(to.path) ? '/auth/login' : true
    }
  } else if (requiresAuthentication(to.path)) {
    return '/auth/login'
  } else {
    return true
  }
})

export default router
