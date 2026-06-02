<template>
  <div class="pagination">
    <button class="page-btn" :disabled="currentPage === 1" @click="$emit('change', currentPage - 1)">
      <i class="ri-arrow-left-s-line"></i>
      Previous
    </button>

    <div class="page-numbers">
      <button
        v-for="page in visiblePages"
        :key="page"
        class="page-num"
        :class="{ active: page === currentPage, dots: page === '...' }"
        :disabled="page === '...'"
        @click="page !== '...' && $emit('change', page)"
      >
        {{ page }}
      </button>
    </div>

    <button class="page-btn" :disabled="currentPage === totalPages" @click="$emit('change', currentPage + 1)">
      Next
      <i class="ri-arrow-right-s-line"></i>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true }
})

defineEmits(['change'])

const visiblePages = computed(() => {
  const pages = []
  const total = props.totalPages
  const current = props.currentPage

  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
    return pages
  }

  pages.push(1)
  if (current > 3) pages.push('...')

  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)

  for (let i = start; i <= end; i++) pages.push(i)

  if (current < total - 2) pages.push('...')
  pages.push(total)

  return pages
})
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
}

.page-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-num {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  transition: all 0.2s;
}

.page-num:hover:not(.dots):not(.active) {
  background: var(--color-bg-hover);
}

.page-num.active {
  background: var(--color-primary);
  color: #fff;
}

.page-num.dots {
  cursor: default;
  color: var(--color-text-placeholder);
}
</style>
