<template>
  <div class="world-map-wrapper">
    <div class="map-grid">
      <div
        v-for="region in regions"
        :key="region.name"
        class="map-region"
        :style="{
          left: region.x + '%',
          top: region.y + '%'
        }"
        @mouseenter="hoveredRegion = region"
        @mouseleave="hoveredRegion = null"
      >
        <div class="map-dot" :style="{ background: region.color, boxShadow: `0 0 12px ${region.color}` }">
          <span class="dot-pulse"></span>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="hoveredRegion" class="map-tooltip" :style="{ left: hoveredX + 'px', top: hoveredY + 'px' }">
        <strong>{{ hoveredRegion.name }}</strong>
        <p>{{ hoveredRegion.properties }} Properties</p>
        <p>{{ hoveredRegion.revenue }}</p>
      </div>
    </Transition>

    <div class="map-legend">
      <div v-for="item in legendItems" :key="item.label" class="legend-item">
        <span class="legend-dot" :style="{ background: item.color }"></span>
        {{ item.label }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const hoveredRegion = ref(null)

const hoveredX = ref(0)
const hoveredY = ref(0)

const regions = [
  { name: 'New York, USA', x: 22, y: 32, color: '#165DFF', properties: 245, revenue: '$12.4M Revenue' },
  { name: 'Los Angeles, USA', x: 12, y: 35, color: '#165DFF', properties: 189, revenue: '$9.2M Revenue' },
  { name: 'London, UK', x: 45, y: 25, color: '#165DFF', properties: 312, revenue: '$18.7M Revenue' },
  { name: 'Berlin, Germany', x: 50, y: 22, color: '#165DFF', properties: 156, revenue: '$7.8M Revenue' },
  { name: 'Dubai, UAE', x: 60, y: 38, color: '#FF7D00', properties: 420, revenue: '$25.1M Revenue' },
  { name: 'Tokyo, Japan', x: 82, y: 28, color: '#165DFF', properties: 198, revenue: '$11.3M Revenue' },
  { name: 'Sydney, Australia', x: 80, y: 58, color: '#165DFF', properties: 167, revenue: '$8.9M Revenue' },
  { name: 'Singapore', x: 72, y: 42, color: '#00B42A', properties: 289, revenue: '$15.6M Revenue' },
  { name: 'Toronto, Canada', x: 24, y: 22, color: '#165DFF', properties: 134, revenue: '$6.5M Revenue' },
  { name: 'Sao Paulo, Brazil', x: 32, y: 55, color: '#165DFF', properties: 98, revenue: '$4.2M Revenue' },
  { name: 'Paris, France', x: 47, y: 28, color: '#165DFF', properties: 201, revenue: '$10.8M Revenue' }
]

const legendItems = [
  { label: 'High Density', color: '#165DFF' },
  { label: 'Growing Market', color: '#00B42A' },
  { label: 'Premium', color: '#FF7D00' }
]
</script>

<style scoped>
.world-map-wrapper {
  position: relative;
  height: 100%;
  background: linear-gradient(180deg, #F0F5FF 0%, #F5F7FA 100%);
  border-radius: var(--radius-md);
  overflow: hidden;
  min-height: 320px;
}

.map-grid {
  position: relative;
  width: 100%;
  height: 100%;
  background-image:
    radial-gradient(circle, var(--color-border-light) 1px, transparent 1px);
  background-size: 24px 24px;
}

.map-region {
  position: absolute;
  transform: translate(-50%, -50%);
  cursor: pointer;
  z-index: 2;
}

.map-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  position: relative;
  transition: transform 0.2s;
}

.map-region:hover .map-dot {
  transform: scale(1.5);
}

.dot-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: inherit;
  opacity: 0.4;
  animation: pulse 2s ease-out infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.4; }
  100% { transform: scale(3); opacity: 0; }
}

.map-tooltip {
  position: absolute;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  box-shadow: var(--shadow-lg);
  z-index: 10;
  min-width: 160px;
  pointer-events: none;
}

.map-tooltip strong {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  display: block;
  margin-bottom: 4px;
}

.map-tooltip p {
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
  line-height: 1.5;
}

.map-legend {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  gap: 16px;
  z-index: 3;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
