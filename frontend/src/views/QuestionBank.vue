<template>
  <div class="qb-page">
    <div class="page-header">
      <h2 class="page-title">题库练习</h2>
      <div class="header-actions">
        <button class="btn btn-outline" @click="openHistory">
          <i class="ri-history-line"></i> 答题历史
        </button>
        <button class="btn btn-outline" @click="randomPick">
          <i class="ri-shuffle-line"></i> 随机一题
        </button>
        <button class="btn btn-primary" @click="startExam">
          <i class="ri-file-list-3-line"></i> 随机组卷
        </button>
      </div>
    </div>

    <div class="stat-cards">
      <div class="card stat-card">
        <span class="stat-label">题库总量</span>
        <span class="stat-value" style="color:#165DFF">{{ stats.total || 0 }}</span>
        <span class="stat-sub">道编程题</span>
      </div>
      <div class="card stat-card">
        <span class="stat-label">简单</span>
        <span class="stat-value" style="color:#00B42A">{{ stats.easy || 0 }}</span>
        <span class="stat-sub">道</span>
      </div>
      <div class="card stat-card">
        <span class="stat-label">中等</span>
        <span class="stat-value" style="color:#FF7D00">{{ stats.medium || 0 }}</span>
        <span class="stat-sub">道</span>
      </div>
      <div class="card stat-card">
        <span class="stat-label">困难</span>
        <span class="stat-value" style="color:#F53F3F">{{ stats.hard || 0 }}</span>
        <span class="stat-sub">道</span>
      </div>
    </div>

    <div class="card">
      <div class="filters">
        <div class="search-box">
          <i class="ri-search-line"></i>
          <input v-model="keyword" placeholder="搜索题目..." @keyup.enter="searchQuestions" />
        </div>
        <select v-model="filterDifficulty" @change="searchQuestions">
          <option value="">全部难度</option>
          <option value="easy">简单</option>
          <option value="medium">中等</option>
          <option value="hard">困难</option>
        </select>
        <select v-model="filterPoint" @change="searchQuestions">
          <option value="">全部知识点</option>
          <option v-for="p in knowledgePoints" :key="p" :value="p">{{ p }}</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">习题列表</h3>
        <span class="pagination-info">共 {{ totalCount }} 题</span>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>#</th>
              <th>题目</th>
              <th>知识点</th>
              <th>难度</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="questions.length === 0">
              <td colspan="5" class="empty-row">暂无题目</td>
            </tr>
            <tr v-for="(q, i) in questions" :key="q.id">
              <td>{{ page * pageSize + i + 1 }}</td>
              <td>
                <span class="q-title">{{ q.title }}</span>
              </td>
              <td>
                <span class="badge badge-blue tag-sm">{{ q.knowledgePoint }}</span>
              </td>
              <td>
                <span class="badge tag-sm" :class="diffBadge(q.difficulty)">{{ diffLabel(q.difficulty) }}</span>
              </td>
              <td>
                <button class="btn btn-sm btn-primary" @click="openDetail(q)">查看详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination-bar" v-if="totalCount > pageSize">
        <button class="btn btn-sm btn-ghost" :disabled="page <= 0" @click="page--; searchQuestions()">上一页</button>
        <span>第 {{ page + 1 }} / {{ Math.ceil(totalCount / pageSize) || 1 }} 页</span>
        <button class="btn btn-sm btn-ghost" :disabled="(page + 1) * pageSize >= totalCount" @click="page++; searchQuestions()">下一页</button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="detailVisible" class="dialog-overlay" @click.self="detailVisible = false">
        <div class="dialog-card wide">
          <div class="dialog-header">
            <h3>{{ currentQ.title }}</h3>
            <button class="dialog-close" @click="detailVisible = false">
              <i class="ri-close-line"></i>
            </button>
          </div>
          <div class="dialog-body">
            <div class="dialog-tags">
              <span class="badge tag-sm" :class="diffBadge(currentQ.difficulty)">{{ diffLabel(currentQ.difficulty) }}</span>
              <span class="badge badge-blue tag-sm">{{ currentQ.knowledgePoint }}</span>
            </div>

            <div class="detail-section" v-if="currentQ.problem">
              <h4 class="detail-label">题目描述</h4>
              <pre class="detail-content">{{ displayText(currentQ.problem) }}</pre>
            </div>

            <div class="detail-row" v-if="currentQ.inputExample || currentQ.outputExample">
              <div class="detail-half" v-if="currentQ.inputExample">
                <h4 class="detail-label">输入示例</h4>
                <pre class="detail-code">{{ displayText(currentQ.inputExample) }}</pre>
              </div>
              <div class="detail-half" v-if="currentQ.outputExample">
                <h4 class="detail-label">输出示例</h4>
                <pre class="detail-code">{{ displayText(currentQ.outputExample) }}</pre>
              </div>
            </div>

            <div class="detail-section">
              <h4 class="detail-label">编写代码</h4>
              <textarea
                v-model="userCode"
                class="code-editor"
                rows="12"
                placeholder="在此编写你的C语言代码..."
                spellcheck="false"
              ></textarea>
            </div>

            <div class="detail-section" v-if="judgeResult !== null">
              <div v-if="judgeResult.compileError" class="judge-result compile-error">
                <div class="judge-result-header">
                  <i class="ri-error-warning-fill"></i>
                  <strong>编译错误</strong>
                </div>
                <pre class="judge-output">{{ judgeResult.compileError }}</pre>
              </div>
              <div v-else-if="judgeResult.error" class="judge-result runtime-error">
                <div class="judge-result-header">
                  <i class="ri-close-circle-fill"></i>
                  <strong>运行错误</strong>
                </div>
                <pre class="judge-output">{{ judgeResult.error }}</pre>
              </div>
              <div v-else class="judge-result" :class="judgeResult.correct ? 'correct' : 'wrong'">
                <div class="judge-result-header">
                  <i :class="judgeResult.correct ? 'ri-checkbox-circle-fill' : 'ri-close-circle-fill'"></i>
                  <strong>{{ judgeResult.correct ? '回答正确！ ✅' : '回答错误 ❌' }}</strong>
                </div>
                <div class="judge-compare">
                  <div class="judge-col">
                    <span class="judge-label">期望输出</span>
                    <pre class="judge-output">{{ judgeResult.expected }}</pre>
                  </div>
                  <div class="judge-col">
                    <span class="judge-label">实际输出</span>
                    <pre class="judge-output">{{ judgeResult.actual }}</pre>
                  </div>
                </div>
              </div>
            </div>

            <div class="detail-section" v-if="showingHint && currentQ.answerHint">
              <h4 class="detail-label">解题提示</h4>
              <pre class="detail-content hint-box">{{ displayText(currentQ.answerHint) }}</pre>
            </div>

            <div class="detail-section" v-if="showingHint && currentQ.codeTemplate">
              <h4 class="detail-label">代码模板</h4>
              <pre class="detail-code code-block">{{ displayText(currentQ.codeTemplate) }}</pre>
            </div>
          </div>
          <div class="dialog-footer">
            <button v-if="!showingHint" class="btn btn-outline" @click="showingHint = true">
              <i class="ri-lightbulb-line"></i> 显示提示
            </button>
            <button v-else class="btn btn-outline" @click="showingHint = false">
              <i class="ri-eye-off-line"></i> 隐藏提示
            </button>
            <div class="footer-spacer"></div>
            <button class="btn btn-primary" @click="submitCode" :disabled="judging">
              <i :class="judging ? 'ri-loader-4-line' : 'ri-play-fill'"></i>
              {{ judging ? '判题中...' : '提交运行' }}
            </button>
            <button class="btn btn-outline" @click="detailVisible = false">关闭</button>
            <button class="btn btn-ghost" @click="nextRandom">下一题</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="historyVisible" class="dialog-overlay" @click.self="historyVisible = false">
        <div class="dialog-card wide">
          <div class="dialog-header">
            <h3>答题历史</h3>
            <button class="dialog-close" @click="historyVisible = false">
              <i class="ri-close-line"></i>
            </button>
          </div>
          <div class="dialog-body">
            <div v-if="historyList.length === 0" class="empty-row">暂无答题记录</div>
            <table v-else class="data-table">
              <thead>
                <tr>
                  <th>时间</th>
                  <th>题目</th>
                  <th>结果</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="h in historyList" :key="h.id">
                  <td>{{ formatTime(h.createdAt) }}</td>
                  <td>{{ h.questionTitle }}</td>
                  <td>
                    <span class="badge tag-sm" :class="resultBadge(h.result)">{{ resultLabel(h.result) }}</span>
                  </td>
                  <td>
                    <button class="btn btn-sm btn-ghost" @click="viewHistoryDetail(h)">查看</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="dialog-footer">
            <button v-if="historyList.length > 0" class="btn btn-danger btn-outline" @click="clearHistory" :disabled="clearingHistory">
              <i class="ri-delete-bin-line"></i> {{ clearingHistory ? '清除中...' : '清除全部记录' }}
            </button>
            <div class="footer-spacer"></div>
            <button class="btn btn-outline" @click="historyVisible = false">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="historyDetailTarget" class="dialog-overlay" @click.self="historyDetailTarget = null">
        <div class="dialog-card wide">
          <div class="dialog-header">
            <h3>{{ historyDetailTarget.questionTitle }}</h3>
            <button class="dialog-close" @click="historyDetailTarget = null">
              <i class="ri-close-line"></i>
            </button>
          </div>
          <div class="dialog-body">
            <div class="dialog-tags">
              <span class="badge tag-sm" :class="resultBadge(historyDetailTarget.result)">{{ resultLabel(historyDetailTarget.result) }}</span>
              <span class="badge badge-blue tag-sm">{{ formatTime(historyDetailTarget.createdAt) }}</span>
            </div>

            <div class="detail-section">
              <h4 class="detail-label">提交的代码</h4>
              <pre class="detail-code code-block">{{ historyDetailTarget.userCode }}</pre>
            </div>

            <div class="detail-section" v-if="historyDetailTarget.expectedOutput">
              <h4 class="detail-label">期望输出</h4>
              <pre class="detail-code">{{ displayText(historyDetailTarget.expectedOutput) }}</pre>
            </div>

            <div class="detail-section" v-if="historyDetailTarget.actualOutput">
              <h4 class="detail-label">实际输出</h4>
              <pre class="detail-code">{{ displayText(historyDetailTarget.actualOutput) }}</pre>
            </div>

            <div class="detail-section" v-if="historyDetailTarget.errorMessage">
              <h4 class="detail-label">错误信息</h4>
              <pre class="detail-content hint-box">{{ displayText(historyDetailTarget.errorMessage) }}</pre>
            </div>
          </div>
          <div class="dialog-footer">
            <button class="btn btn-outline" @click="historyDetailTarget = null">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'

const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''

const questions = ref([])
const knowledgePoints = ref([])
const stats = reactive({ total: 0, easy: 0, medium: 0, hard: 0 })
const totalCount = ref(0)
const page = ref(0)
const pageSize = 15
const keyword = ref('')
const filterDifficulty = ref('')
const filterPoint = ref('')

const detailVisible = ref(false)
const currentQ = ref({})
const showingHint = ref(false)
const userCode = ref('')
const judging = ref(false)
const judgeResult = ref(null)

const historyVisible = ref(false)
const historyList = ref([])
const historyDetailTarget = ref(null)
const clearingHistory = ref(false)

onMounted(() => {
  loadStats()
  loadKnowledgePoints()
  searchQuestions()
})

async function searchQuestions() {
  try {
    const params = new URLSearchParams({
      page: page.value,
      pageSize: pageSize,
      difficulty: filterDifficulty.value,
      knowledgePoint: filterPoint.value,
      keyword: keyword.value
    })
    const res = await fetch(`${API_BASE}/quiz/list?${params}`, {
      headers: { 'Authorization': token() }
    })
    const data = await res.json()
    questions.value = data.list || []
    totalCount.value = data.total || 0
  } catch (e) { console.error('加载题目失败', e) }
}

async function loadStats() {
  try {
    const res = await fetch(`${API_BASE}/quiz/stats`, {
      headers: { 'Authorization': token() }
    })
    const data = await res.json()
    Object.assign(stats, data)
  } catch (e) { console.error('加载统计失败', e) }
}

async function loadKnowledgePoints() {
  try {
    const res = await fetch(`${API_BASE}/quiz/knowledge-points`, {
      headers: { 'Authorization': token() }
    })
    knowledgePoints.value = await res.json()
  } catch (e) { console.error('加载知识点失败', e) }
}

async function openDetail(q) {
  showingHint.value = false
  try {
    const res = await fetch(`${API_BASE}/quiz/detail/${q.id}`, {
      headers: { 'Authorization': token() }
    })
    currentQ.value = await res.json()
    userCode.value = unescapeCodeTemplate(currentQ.value.codeTemplate)
    judgeResult.value = null
    detailVisible.value = true
  } catch (e) {
    currentQ.value = q
    userCode.value = unescapeCodeTemplate(q.codeTemplate)
    judgeResult.value = null
    detailVisible.value = true
  }
}

async function submitCode() {
  judging.value = true
  judgeResult.value = null
  try {
    const res = await fetch(`${API_BASE}/quiz/judge`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify({
        questionId: currentQ.value.id,
        code: userCode.value
      })
    })
    judgeResult.value = await res.json()
  } catch (e) {
    judgeResult.value = { correct: false, error: '判题服务连接失败: ' + e.message }
  } finally {
    judging.value = false
  }
}

function randomPick() {
  const idx = Math.floor(Math.random() * questions.value.length)
  if (questions.value[idx]) openDetail(questions.value[idx])
}

function nextRandom() {
  if (questions.value.length === 0) return
  const idx = Math.floor(Math.random() * questions.value.length)
  showingHint.value = false
  openDetail(questions.value[idx])
}

async function openHistory() {
  historyVisible.value = true
  try {
    const res = await fetch(`${API_BASE}/quiz/history?limit=50`, {
      headers: { 'Authorization': token() }
    })
    const data = await res.json()
    historyList.value = data.data || []
  } catch (e) {
    historyList.value = []
  }
}

function viewHistoryDetail(h) {
  historyDetailTarget.value = h
}

async function clearHistory() {
  if (!confirm('确定要清除所有答题历史记录吗？此操作不可撤销。')) return
  clearingHistory.value = true
  try {
    await fetch(`${API_BASE}/quiz/history`, {
      method: 'DELETE',
      headers: { 'Authorization': token() }
    })
    historyList.value = []
  } catch (e) { alert('清除失败') }
  finally { clearingHistory.value = false }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function displayText(text) {
  if (!text) return ''
  return text.replace(/\\n/g, '\n')
}

function unescapeCodeTemplate(template) {
  if (!template) return ''
  return template.replace(/\\n/g, '\n')
}

function resultLabel(r) {
  if (r === 'correct') return '正确'
  if (r === 'wrong') return '错误'
  if (r === 'compile_error') return '编译错误'
  if (r === 'runtime_error') return '运行错误'
  return r
}

function resultBadge(r) {
  if (r === 'correct') return 'badge-green'
  if (r === 'wrong') return 'badge-red'
  if (r === 'compile_error') return 'badge-orange'
  if (r === 'runtime_error') return 'badge-red'
  return ''
}

async function startExam() {
  const count = prompt('想要生成几道题？（默认10道）', '10')
  const n = parseInt(count) || 10
  const diff = filterDifficulty.value || ''
  try {
    const res = await fetch(`${API_BASE}/quiz/exam`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify({ difficulty: diff, count: n })
    })
    const list = await res.json()
    if (list.length > 0) {
      questions.value = list
      totalCount.value = list.length
      page.value = 0
    }
  } catch (e) { alert('组卷失败') }
}

function diffLabel(d) {
  if (d === 'easy') return '简单'
  if (d === 'hard') return '困难'
  return '中等'
}
function diffBadge(d) {
  if (d === 'easy') return 'badge-green'
  if (d === 'hard') return 'badge-red'
  return 'badge-orange'
}
</script>

<style scoped>
.qb-page { display: flex; flex-direction: column; gap: 20px; }

.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }
.header-actions { display: flex; gap: 8px; }

.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { padding: 24px 16px; text-align: center; }
.stat-label { font-size: var(--font-size-xs); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; display: block; }
.stat-value { font-size: 36px; font-weight: var(--font-weight-bold); line-height: 1; display: block; }
.stat-sub { font-size: var(--font-size-xs); color: var(--color-text-tertiary); margin-top: 2px; display: block; }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }
.card-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid var(--color-border-light); }
.card-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.filters { display: flex; gap: 12px; padding: 16px 20px; flex-wrap: wrap; align-items: center; }
.search-box { position: relative; }
.search-box i { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: var(--color-text-tertiary); font-size: 14px; }
.search-box input { padding-left: 32px; width: 220px; }
.filters select, .filters input {
  padding: 8px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md);
  font-size: var(--font-size-sm); background: var(--color-bg-page); color: var(--color-text-secondary);
}

.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 12px 20px; text-align: left; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); background: var(--color-bg-page); border-bottom: 1px solid var(--color-border-light); }
.data-table td { padding: 12px 20px; font-size: var(--font-size-sm); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border-light); }
.data-table tbody tr:hover { background: var(--color-bg-hover); }
.empty-row { text-align: center; color: var(--color-text-placeholder) !important; padding: 36px 0 !important; }
.q-title { font-weight: var(--font-weight-medium); color: var(--color-text-primary); }
.q-title:hover { color: var(--color-primary); cursor: pointer; }

.pagination-bar { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 16px; }
.pagination-info { font-size: var(--font-size-xs); color: var(--color-text-tertiary); }

.badge { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 12px; font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); }
.badge-sm, .tag-sm { padding: 2px 8px; font-size: 11px; border-radius: 10px; }
.badge-green { background: var(--color-success-bg, #e8f8ee); color: #00B42A; }
.badge-orange { background: #fff7e6; color: #FF7D00; }
.badge-red { background: #ffece8; color: #F53F3F; }
.badge-blue { background: var(--color-primary-bg, #e8f0fe); color: var(--color-primary, #165DFF); }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog-card { background: var(--color-bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); max-width: 900px; width: 90vw; max-height: 85vh; display: flex; flex-direction: column; }
.dialog-card.wide { max-width: 960px; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; border-bottom: 1px solid var(--color-border-light); }
.dialog-header h3 { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin: 0; }
.dialog-close { background: none; border: none; color: var(--color-text-tertiary); font-size: 20px; cursor: pointer; padding: 4px; }
.dialog-body { padding: 20px 24px; overflow-y: auto; flex: 1; }
.dialog-tags { display: flex; gap: 8px; margin-bottom: 16px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 16px 24px; border-top: 1px solid var(--color-border-light); align-items: center; }
.footer-spacer { flex: 1; }

.code-editor {
  width: 100%;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  padding: 14px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #1e1e2e;
  color: #cdd6f4;
  resize: vertical;
  box-sizing: border-box;
  tab-size: 4;
}
.code-editor:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(22,93,255,0.15);
  outline: none;
}

.judge-result { border-radius: var(--radius-md); padding: 14px 16px; margin-top: 0; }
.judge-result.correct { background: #e8f8ee; border: 1px solid #b7ebc8; }
.judge-result.wrong { background: #ffece8; border: 1px solid #f5c2b7; }
.judge-result.compile-error { background: #fffbe6; border: 1px solid #ffe58f; }
.judge-result.runtime-error { background: #fff0f0; border: 1px solid #ffb8b8; }
.judge-result-header { display: flex; align-items: center; gap: 8px; font-size: var(--font-size-sm); margin-bottom: 10px; }
.judge-result.correct .judge-result-header { color: #00B42A; }
.judge-result.wrong .judge-result-header { color: #F53F3F; }
.judge-output {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  margin: 4px 0 0;
  padding: 8px 10px;
  background: rgba(0,0,0,0.04);
  border-radius: 4px;
  line-height: 1.5;
}
.judge-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.judge-label { font-size: 11px; font-weight: 600; color: var(--color-text-tertiary); text-transform: uppercase; margin-bottom: 2px; display: block; }
.judge-col { min-width: 0; }

.detail-section { margin-bottom: 16px; }
.detail-label { font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-secondary); margin-bottom: 8px; }
.detail-content { font-size: var(--font-size-sm); color: var(--color-text-primary); line-height: 1.8; white-space: pre-wrap; font-family: inherit; background: var(--color-bg-page); padding: 12px 16px; border-radius: var(--radius-md); margin: 0; }
.detail-code { font-size: var(--font-size-xs); font-family: 'Consolas', 'Courier New', monospace; background: #1e1e2e; color: #cdd6f4; padding: 12px 16px; border-radius: var(--radius-md); overflow-x: auto; white-space: pre; margin: 0; }
.code-block { background: #1e1e2e; color: #cdd6f4; }
.hint-box { background: #fffbe6; border: 1px solid #ffe58f; color: #614700; }
.detail-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.detail-half { min-width: 0; }

.btn-danger.btn-outline { color: #F53F3F; border-color: #F53F3F; background: transparent; }
.btn-danger.btn-outline:hover { background: #ffece8; }

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .detail-row { grid-template-columns: 1fr; }
  .filters { flex-direction: column; }
  .filters select, .filters input, .search-box input { width: 100%; }
}
</style>
