<template>
  <div class="eval-page">
    <div class="page-header">
      <h2 class="page-title">学习效果评估</h2>
      <button class="btn btn-primary" @click="generateReport">生成详细报告</button>
    </div>

    <div v-if="!ready" class="card eval-empty">
      <div class="eval-empty-icon">
        <i class="ri-bar-chart-2-line"></i>
      </div>
      <h3>数据收集中</h3>
      <p>完成至少 {{ evalData?.requiredQuestions || 10 }} 道题目后，系统将自动生成评估报告。</p>
      <div class="eval-progress">
        <div class="eval-progress-bar">
          <div class="eval-progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <span class="eval-progress-text">{{ evalData?.totalQuestions || 0 }} / {{ evalData?.requiredQuestions || 10 }}</span>
      </div>
      <router-link to="/app/questionbank" class="btn btn-primary">去题库练习</router-link>
    </div>

    <template v-else>
    <div class="score-cards">
      <div class="card score-card">
        <span class="score-label">综合得分</span>
        <span class="score-value" style="color:#165DFF">{{ overallScore }}</span>
        <span class="score-desc">{{ overallScore >= 80 ? '优秀' : overallScore >= 60 ? '良好' : '需努力' }}</span>
      </div>
      <div class="card score-card">
        <span class="score-label">正确率</span>
        <span class="score-value" style="color:#00B42A">{{ accuracyRate }}%</span>
        <span class="score-desc">共答题 {{ evalData?.totalQuestions || 0 }} 道</span>
      </div>
      <div class="card score-card">
        <span class="score-label">知识掌握率</span>
        <span class="score-value" style="color:#FF7D00">{{ averageMastery }}%</span>
        <span class="score-desc">{{ averageMastery >= 70 ? '良好基础' : '需要加强' }}</span>
      </div>
    </div>

    <div class="charts-row">
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">各维度能力评估</h3>
        </div>
        <BaseChart :option="abilityRadarOption" height="320px" />
      </div>
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">学习成绩趋势</h3>
        </div>
        <BaseChart :option="scoreTrendOption" height="320px" />
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">知识点掌握情况</h3>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>知识点</th>
              <th>掌握程度</th>
              <th>学习建议</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in knowledgeMastery" :key="item.name">
              <td>{{ item.name }}</td>
              <td>
                <div class="progress-cell">
                  <div class="progress-bar">
                    <div class="progress-fill" :style="{ width: item.mastery + '%', background: item.mastery >= 80 ? '#00B42A' : item.mastery >= 60 ? '#FF7D00' : '#F53F3F' }"></div>
                  </div>
                  <span class="progress-num">{{ item.mastery }}%</span>
                </div>
              </td>
              <td>{{ item.suggestion }}</td>
              <td><button class="btn btn-sm btn-primary">强化学习</button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card suggestions-card">
      <div class="card-header">
        <h3 class="card-title">个性化改进建议</h3>
      </div>
      <div class="suggestions-list">
        <div v-for="(s, idx) in suggestions" :key="idx" class="suggestion-item" :class="s.type">
          <i :class="s.type === 'warning' ? 'ri-error-warning-line' : s.type === 'info' ? 'ri-information-line' : 'ri-checkbox-circle-line'"></i>
          <div>
            <strong>{{ s.title }}</strong>
            <p>{{ s.content }}</p>
          </div>
        </div>
        <div v-if="suggestions.length === 0" class="suggestion-item info">
          <i class="ri-information-line"></i>
          <div>
            <strong>暂无评估数据</strong>
            <p>去题库页面完成一些题目后，系统将自动生成评估报告。</p>
          </div>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import BaseChart from '@/components/BaseChart.vue'

const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''

const evalData = ref(null)
const loading = ref(true)

const ready = computed(() => evalData.value?.ready === true)
const progressPercent = computed(() => {
  const total = evalData.value?.totalQuestions || 0
  const required = evalData.value?.requiredQuestions || 10
  return Math.min(100, Math.round((total / required) * 100))
})

const knowledgeMastery = computed(() => evalData.value?.knowledgeMastery || [])
const suggestions = computed(() => evalData.value?.suggestions || [])
const overallScore = computed(() => evalData.value?.overallScore || 0)
const learningEfficiency = computed(() => evalData.value?.learningEfficiency || 0)
const averageMastery = computed(() => evalData.value?.averageMastery || 0)
const accuracyRate = computed(() => evalData.value?.accuracyRate || 0)

const abilityRadarOption = computed(() => {
  const names = evalData.value?.dimensionNames || ['理论理解', '编程实现', '问题解决', '知识应用', '创新思维']
  const values = evalData.value?.dimensionValues || [0, 0, 0, 0, 0]
  return {
    tooltip: {},
    radar: {
      center: ['50%', '45%'],
      radius: '65%',
      indicator: names.map(n => ({ name: n, max: 100 })),
      axisName: { color: '#86909C', fontSize: 11 }
    },
    series: [{
      type: 'radar',
      data: [{ value: values, name: '你的能力' }],
      areaStyle: { color: 'rgba(22,93,255,0.08)' },
      lineStyle: { color: '#165DFF', width: 2 },
      itemStyle: { color: '#165DFF' }
    }]
  }
})

const scoreTrendOption = computed(() => {
  const trend = evalData.value?.weeklyTrend
  const weeks = trend?.weeks || ['暂无数据']
  const values = trend?.values || [0]
  return {
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#E5E6EB', textStyle: { color: '#1D2129', fontSize: 12 } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: {
      type: 'category', data: weeks,
      axisLine: { lineStyle: { color: '#E5E6EB' } },
      axisTick: { show: false },
      axisLabel: { color: '#86909C', fontSize: 11, rotate: weeks.length > 7 ? 30 : 0 }
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      splitLine: { lineStyle: { color: '#F2F3F5', type: 'dashed' } },
      axisLabel: { color: '#86909C', fontSize: 11 }
    },
    series: [{
      data: values, type: 'line', smooth: true,
      symbol: 'circle', symbolSize: 8,
      lineStyle: { color: '#165DFF', width: 2 },
      itemStyle: { color: '#165DFF', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,93,255,0.15)' }, { offset: 1, color: 'rgba(22,93,255,0.01)' }] }
      },
      markLine: values.length > 0 ? { data: [{ type: 'average', name: '平均值' }], lineStyle: { color: '#00B42A', type: 'dashed' } } : {}
    }]
  }
})

onMounted(() => loadEvaluation())

async function loadEvaluation() {
  try {
    const res = await fetch(`${API_BASE}/evaluation`, {
      headers: { 'Authorization': token() }
    })
    evalData.value = await res.json()
  } catch (e) {
    console.error('加载评估失败', e)
  } finally {
    loading.value = false
  }
}

const generateReport = () => { loadEvaluation() }
</script>

<style scoped>
.eval-page { display: flex; flex-direction: column; gap: 20px; }

.page-header { display: flex; justify-content: space-between; align-items: center; }

.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }

.score-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }

.score-card { padding: 28px 20px; text-align: center; display: flex; flex-direction: column; align-items: center; }

.score-label { font-size: var(--font-size-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 8px; }

.score-value { font-size: 56px; font-weight: var(--font-weight-bold); line-height: 1; }

.score-desc { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-top: 6px; font-weight: var(--font-weight-medium); }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }

.eval-empty { padding: 48px 24px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.eval-empty-icon { width: 64px; height: 64px; border-radius: 50%; background: var(--color-primary-bg, #e8f0fe); display: flex; align-items: center; justify-content: center; }
.eval-empty-icon i { font-size: 28px; color: var(--color-primary, #165DFF); }
.eval-empty h3 { font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin: 0; }
.eval-empty p { color: var(--color-text-tertiary); font-size: var(--font-size-sm); max-width: 360px; margin: 0; line-height: 1.6; }
.eval-progress { display: flex; align-items: center; gap: 12px; width: 320px; max-width: 100%; }
.eval-progress-bar { flex: 1; height: 8px; background: var(--color-bg-page); border-radius: 4px; overflow: hidden; }
.eval-progress-fill { height: 100%; background: var(--color-primary, #165DFF); border-radius: 4px; transition: width 0.4s ease; }
.eval-progress-text { font-size: var(--font-size-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); white-space: nowrap; }

.card-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--color-border-light); }

.card-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.table-wrap { overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; }

.data-table th { padding: 12px 20px; text-align: left; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); background: var(--color-bg-page); border-bottom: 1px solid var(--color-border-light); }

.data-table td { padding: 12px 20px; font-size: var(--font-size-sm); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border-light); }

.data-table tbody tr:hover { background: var(--color-bg-hover); }

.progress-cell { display: flex; align-items: center; gap: 10px; }

.progress-bar { flex: 1; height: 8px; background: var(--color-border-light); border-radius: 4px; overflow: hidden; }

.progress-fill { height: 100%; border-radius: 4px; transition: width 0.6s ease; }

.progress-num { font-size: var(--font-size-xs); color: var(--color-text-tertiary); min-width: 36px; text-align: right; font-weight: var(--font-weight-medium); }

.suggestions-card { border: none; }

.suggestions-list { padding: 20px; display: flex; flex-direction: column; gap: 12px; }

.suggestion-item { display: flex; gap: 12px; padding: 16px; border-radius: var(--radius-md); border: 1px solid; align-items: flex-start; }

.suggestion-item.warning { background: var(--color-warning-bg); border-color: transparent; color: #92400E; }
.suggestion-item.info { background: var(--color-primary-bg); border-color: transparent; color: var(--color-primary); }
.suggestion-item.success { background: var(--color-success-bg); border-color: transparent; color: #0F766E; }

.suggestion-item i { font-size: 20px; margin-top: 1px; }

.suggestion-item strong { font-size: var(--font-size-sm); display: block; margin-bottom: 4px; }

.suggestion-item p { font-size: var(--font-size-sm); opacity: 0.8; line-height: 1.5; }
</style>
