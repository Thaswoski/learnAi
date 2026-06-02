<template>
  <div class="chart-container" ref="chartRef"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '300px' }
})

const chartRef = ref(null)
let chart = null
let resizeObserver = null

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  chart.setOption(props.option)
}

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  initChart()
  resizeObserver = new ResizeObserver(resizeChart)
  if (chartRef.value) resizeObserver.observe(chartRef.value)
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  resizeObserver?.disconnect()
  chart?.dispose()
})

watch(() => props.option, (newOpt) => {
  chart?.setOption(newOpt)
}, { deep: true })
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: v-bind(height);
}
</style>
