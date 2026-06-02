<template>
  <div class="dashboard">
    <div class="page-header">
      <h2 class="page-title">学习仪表盘</h2>
      <button class="btn btn-outline" @click="refresh" :disabled="loading">
        <i :class="loading ? 'ri-loader-4-line' : 'ri-refresh-line'"></i>
        {{ loading ? '加载中...' : '刷新数据' }}
      </button>
    </div>

    <div class="stat-cards">
      <div class="card stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <i :class="stat.icon" :style="{ color: stat.color }"></i>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">今日学习任务</h3>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>所属课程</th>
              <th>预计时长</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="todayTasks.length === 0">
              <td colspan="5" class="empty-row">
                <router-link to="/app/path" style="color:var(--color-primary)">去学习路径</router-link> 生成今日计划
              </td>
            </tr>
            <tr v-for="task in todayTasks" :key="task.name">
              <td>{{ task.name }}</td>
              <td>{{ task.course }}</td>
              <td>{{ task.duration }}</td>
              <td>
                <span class="badge" :class="task.status === '已完成' ? 'badge-green' : 'badge-orange'">
                  {{ task.status }}
                </span>
              </td>
              <td>
                <button v-if="task.status !== '已完成'" class="btn btn-sm btn-primary" @click="$router.push('/app/questionbank')">开始练习</button>
                <button v-else class="btn btn-sm btn-ghost">查看详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="charts-row">
      <div class="card chart-card">
        <div class="card-header">
          <h3 class="card-title">每日答题趋势</h3>
        </div>
        <BaseChart :option="timeChartOption" height="280px" />
      </div>
      <div class="card chart-card">
        <div class="card-header">
          <h3 class="card-title">知识点掌握分布</h3>
        </div>
        <BaseChart :option="knowledgeChartOption" height="280px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BaseChart from '@/components/BaseChart.vue'

const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''

const dashboardData = ref(null)
const loading = ref(false)

const stats = computed(() => dashboardData.value?.stats || [
  { label: '今日答题', value: '0题', icon: 'ri-question-answer-line', color: '#165DFF', bg: '#E8F0FE' },
  { label: '累计正确', value: '0', icon: 'ri-check-double-line', color: '#00B42A', bg: '#E8FFEA' },
  { label: '生成资源', value: '0份', icon: 'ri-file-copy-line', color: '#FF7D00', bg: '#FFF7E8' },
  { label: '学习进度', value: '0%', icon: 'ri-bar-chart-2-line', color: '#722ED1', bg: '#F5F0FF' }
])

const todayTasks = computed(() => dashboardData.value?.todayTasks || [])

const timeChartOption = computed(() => {
  const chart = dashboardData.value?.weeklyChart
  return {
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#E5E6EB', textStyle: { color: '#1D2129', fontSize: 12 } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category', data: chart?.dates || [],
      axisLine: { lineStyle: { color: '#E5E6EB' } }, axisTick: { show: false },
      axisLabel: { color: '#86909C', fontSize: 11 }
    },
    yAxis: {
      type: 'value', name: '答题数',
      nameTextStyle: { color: '#86909C', fontSize: 11 },
      splitLine: { lineStyle: { color: '#F2F3F5', type: 'dashed' } },
      axisLabel: { color: '#86909C', fontSize: 11 }
    },
    series: [{
      data: chart?.values || [], type: 'line', smooth: true,
      symbol: 'circle', symbolSize: 8,
      lineStyle: { color: '#165DFF', width: 2 },
      itemStyle: { color: '#165DFF', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(22,93,255,0.15)' }, { offset: 1, color: 'rgba(22,93,255,0.01)' }] }
      }
    }]
  }
})

const knowledgeChartOption = computed(() => {
  const data = dashboardData.value?.knowledgeChart || [
    { value: 0, name: '已掌握', color: '#165DFF' },
    { value: 0, name: '学习中', color: '#3C7EFF' },
    { value: 1, name: '未学习', color: '#C9CDD4' }
  ]
  return {
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#E5E6EB', textStyle: { color: '#1D2129', fontSize: 12 } },
    legend: { bottom: '5%', textStyle: { color: '#4E5969', fontSize: 12 } },
    series: [{
      name: '知识点', type: 'pie', radius: ['45%', '72%'], center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: data.map(d => ({ value: d.value, name: d.name, itemStyle: { color: d.color } }))
    }]
  }
})

onMounted(() => loadDashboard())

async function loadDashboard() {
  loading.value = true
  try {
    const res = await fetch(`${API_BASE}/dashboard`, {
      headers: { 'Authorization': token() }
    })
    dashboardData.value = await res.json()
  } catch (e) {
    console.error('加载仪表盘失败', e)
  } finally {
    loading.value = false
  }
}

const refresh = () => loadDashboard()
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 20px; }

.page-header { display: flex; justify-content: space-between; align-items: center; }

.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }

.stat-card { display: flex; align-items: center; gap: 16px; padding: 20px; transition: transform 0.2s, box-shadow 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }

.stat-icon { width: 48px; height: 48px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0; }

.stat-body { display: flex; flex-direction: column; }
.stat-value { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); line-height: 1.2; }
.stat-label { font-size: var(--font-size-xs); color: var(--color-text-tertiary); margin-top: 2px; }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); overflow: hidden; }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--color-border-light); }
.card-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 12px 20px; text-align: left; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); background: var(--color-bg-page); border-bottom: 1px solid var(--color-border-light); white-space: nowrap; }
.data-table td { padding: 12px 20px; font-size: var(--font-size-sm); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border-light); }
.data-table tbody tr:hover { background: var(--color-bg-hover); }
.empty-row { text-align: center; color: var(--color-text-tertiary); padding: 24px 0 !important; }

.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { padding: 0; }

.badge { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 12px; font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); }
.badge-green { background: #e8f8ee; color: #00B42A; }
.badge-orange { background: #fff7e6; color: #FF7D00; }

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}
</style>
