<template>
  <aside class="sidebar">
    <div class="sidebar-logo">
      <div class="logo-icon">
        <img src="/logo.ico" alt="logo" class="logo-img" />
      </div>
      <span class="logo-text">智多星</span>
    </div>

    <nav class="sidebar-nav">
      <router-link
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <i :class="item.icon"></i>
        <span>{{ item.label }}</span>
      </router-link>
    </nav>

    <div class="sidebar-ad">
      <div class="ad-card">
        <div class="ad-icon">
          <i class="ri-rocket-2-line"></i>
        </div>
        <h4 class="ad-title">AI 驱动学习</h4>
        <p class="ad-text">基于大模型的个性化学习系统，智能规划你的学习路径</p>
        <button class="ad-btn" @click="$router.push('/app/profile')">查看学习画像</button>
      </div>
    </div>

    <div class="sidebar-footer">
      <button class="logout-btn" @click="handleLogout">
        <i class="ri-logout-box-r-line"></i>
        <span>退出登录</span>
      </button>
      <p class="copyright">&copy; 2026 智多星. All rights reserved.</p>
    </div>
  </aside>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

function handleLogout() {
  localStorage.removeItem('isLoggedIn')
  router.push('/')
}

const menuItems = [
  { path: '/app/dashboard', label: '学习仪表盘', icon: 'ri-dashboard-line' },
  { path: '/app/profile', label: '学习画像', icon: 'ri-user-settings-line' },
  { path: '/app/resource', label: '资源生成', icon: 'ri-file-copy-line' },
  { path: '/app/path', label: '学习路径', icon: 'ri-guide-line' },
  { path: '/app/tutor', label: '智能辅导', icon: 'ri-chat-smile-2-line' },
  { path: '/app/evaluation', label: '学习评估', icon: 'ri-bar-chart-2-line' },
  { path: '/app/questionbank', label: '题库练习', icon: 'ri-book-open-line' },
  { path: '/app/community', label: '社区讨论', icon: 'ri-chat-1-line' },
  { path: '/app/settings', label: '个人信息设置', icon: 'ri-settings-3-line' }
]

function isActive(path) {
  return route.path.startsWith(path)
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  z-index: 100;
  padding: 0 12px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 12px 24px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.logo-text {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: all 0.2s;
}

.nav-item i {
  font-size: 20px;
  width: 20px;
}

.nav-item:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.nav-item.active {
  background: var(--color-primary-bg);
  color: var(--color-primary);
}

.sidebar-ad {
  padding: 16px 0;
}

.ad-card {
  background: var(--color-primary-gradient);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
  text-align: center;
  color: #fff;
}

.ad-icon {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-bottom: 12px;
}

.ad-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  margin-bottom: 6px;
}

.ad-text {
  font-size: var(--font-size-xs);
  opacity: 0.85;
  line-height: 1.5;
  margin-bottom: 14px;
}

.ad-btn {
  width: 100%;
  padding: 8px;
  background: #fff;
  color: var(--color-primary);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  cursor: pointer;
  border: none;
  transition: opacity 0.2s;
}

.ad-btn:hover {
  opacity: 0.9;
}

.sidebar-footer {
  padding: 12px 12px 16px;
}

.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: var(--color-bg-hover);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  color: var(--color-text-tertiary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 10px;
}

.logout-btn:hover {
  color: var(--color-danger);
  border-color: var(--color-danger);
  background: var(--color-danger-bg);
}

.copyright {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  text-align: center;
}
</style>
