<template>
  <div class="main-layout">
    <Sidebar />
    <TopNavbar />
    <main class="main-content">
      <div class="breadcrumb">
        <span class="breadcrumb-item">Home</span>
        <i class="ri-arrow-right-s-line breadcrumb-sep"></i>
        <span class="breadcrumb-item active">{{ currentPage }}</span>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './Sidebar.vue'
import TopNavbar from './TopNavbar.vue'

const route = useRoute()
const currentPage = computed(() => route.meta?.title || 'Dashboard')
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: var(--color-bg-page);
}

.main-content {
  margin-left: var(--sidebar-width);
  margin-top: var(--topbar-height);
  padding: 24px;
  min-height: calc(100vh - var(--topbar-height));
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 24px;
}

.breadcrumb-item {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.breadcrumb-item.active {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.breadcrumb-sep {
  font-size: 16px;
  color: var(--color-text-placeholder);
}
</style>
