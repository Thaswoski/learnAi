<template>
  <span class="status-badge badge" :class="badgeClass">{{ label }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const statusMap = {
  negotiation: { class: 'badge-blue', label: 'Negotiation' },
  pending: { class: 'badge-orange', label: 'Pending' },
  approved: { class: 'badge-green', label: 'Approved' },
  rejected: { class: 'badge-red', label: 'Rejected' },
  completed: { class: 'badge-green', label: 'Completed' },
  active: { class: 'badge-blue', label: 'Active' },
  cancelled: { class: 'badge-red', label: 'Cancelled' }
}

const badgeClass = computed(() => statusMap[props.status]?.class || 'badge-blue')
const label = computed(() => statusMap[props.status]?.label || props.status)
</script>
