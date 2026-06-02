<template>
  <div class="stat-card card card-hover">
    <div class="stat-header">
      <div class="stat-icon" :style="{ background: iconBg, color: iconColor }">
        <i :class="icon"></i>
      </div>
      <div class="stat-progress">
        <svg viewBox="0 0 36 36" class="progress-ring">
          <path
            class="progress-bg"
            d="M18 2.0845
              a 15.9155 15.9155 0 0 1 0 31.831
              a 15.9155 15.9155 0 0 1 0 -31.831"
          />
          <path
            class="progress-fill"
            :stroke="iconColor"
            :stroke-dasharray="`${percentage}, 100`"
            d="M18 2.0845
              a 15.9155 15.9155 0 0 1 0 31.831
              a 15.9155 15.9155 0 0 1 0 -31.831"
          />
        </svg>
        <span class="progress-text" :style="{ color: iconColor }">{{ percentage }}%</span>
      </div>
    </div>
    <div class="stat-body">
      <h3 class="stat-value">{{ value }}</h3>
      <p class="stat-label">{{ label }}</p>
    </div>
    <div class="stat-trend" :class="trendDirection">
      <i :class="trendDirection === 'up' ? 'ri-arrow-up-line' : 'ri-arrow-down-line'"></i>
      <span>{{ trend }}% vs last month</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, required: true },
  iconBg: { type: String, default: '#E8F0FE' },
  iconColor: { type: String, default: '#165DFF' },
  label: { type: String, required: true },
  value: { type: String, required: true },
  percentage: { type: Number, default: 0 },
  trend: { type: Number, default: 0 },
  trendDirection: { type: String, default: 'up' }
})
</script>

<style scoped>
.stat-card {
  padding: 20px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-progress {
  position: relative;
  width: 40px;
  height: 40px;
}

.progress-ring {
  width: 40px;
  height: 40px;
  transform: rotate(-90deg);
}

.progress-bg {
  fill: none;
  stroke: var(--color-border-light);
  stroke-width: 3;
}

.progress-fill {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.6s ease;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 9px;
  font-weight: var(--font-weight-bold);
}

.stat-body {
  margin-bottom: 12px;
}

.stat-value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  padding: 3px 8px;
  border-radius: 10px;
}

.stat-trend.up {
  color: var(--color-success);
  background: var(--color-success-bg);
}

.stat-trend.down {
  color: var(--color-danger);
  background: var(--color-danger-bg);
}

.stat-trend i {
  font-size: 12px;
}
</style>
