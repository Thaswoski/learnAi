<template>
  <div class="path-page">
    <div class="page-header">
      <h2 class="page-title">个性化学习路径</h2>
      <button class="btn btn-primary" @click="replanPath" :disabled="loading">
        <i :class="loading ? 'ri-loader-4-line' : 'ri-refresh-line'"></i>
        {{ loading ? '规划中...' : '重新规划路径' }}
      </button>
    </div>

    <div v-if="!steps || steps.length === 0" class="card eval-empty">
      <div class="eval-empty-icon">
        <i class="ri-guide-line"></i>
      </div>
      <h3>暂无学习路径</h3>
      <p>去题库页面完成至少5道题目后，DeepSeek 将为你定制专属学习路径和每日练习计划。</p>
      <router-link to="/app/questionbank" class="btn btn-primary">去题库练习</router-link>
    </div>

    <template v-else>
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">C语言学习路径</h3>
      </div>
      <div class="steps-list">
        <div
          v-for="(step, i) in steps"
          :key="step.title + i"
          class="step-item"
          :class="{ active: step.status === '进行中', done: step.status === '已完成' }"
        >
          <div class="step-marker">
            <div class="step-dot" :class="{ active: step.status === '进行中' || step.status === '已完成', done: step.status === '已完成' }">
              <i v-if="step.status === '已完成'" class="ri-check-line"></i>
              <span v-else-if="step.status === '进行中'" class="step-dot-inner"></span>
            </div>
            <div v-if="i < steps.length - 1" class="step-line" :class="{ active: step.status === '已完成' }"></div>
          </div>
          <div class="step-content">
            <div class="step-header">
              <h4 class="step-title">{{ step.title }}</h4>
              <span class="badge" :class="step.statusClass">{{ step.status }}</span>
              <span class="step-duration">预计{{ step.duration }}小时</span>
            </div>
            <div class="step-tags">
              <span v-for="tag in step.tags" :key="tag" class="badge" :class="step.tagClass || 'badge-blue'">{{ tag }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">本周学习计划</h3>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>学习任务</th>
              <th>预计时长</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="plan in weeklyPlan" :key="plan.day">
              <td>{{ plan.day }}</td>
              <td>{{ plan.task }}</td>
              <td>{{ plan.duration }}</td>
              <td>
                <span class="badge" :class="plan.statusClass || 'badge-blue'">{{ plan.status }}</span>
              </td>
              <td>
                <button v-if="plan.status !== '已完成'" class="btn btn-sm btn-primary" @click="gotoQuiz">开始练习</button>
                <button v-else class="btn btn-sm btn-ghost">查看详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''
const router = useRouter()

const steps = ref(null)
const weeklyPlan = ref([])
const loading = ref(false)

onMounted(() => loadPath())

async function loadPath() {
  loading.value = true
  try {
    const res = await fetch(`${API_BASE}/path`, {
      headers: { 'Authorization': token() }
    })
    const data = await res.json()
    steps.value = data.steps || null
    weeklyPlan.value = data.weeklyPlan || []
  } catch (e) {
    console.error('加载学习路径失败', e)
  } finally {
    loading.value = false
  }
}

const replanPath = () => { loadPath() }
const gotoQuiz = () => { router.push('/app/questionbank') }
</script>

<style scoped>
.path-page { display: flex; flex-direction: column; gap: 20px; }

.page-header { display: flex; justify-content: space-between; align-items: center; }

.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }

.card-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--color-border-light); }

.card-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.steps-list { padding: 24px; display: flex; flex-direction: column; gap: 0; }

.step-item { display: flex; gap: 16px; min-height: 72px; }

.step-marker { display: flex; flex-direction: column; align-items: center; width: 28px; flex-shrink: 0; }

.step-dot { width: 28px; height: 28px; border-radius: 50%; border: 2px solid var(--color-border); display: flex; align-items: center; justify-content: center; font-size: 14px; color: var(--color-text-placeholder); background: var(--color-bg-card); flex-shrink: 0; }

.step-dot.done { background: var(--color-success); border-color: var(--color-success); color: #fff; }

.step-dot.active { border-color: var(--color-primary); }

.step-dot-inner { width: 10px; height: 10px; border-radius: 50%; background: var(--color-primary); }

.step-line { width: 2px; flex: 1; background: var(--color-border); margin: 4px 0; }

.step-line.active { background: var(--color-success); }

.step-content { flex: 1; padding-bottom: 20px; }

.step-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }

.step-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.step-duration { font-size: var(--font-size-xs); color: var(--color-text-tertiary); }

.step-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.table-wrap { overflow-x: auto; }

.data-table { width: 100%; border-collapse: collapse; }

.data-table th { padding: 12px 20px; text-align: left; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); background: var(--color-bg-page); border-bottom: 1px solid var(--color-border-light); }

.data-table td { padding: 12px 20px; font-size: var(--font-size-sm); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border-light); }

.data-table tbody tr:hover { background: var(--color-bg-hover); }

.eval-empty { padding: 48px 24px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.eval-empty-icon { width: 64px; height: 64px; border-radius: 50%; background: var(--color-primary-bg, #e8f0fe); display: flex; align-items: center; justify-content: center; }
.eval-empty-icon i { font-size: 28px; color: var(--color-primary, #165DFF); }
.eval-empty h3 { font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin: 0; }
.eval-empty p { color: var(--color-text-tertiary); font-size: var(--font-size-sm); max-width: 420px; margin: 0; line-height: 1.6; }

.badge { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 12px; font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); }
.badge-green { background: var(--color-success-bg, #e8f8ee); color: #00B42A; }
.badge-orange { background: #fff7e6; color: #FF7D00; }
.badge-blue { background: var(--color-primary-bg, #e8f0fe); color: var(--color-primary, #165DFF); }
</style>
