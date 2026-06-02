<template>
  <div class="resource-page">
    <h2 class="page-title">多智能体资源生成</h2>

    <div class="card form-card">
      <h3 class="card-title">选择资源类型</h3>

      <div class="resource-cards">
        <div
          v-for="type in resourceTypes"
          :key="type.value"
          class="resource-card"
          :class="{ active: activeResourceType === type.value }"
          @click="selectType(type.value)"
        >
          <div class="rc-icon"><i :class="type.icon"></i></div>
          <div class="rc-info">
            <div class="rc-name">{{ type.label }}</div>
            <div class="rc-desc">{{ type.desc }}</div>
          </div>
        </div>
      </div>

      <div v-if="activeResourceType && activeResourceType !== 'video'" class="generate-form">
        <h4 class="form-section-title">生成配置</h4>

        <!-- === mindmap === -->
        <template v-if="activeResourceType === 'mindmap'">
          <div class="form-grid-2col">
            <div class="form-group">
              <label>选择搜索资料AI</label>
              <select v-model="form.searchModel" class="form-select">
                <option value="xfsearch">讯飞ONE SEARCH</option>
                <option value="deepseek">DeepSeek</option>
              </select>
            </div>
            <div class="form-group">
              <label>选择生成模型</label>
              <select v-model="form.model" class="form-select">
                <option value="deepseek">DeepSeek V4 Pro</option>
                <option value="spark">讯飞星火 Spark X2</option>
              </select>
            </div>
          </div>
          <div class="form-grid-2col">
            <div class="form-group">
              <label>专业</label>
              <input v-model="form.major" placeholder="例如：软件工程" />
            </div>
            <div class="form-group"></div>
          </div>
        </template>

        <!-- === reading === -->
        <template v-if="activeResourceType === 'reading'">
          <div class="form-grid-3col">
            <div class="form-group">
              <label>搜索AI模型</label>
              <select v-model="form.readingSearchModel" class="form-select">
                <option value="xfsearch">讯飞ONE SEARCH</option>
              </select>
            </div>
            <div class="form-group">
              <label>选择AI文本生成</label>
              <select v-model="form.model" class="form-select">
                <option value="deepseek">DeepSeek V4 Pro</option>
                <option value="spark">讯飞星火 Spark X2</option>
              </select>
            </div>
            <div class="form-group">
              <label>选择图片生成模型</label>
              <select v-model="form.imageModel" class="form-select">
                <option value="seedream">豆包 Seedream 4.0</option>
              </select>
            </div>
          </div>
          <div class="form-grid-2col">
            <div class="form-group">
              <label>专业</label>
              <input v-model="form.major" placeholder="例如：软件工程" />
            </div>
            <div class="form-group"></div>
          </div>
        </template>

        <!-- === ppt / exercise / lecture === -->
        <template v-if="activeResourceType && activeResourceType !== 'mindmap' && activeResourceType !== 'reading'">
          <div class="form-grid-2col" v-if="activeResourceType === 'exercise'">
            <div class="form-group">
              <label>选择搜索AI模型</label>
              <select v-model="form.readingSearchModel" class="form-select">
                <option value="xfsearch">讯飞ONE SEARCH</option>
              </select>
            </div>
            <div class="form-group"></div>
          </div>
          <div class="form-grid-2col">
            <div class="form-group">
              <label>选择AI模型</label>
              <select v-model="form.model" class="form-select">
                <option value="deepseek">DeepSeek V4 Pro</option>
                <option value="spark">讯飞星火 Spark X2</option>
              </select>
            </div>
            <div class="form-group">
              <label>专业</label>
              <input v-model="form.major" placeholder="例如：软件工程" />
            </div>
          </div>
          <div class="form-grid-2col" v-if="activeResourceType === 'lecture' || activeResourceType === 'ppt'">
            <div class="form-group">
              <label>选择图片生成模型</label>
              <select v-model="form.imageModel" class="form-select">
                <option value="seedream">豆包 Seedream 4.0</option>
              </select>
            </div>
            <div class="form-group"></div>
          </div>
        </template>

        <div class="form-group">
          <label>课程内容 <span class="required">*</span></label>
          <input v-model="form.courseName" placeholder="例如：C语言程序设计" />
        </div>

        <div class="form-grid-2col">
          <div class="form-group">
            <label>知识短板</label>
            <textarea v-model="form.knowledgeGaps" placeholder="例如：指针概念模糊、动态内存分配不熟练" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>学习需求</label>
            <textarea v-model="form.learningNeeds" placeholder="例如：系统掌握指针与数组、函数的关系，能够独立完成链表操作" rows="3"></textarea>
          </div>
        </div>

        <button
          class="btn btn-primary btn-lg generate-btn"
          :disabled="isGenerating"
          @click="generateSingle(activeResourceType)"
        >
          <i class="ri-magic-line"></i>
          生成{{ typeLabelMap[activeResourceType] || '' }}
        </button>
      </div>
    </div>

    <div v-if="activeResourceType === 'video'" class="card">
      <div class="card-header">
        <h3 class="card-title">教学视频生成</h3>
      </div>
      <div class="tab-content">
        <div id="cozeapp-page" class="coze-video-container"></div>
      </div>
    </div>

    <div v-if="generatingAgent" class="card">
      <div class="card-header">
        <h3 class="card-title">多智能体协同进度</h3>
      </div>
      <div class="agents-list">
        <div
          v-for="ag in agentWorkers"
          :key="ag.id"
          class="agent-item"
        >
          <div class="agent-icon" :class="ag.status">
            <i :class="ag.icon"></i>
          </div>
          <span class="agent-name">{{ ag.agent }}</span>
          <span class="agent-status-text">{{ ag.message }}</span>
        </div>
        <div v-if="streamSteps.length > 0" class="steps-list">
          <div
            v-for="(s, idx) in streamSteps"
            :key="idx"
            class="step-row"
            :class="{ 'step-active': idx === streamSteps.length - 1 && s.status === 'active', 'step-done': s.status === 'done', 'step-error': s.status === 'error' }"
          >
            <i :class="s.icon || 'ri-check-line'" class="step-icon"></i>
            <span class="step-text">{{ s.message }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showResult" class="card">
      <div class="card-header">
        <h3 class="card-title">生成结果</h3>
      </div>
      <div class="tab-content">
        <div v-if="resultType === 'mindmap' && resultImageUrl" class="mindmap-result">
          <div class="mindmap-actions">
            <a :href="resultImageUrl" :download="resultDownloadName" class="btn btn-primary btn-sm">
              <i class="ri-download-2-line"></i> 下载思维导图PNG
            </a>
          </div>
          <div class="mindmap-image-wrap">
            <img :src="resultImageUrl" :alt="form.courseName + ' 思维导图'" class="mindmap-img" />
          </div>
        </div>

        <div v-else-if="resultType !== 'mindmap'" class="mindmap-result">
          <div class="ppt-result-success" v-if="pptDownloaded">
            <i class="ri-check-double-line ppt-check-icon"></i>
            <p class="ppt-result-text">{{ typeLabelMap[resultType] || '文件' }} 已生成并自动下载！</p>
            <button class="btn btn-primary btn-sm" @click="reDownloadPpt">
              <i class="ri-download-2-line"></i> 重新下载
            </button>
          </div>
          <p class="result-placeholder" v-else>{{ typeLabelMap[resultType] || '文档' }}正在生成中，请稍候...</p>
        </div>

        <div v-else class="mindmap-empty">
          点击「生成」按钮开始生成
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">历史生成记录</h3>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>课程名称</th>
              <th>资源类型</th>
              <th>生成时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="historyRecords.length === 0">
              <td colspan="4" class="empty-row">暂无生成记录</td>
            </tr>
            <tr v-for="r in historyRecords" :key="r.createTime">
              <td>{{ r.courseName }}</td>
              <td>{{ r.resourceTypes }}</td>
              <td>{{ r.createTime }}</td>
              <td>
                <a v-if="r.downloadUrl" :href="r.downloadUrl" :download="r.fileName" class="btn btn-primary btn-sm">
                  <i class="ri-download-2-line"></i> 下载
                </a>
                <span v-else class="tag-pending">待生成</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''

const form = reactive({
  model: 'deepseek',
  searchModel: 'deepseek',
  imageModel: 'seedream',
  readingSearchModel: 'xfsearch',
  major: '',
  courseName: '',
  knowledgeGaps: '',
  learningNeeds: ''
})

const resourceTypes = [
  { label: '思维导图', value: 'mindmap', icon: 'ri-mind-map', desc: '生成知识点结构思维导图PNG' },
  { label: 'PPT课件', value: 'ppt', icon: 'ri-slideshow-3-line', desc: '生成教学PPT课件文稿' },
  { label: '练习题目', value: 'exercise', icon: 'ri-edit-2-line', desc: 'AI出题，巩固知识点' },
  { label: '拓展阅读', value: 'reading', icon: 'ri-book-open-line', desc: '推荐相关拓展阅读材料' },
  { label: '知识讲解', value: 'lecture', icon: 'ri-file-text-line', desc: '生成详细知识讲解文档' },
  { label: '教学视频', value: 'video', icon: 'ri-video-line', desc: 'AI智能生成教学讲解视频' }
]

const typeLabelMap = {
  mindmap: '思维导图',
  ppt: 'PPT课件',
  exercise: '练习题',
  reading: '拓展阅读',
  lecture: '知识讲解',
  video: '教学视频'
}

const activeResourceType = ref('')
const isGenerating = ref(false)
const showResult = ref(false)
const resultType = ref('')
const resultImageUrl = ref('')
const resultDownloadName = ref('')
const pptDownloaded = ref(false)
const pptBlobUrl = ref('')

const generatingAgent = ref(null)
const streamSteps = ref([])
const agentWorkers = ref([])
const historyRecords = ref([])
const historyBlobs = reactive({})

onMounted(() => loadHistory())

async function loadHistory() {
  try {
    const res = await fetch(`${API_BASE}/resource/history`, { headers: { 'Authorization': token() } })
    const data = await res.json()
    if (data.code === 200 && data.data) {
      historyRecords.value = data.data.map(r => {
        const ct = r.createdAt ? new Date(r.createdAt).toLocaleString('zh-CN') : ''
        return {
          courseName: r.courseName,
          knowledgePoint: r.knowledgePoint,
          resourceTypes: typeLabelMap[r.resourceType] || r.resourceType,
          createTime: ct,
          imageUrl: r.imageUrl,
          fileName: r.fileName,
          downloadUrl: r.imageUrl || historyBlobs[r.fileName] || ''
        }
      })
    }
  } catch (e) { console.error('加载历史记录失败', e) }
}

async function generateSingle(type) {
  if (!form.courseName.trim()) return alert('请填写课程内容')

  isGenerating.value = true
  showResult.value = false
  resultType.value = type
  pptDownloaded.value = false

  const modelLabel = form.model === 'spark' ? '讯飞星火' : 'DeepSeek'

  if (type === 'mindmap') {
    const searchLabel = form.searchModel === 'xfsearch' ? '讯飞ONE SEARCH' : 'DeepSeek'
    generatingAgent.value = { name: '思维导图师', icon: 'ri-mind-map', status: 'loading', statusText: `正在用${searchLabel}搜索资料，用${modelLabel}生成思维导图...` }
    try {
      const res = await fetch(`${API_BASE}/resource/mindmap`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': token() },
        body: JSON.stringify({
          model: form.model,
          searchModel: form.searchModel,
          major: form.major,
          courseName: form.courseName,
          knowledgeGaps: form.knowledgeGaps,
          learningNeeds: form.learningNeeds
        })
      })
      const data = await res.json()
      if (data.code === 200) {
        generatingAgent.value = { name: '思维导图师', icon: 'ri-mind-map', status: 'success', statusText: '生成完成' }
        resultImageUrl.value = data.data.imageUrl
        const now = new Date().toLocaleString('zh-CN')
        resultDownloadName.value = `${form.courseName}_思维导图_${now.replace(/[\/:]/g, '-')}.png`
        showResult.value = true
        loadHistory()
        isGenerating.value = false
        generatingAgent.value = null
      } else {
        generatingAgent.value = { name: '思维导图师', icon: 'ri-mind-map', status: 'error', statusText: data.message || '生成失败' }
        isGenerating.value = false
      }
    } catch (e) {
      generatingAgent.value = { name: '思维导图师', icon: 'ri-mind-map', status: 'error', statusText: '网络错误' }
      isGenerating.value = false
    }
  } else if (type === 'ppt') {
    await streamGenerate(type)
  } else {
    await streamGenerate(type)
  }
}

async function streamGenerate(type) {
  const label = typeLabelMap[type] || type

  generatingAgent.value = { status: 'loading' }
  streamSteps.value = []
  agentWorkers.value = []
  showResult.value = true

  try {
    const res = await fetch(`${API_BASE}/agent/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify({
        model: form.model,
        searchModel: type === 'reading' ? form.readingSearchModel : form.searchModel,
        imageModel: form.imageModel,
        major: form.major,
        courseName: form.courseName,
        knowledgeGaps: form.knowledgeGaps,
        learningNeeds: form.learningNeeds,
        resourceType: type
      })
    })

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}))
      throw new Error(errData.detail || errData.message || `${label}生成失败`)
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:done')) {
          const idx = lines.indexOf(line) + 1
          if (idx < lines.length && lines[idx].startsWith('data:')) continue
        }
        if (!line.startsWith('data:')) continue
        const rawData = line.substring(5).trim()
        if (!rawData) continue

        try {
          const evt = JSON.parse(rawData)

          // ─── Agent 步骤事件 ───
          if (evt.type === 'agent_step') {
            const existing = agentWorkers.value.find(a => a.id === evt.stepId)
            if (existing) {
              existing.status = evt.status === 'completed' ? 'success'
                : evt.status === 'failed' ? 'error'
                : evt.status === 'running' ? 'loading' : 'pending'
              existing.message = evt.description
            } else {
              agentWorkers.value.push({
                id: evt.stepId,
                agent: evt.agent,
                message: evt.description,
                icon: evt.icon || 'ri-loader-4-line',
                status: evt.status === 'running' ? 'loading'
                  : evt.status === 'completed' ? 'success'
                  : evt.status === 'failed' ? 'error' : 'pending'
              })
            }
            const stepEntry = streamSteps.value.find(s => s.stepId === evt.stepId)
            if (!stepEntry) {
              streamSteps.value.push({
                stepId: evt.stepId,
                icon: evt.icon || 'ri-loader-4-line',
                message: evt.agent + ': ' + evt.description,
                status: evt.status === 'running' ? 'active'
                  : evt.status === 'completed' ? 'done'
                  : evt.status === 'failed' ? 'error' : 'done'
              })
            } else {
              stepEntry.status = evt.status === 'running' ? 'active'
                : evt.status === 'failed' ? 'error' : 'done'
            }
          }

          // ─── Python 进度事件 ───
          if (evt.type === 'python_step') {
            streamSteps.value.push({
              stepId: 'py_' + (evt.step || ''),
              icon: evt.icon || 'ri-loader-4-line',
              message: evt.message || '',
              status: evt.step === 'done' ? 'done' : 'active'
            })
            generatingAgent.value = { ...generatingAgent.value, statusText: evt.message }
          }

          // ─── 最终完成事件 ───
          if (evt.type === 'done') {
            markStepsDone()
            if (evt.success) {
              generatingAgent.value = { status: 'success', statusText: '生成完成' }
              streamSteps.value.push({
                icon: 'ri-check-double-line',
                message: '多智能体协同完成，文件已下载',
                status: 'done'
              })
            } else {
              generatingAgent.value = { status: 'error', statusText: evt.error || '生成失败' }
              streamSteps.value.push({
                icon: 'ri-error-warning-line',
                message: evt.error || '多智能体协作失败',
                status: 'error'
              })
            }

            if (evt.base64) {
              const byteChars = atob(evt.base64)
              const byteNums = new Array(byteChars.length)
              for (let i = 0; i < byteChars.length; i++) byteNums[i] = byteChars.charCodeAt(i)
              const blob = new Blob([new Uint8Array(byteNums)], { type: evt.mimeType })
              pptBlobUrl.value = URL.createObjectURL(blob)
              pptDownloaded.value = true

              const a = document.createElement('a')
              a.href = pptBlobUrl.value
              a.download = evt.filename || `${form.courseName}_资源.${(evt.mimeType || '').includes('word') ? 'docx' : 'pptx'}`
              document.body.appendChild(a)
              a.click()
              document.body.removeChild(a)
            }

            isGenerating.value = false

            try {
              await fetch(`${API_BASE}/resource/record`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Authorization': token() },
                body: JSON.stringify({
                  courseName: form.courseName,
                  knowledgePoint: form.courseName,
                  resourceType: type,
                  fileName: evt.filename || '',
                  imageUrl: evt.downloadUrl || ''
                })
              })
              loadHistory()
            } catch (e) { console.error('保存记录失败', e) }
            return
          }
        } catch (e) { /* skip */ }
      }
    }
    throw new Error('连接意外中断')
  } catch (e) {
    generatingAgent.value = { status: 'error', statusText: e.message || '网络错误' }
    isGenerating.value = false
  }
}

function markStepsDone() {
  for (const s of streamSteps.value) { if (s.status === 'active') s.status = 'done' }
}

function reDownloadPpt() {
  if (!pptBlobUrl.value) return
  const a = document.createElement('a')
  a.href = pptBlobUrl.value
  a.download = `${form.courseName}_课件.pptx`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

let cozeSdkInstance = null
const cozeConfig = ref(null)

async function initCozeSdk() {
  if (cozeSdkInstance) destroyCozeSdk()

  if (!cozeConfig.value) {
    try {
      const res = await fetch(`${API_BASE}/coze/config`)
      const data = await res.json()
      if (data.code === 200) {
        cozeConfig.value = data.data
      }
    } catch (e) {
      console.error('获取Coze配置失败', e)
      return
    }
  }

  const container = document.getElementById('cozeapp-page')
  if (!container) {
    setTimeout(() => initCozeSdk(), 200)
    return
  }

  container.innerHTML = ''

  if (window.CozeWebSDK) {
    createCozeInstance()
  } else {
    const script = document.createElement('script')
    script.src = 'https://lf-cdn.coze.cn/obj/unpkg/flow-platform/builder-web-sdk/0.1.1-beta.1/dist/umd/index.js'
    script.onload = () => createCozeInstance()
    script.onerror = () => {
      generatingAgent.value = {
        name: '教学视频生成器',
        icon: 'ri-video-line',
        status: 'error',
        statusText: 'Coze SDK加载失败'
      }
    }
    document.head.appendChild(script)
  }
}

function createCozeInstance() {
  cozeSdkInstance = new window.CozeWebSDK.AppWebSDK({
    token: cozeConfig.value.satToken,
    appId: cozeConfig.value.appId,
    container: '#cozeapp-page',
    userInfo: {
      id: 'user',
      url: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/coze-logo.png',
      nickname: 'User'
    },
    ui: {
      className: ''
    }
  })
}

function destroyCozeSdk() {
  const container = document.getElementById('cozeapp-page')
  if (container) {
    container.innerHTML = ''
  }
  cozeSdkInstance = null
}

function selectType(type) {
  if (type === 'video') {
    router.push('/app/video-generate')
    return
  }
  if (activeResourceType.value === type) {
    activeResourceType.value = ''
    return
  }
  if (activeResourceType.value === 'video') {
    destroyCozeSdk()
  }
  activeResourceType.value = type
  pptDownloaded.value = false
}

onBeforeUnmount(() => {
  destroyCozeSdk()
})
</script>

<style scoped>
.resource-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.card {
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--color-border-light);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.card-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-card { padding: 24px; }

.resource-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 12px; margin-bottom: 0; }

.resource-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  background: var(--color-bg-page);
}
.resource-card:hover { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(22,93,255,0.08); }
.resource-card.active { border-color: var(--color-primary); background: var(--color-primary-bg); }

.rc-icon {
  width: 44px; height: 44px;
  border-radius: var(--radius-md);
  background: var(--color-primary-bg);
  color: var(--color-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.resource-card.active .rc-icon { background: var(--color-primary); color: #fff; }

.rc-info { flex: 1; min-width: 0; }
.rc-name { font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: 2px; }
.rc-desc { font-size: var(--font-size-xs); color: var(--color-text-tertiary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.generate-form {
  margin-top: 20px;
  padding: 20px;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.form-section-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.form-grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-grid-3col { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px; }

.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.form-grid-2col .form-group { margin-bottom: 0; }
.form-grid-3col .form-group { margin-bottom: 0; }

.form-group label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.required { color: var(--color-danger, #ef4444); }

.form-group input,
.form-group textarea,
.form-select {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  font-family: inherit;
  width: 100%;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus,
.form-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(22,93,255,0.1);
  outline: none;
}

.form-group textarea { resize: vertical; }

.form-select { cursor: pointer; }

.generate-btn {
  margin-top: 8px;
  width: 100%;
  padding: 12px 24px;
  font-size: var(--font-size-md);
}

.agents-list { padding: 20px; }
.agent-item { display: flex; align-items: center; gap: 12px; }
.agent-icon { width: 36px; height: 36px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; background: var(--color-bg-page); color: var(--color-text-tertiary); font-size: 18px; }
.agent-icon.loading { background: var(--color-primary-bg); color: var(--color-primary); animation: spin 1s linear infinite; }
.agent-icon.success { background: var(--color-success-bg); color: var(--color-success); }
.agent-icon.error { background: var(--color-danger-bg); color: var(--color-danger); }
.agent-name { font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); }
.agent-status-text { font-size: var(--font-size-xs); color: var(--color-text-tertiary); }

.steps-list {
  margin-top: 16px;
  padding: 12px 16px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
}
.step-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border-light);
  transition: opacity 0.3s;
}
.step-row:last-child { border-bottom: none; }
.step-row.step-done { opacity: 0.5; }
.step-row.step-active { opacity: 1; }
.step-row.step-error { opacity: 1; }
.step-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}
.step-done .step-icon { color: var(--color-success, #22c55e); }
.step-active .step-icon { color: var(--color-primary); animation: spin 1s linear infinite; }
.step-error .step-icon { color: var(--color-danger, #ef4444); }
.step-text { font-size: var(--font-size-xs); color: var(--color-text-secondary); line-height: 1.5; }
.step-active .step-text { color: var(--color-text-primary); font-weight: var(--font-weight-medium); }

@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.tab-content { padding: 20px; }

.mindmap-result { display: flex; flex-direction: column; gap: 12px; }
.mindmap-actions { display: flex; gap: 8px; }
.mindmap-image-wrap { border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden; background: #fafafa; }
.mindmap-img { width: 100%; display: block; }
.mindmap-empty { text-align: center; padding: 48px; color: var(--color-text-placeholder); font-size: var(--font-size-sm); }
.result-placeholder { font-size: var(--font-size-sm); color: var(--color-text-tertiary); padding: 24px 0; }

.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { padding: 12px 20px; text-align: left; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-tertiary); background: var(--color-bg-page); border-bottom: 1px solid var(--color-border-light); }
.data-table td { padding: 12px 20px; font-size: var(--font-size-sm); color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border-light); }
.data-table tbody tr:hover { background: var(--color-bg-hover); }
.empty-row { text-align: center; color: var(--color-text-placeholder) !important; }
.tag-pending { font-size: var(--font-size-xs); color: var(--color-warning); background: var(--color-warning-bg); padding: 3px 10px; border-radius: 12px; }
.tag-success { font-size: var(--font-size-xs); color: var(--color-success, #22c55e); background: var(--color-success-bg, #f0fdf4); padding: 3px 10px; border-radius: 12px; }

.ppt-result-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 36px 24px;
  background: var(--color-success-bg, #f0fdf4);
  border: 1px solid var(--color-success, #22c55e);
  border-radius: var(--radius-md);
}
.ppt-check-icon {
  font-size: 48px;
  color: var(--color-success, #22c55e);
}
.ppt-result-text {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

@media (max-width: 768px) {
  .form-grid-2col { grid-template-columns: 1fr; }
  .form-grid-3col { grid-template-columns: 1fr; }
}

.video-result-area {
  width: 100%;
}

.coze-video-container {
  width: 100%;
  height: 600px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: #fff;
}

.generated-content { padding: 4px 0; }

.gen-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.gen-summary {
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  line-height: 1.7;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-light);
  margin-bottom: 16px;
}

.ppt-download-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  background: #e8f5e9;
  border: 1px solid #4caf50;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: #2e7d32;
}

.ppt-download-bar i {
  font-size: 22px;
}

.ppt-download-bar span {
  flex: 1;
}

.gen-section { margin-bottom: 20px; }

.gen-section-heading {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0 0 8px 0;
  padding-left: 12px;
  border-left: 3px solid var(--color-primary);
}

.gen-section-body {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 12px 16px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
}

.gen-exercises { margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--color-border-light); }

.gen-exercise-item {
  padding: 12px 16px;
  margin-bottom: 10px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
}

.gen-ex-q {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.gen-ex-a {
  font-size: var(--font-size-sm);
  color: var(--color-success);
  margin-bottom: 4px;
}

.gen-ex-diff {
  font-size: var(--font-size-xs);
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
  text-transform: capitalize;
}

.gen-ex-diff.diff-easy { background: #e8f5e9; color: #2e7d32; }
.gen-ex-diff.diff-medium { background: #fff3e0; color: #e65100; }
.gen-ex-diff.diff-hard { background: #fce4ec; color: #c62828; }

:deep(#cozeapp-page) {
  width: 100%;
  height: 100%;
}
</style>
