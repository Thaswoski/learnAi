<template>
  <header class="topbar">
    <div class="topbar-left">
      <button class="todo-btn" @click="$router.push('/app/tutor')">
        <i class="ri-chat-smile-2-line"></i>
        <span>AI 助手</span>
      </button>
      <div class="search-box">
        <i class="ri-search-line search-icon"></i>
        <input
          type="text"
          placeholder="搜索知识点、课程、题目..."
          class="search-input"
        />
        <kbd class="search-shortcut">Ctrl + K</kbd>
      </div>
    </div>

    <div class="topbar-right">
      <button class="icon-btn has-notification">
        <i class="ri-notification-3-line"></i>
        <span class="notification-dot">5</span>
      </button>

      <button class="icon-btn has-notification">
        <i class="ri-message-3-line"></i>
        <span class="notification-dot">12</span>
      </button>

      <div class="language-switch">
        <i class="ri-global-line"></i>
        <select class="lang-select">
          <option value="zh">中</option>
          <option value="en">EN</option>
        </select>
      </div>

      <div class="user-profile" @click="$router.push('/app/settings')" title="点击进入个人信息设置">
        <div class="user-avatar">
          <img :src="avatarUrl" alt="User avatar" />
        </div>
        <div class="user-info">
          <span class="user-name">{{ displayName }}</span>
          <span class="user-email">{{ displayEmail }}</span>
        </div>
        <i class="ri-arrow-down-s-line user-arrow"></i>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const defaultAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e558bpng.png'

const avatarUrl = ref(localStorage.getItem('userAvatar') || defaultAvatar)
const displayName = ref(localStorage.getItem('userName') || '用户')
const displayEmail = ref(localStorage.getItem('userEmail') || '')

function refreshUser() {
  avatarUrl.value = localStorage.getItem('userAvatar') || defaultAvatar
  displayName.value = localStorage.getItem('userName') || '用户'
  displayEmail.value = localStorage.getItem('userEmail') || ''
}

onMounted(() => window.addEventListener('user-updated', refreshUser))
onUnmounted(() => window.removeEventListener('user-updated', refreshUser))
</script>

<style scoped>
.topbar {
  position: fixed;
  top: 0;
  left: var(--sidebar-width);
  right: 0;
  height: var(--topbar-height);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 99;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.todo-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: background 0.2s;
}

.todo-btn:hover {
  background: var(--color-primary-light);
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: var(--color-text-tertiary);
  font-size: 16px;
}

.search-input {
  width: 360px;
  padding: 8px 12px 8px 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
  font-size: var(--font-size-sm);
  transition: all 0.2s;
}

.search-input:focus {
  border-color: var(--color-primary);
  background: var(--color-bg-card);
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.1);
}

.search-input::placeholder {
  color: var(--color-text-placeholder);
}

.search-shortcut {
  position: absolute;
  right: 8px;
  padding: 2px 6px;
  background: var(--color-border-light);
  border-radius: 4px;
  font-size: 10px;
  color: var(--color-text-tertiary);
  font-family: inherit;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 20px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--color-bg-hover);
  color: var(--color-text-primary);
}

.notification-dot {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--color-danger);
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.language-switch {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  cursor: pointer;
}

.language-switch i {
  font-size: 18px;
}

.lang-select {
  border: none;
  background: transparent;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: 0;
}

.lang-select:focus {
  box-shadow: none;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}

.user-profile:hover {
  background: var(--color-bg-hover);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  line-height: 1.2;
}

.user-email {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  line-height: 1.2;
}

.user-arrow {
  font-size: 16px;
  color: var(--color-text-tertiary);
}
</style>
