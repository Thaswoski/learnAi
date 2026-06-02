import { createRouter, createWebHashHistory } from 'vue-router'
import MainLayout from '@/components/layout/MainLayout.vue'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('@/views/Landing.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/app',
    component: MainLayout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '学习仪表盘' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '学习画像' }
      },
      {
        path: 'resource',
        name: 'Resource',
        component: () => import('@/views/Resource.vue'),
        meta: { title: '资源生成' }
      },
      {
        path: 'path',
        name: 'Path',
        component: () => import('@/views/Path.vue'),
        meta: { title: '学习路径' }
      },
      {
        path: 'tutor',
        name: 'Tutor',
        component: () => import('@/views/TutorChat.vue'),
        meta: { title: '智能辅导' }
      },
      {
        path: 'evaluation',
        name: 'Evaluation',
        component: () => import('@/views/Evaluation.vue'),
        meta: { title: '学习评估' }
      },
      {
        path: 'questionbank',
        name: 'QuestionBank',
        component: () => import('@/views/QuestionBank.vue'),
        meta: { title: '题库练习' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '个人信息设置' }
      },
      {
        path: 'community',
        name: 'Community',
        component: () => import('@/views/Community.vue'),
        meta: { title: '社区讨论' }
      },
      {
        path: 'video-generate',
        name: 'VideoGenerate',
        component: () => import('@/views/VideoGenerate.vue'),
        meta: { title: '教学视频生成' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true'

  if (to.name === 'Login') {
    return isLoggedIn ? next('/app/dashboard') : next()
  }

  if (to.path.startsWith('/app')) {
    if (!isLoggedIn) return next('/login')
  }

  next()
})

export default router
