<template>
  <div class="profile-page">
    <div class="page-header">
      <h2 class="page-title">学习画像</h2>
      <button class="btn btn-primary" @click="activeTab = 'build'">
        <i class="ri-chat-smile-2-line"></i> {{ profileData ? '更新画像' : '构建画像' }}
      </button>
    </div>

    <div class="tab-bar">
      <button class="tab-btn" :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">
        <i class="ri-file-chart-line"></i> 画像概览
      </button>
      <button class="tab-btn" :class="{ active: activeTab === 'build' }" @click="activeTab = 'build'">
        <i class="ri-chat-3-line"></i> AI 对话构建
      </button>
    </div>

    <div v-if="activeTab === 'overview'">
      <div v-if="!profileData || profileData.completedDimensions === 0" class="card empty-card">
        <div class="empty-content">
          <i class="ri-user-search-line empty-icon"></i>
          <h3>还没有学习画像</h3>
          <p>通过DeepSeek AI对话构建你的专属学习画像，了解自己的知识掌握、认知偏好、学习节奏等9个维度</p>
          <button class="btn btn-primary" @click="activeTab = 'build'">
            <i class="ri-chat-smile-2-line"></i> 开始构建画像
          </button>
        </div>
      </div>

      <div v-else class="profile-overview">
        <div class="card summary-card">
          <div class="summary-top">
            <div class="summary-badge">
              <span class="badge-pill complete">{{ profileData.status === 'COMPLETED' ? '已完成' : '构建中' }}</span>
              <span class="dimension-count">{{ profileData.completedDimensions }}/9 维度</span>
            </div>
          </div>
          <div class="summary-bar-wrap">
            <div class="summary-bar">
              <div class="summary-fill" :style="{ width: (profileData.completedDimensions / 9 * 100) + '%' }"></div>
            </div>
          </div>
          <div class="summary-grid">
            <div class="summary-item" v-if="profileData.overallLevel">
              <span class="s-label">整体水平</span>
              <span class="s-value level-tag" :class="levelTag">{{ profileData.overallLevel }}</span>
            </div>
            <div class="summary-item" v-if="parsedLearningGoal">
              <span class="s-label">学习目标</span>
              <span class="s-value">{{ parsedLearningGoal.purpose || '-' }} | {{ parsedLearningGoal.weeklyHours || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="dimensions-grid">
          <div v-if="parsedKnowledge.length" class="card dim-card">
            <div class="dim-header">
              <i class="ri-book-open-line dim-icon"></i>
              <h4 class="dim-title">知识点掌握</h4>
            </div>
            <div class="dim-tags">
              <span v-for="k in parsedKnowledge" :key="k.name" class="tag-small" :class="knowledgeTag(k.status)">
                {{ k.name }} <span class="tag-score">{{ k.score ? k.score + '分' : '' }}</span>
              </span>
            </div>
          </div>

          <div v-if="parsedCognitive" class="card dim-card">
            <div class="dim-header"><i class="ri-brain-line dim-icon"></i><h4 class="dim-title">认知偏好</h4></div>
            <p class="dim-text">{{ formatList(parsedCognitive.mediaPreference) }} | {{ parsedCognitive.understanding || '-' }}</p>
          </div>

          <div v-if="parsedRhythm" class="card dim-card">
            <div class="dim-header"><i class="ri-time-line dim-icon"></i><h4 class="dim-title">学习节奏</h4></div>
            <p class="dim-text">{{ parsedRhythm.studySlot || '-' }} | 专注{{ parsedRhythm.focusDuration || '-' }} | {{ parsedRhythm.habit || '-' }}式</p>
          </div>

          <div v-if="parsedLearningGoal" class="card dim-card">
            <div class="dim-header"><i class="ri-flag-line dim-icon"></i><h4 class="dim-title">学习目标</h4></div>
            <p class="dim-text">目标: {{ parsedLearningGoal.purpose || '-' }} | 每周{{ parsedLearningGoal.weeklyHours || '-' }}</p>
          </div>

          <div v-if="parsedErrors.length" class="card dim-card">
            <div class="dim-header"><i class="ri-error-warning-line dim-icon"></i><h4 class="dim-title">易错类型</h4></div>
            <div class="dim-tags">
              <span v-for="e in parsedErrors" :key="e.type" class="tag-small tag-error">{{ e.type }}({{ e.frequency }}) - {{ e.cause }}</span>
            </div>
          </div>

          <div v-if="parsedResource" class="card dim-card">
            <div class="dim-header"><i class="ri-folder-line dim-icon"></i><h4 class="dim-title">资源偏好</h4></div>
            <p class="dim-text">{{ parsedResource.difficulty || '-' }} | {{ parsedResource.contentLength || '-' }} | 拓展{{ parsedResource.acceptExtension ? '接受' : '拒绝' }}</p>
          </div>

          <div v-if="parsedFeedback" class="card dim-card">
            <div class="dim-header"><i class="ri-chat-1-line dim-icon"></i><h4 class="dim-title">反馈偏好</h4></div>
            <p class="dim-text">{{ parsedFeedback.answerStyle || '-' }} | {{ parsedFeedback.feedbackFrequency || '-' }}</p>
          </div>

          <div v-if="profileData.diagnosisReport" class="card dim-card dim-diagnosis">
            <div class="dim-header"><i class="ri-file-text-line dim-icon"></i><h4 class="dim-title">诊断报告</h4></div>
            <p class="dim-text dim-text-lg">{{ profileData.diagnosisReport }}</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="activeTab === 'build'" class="ai-chat">
      <div class="card chat-card">
        <div class="card-header">
          <div class="chat-header-left">
            <div class="ai-badge">
              <i class="ri-robot-2-line"></i>
              <span>DeepSeek AI</span>
            </div>
            <span class="ai-status" :class="{ streaming: isStreaming }">
              {{ isStreaming ? '正在回复...' : '在线' }}
            </span>
          </div>
          <button class="btn btn-outline btn-sm" @click="resetChat" :disabled="isStreaming">
            <i class="ri-refresh-line"></i> 重新开始
          </button>
        </div>

        <div class="chat-messages" ref="chatListRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <div class="welcome-icon"><i class="ri-robot-2-line"></i></div>
            <h3>DeepSeek AI 画像助手</h3>
            <p>我会通过对话了解你的学习情况，帮你构建专属学习画像。在下方输入框开始聊天吧！</p>
            <div class="quick-prompts">
              <button v-for="q in quickPrompts" :key="q" class="quick-btn" @click="sendMessage(q)">{{ q }}</button>
            </div>
          </div>

          <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="msg.role">
            <div class="msg-avatar">
              <i v-if="msg.role === 'user'" class="ri-user-3-fill"></i>
              <i v-else class="ri-robot-2-fill"></i>
            </div>
            <div class="msg-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'ai-bubble'">
              <div class="msg-content">{{ stripProfileJson(msg.content) }}</div>
              <div v-if="i === messages.length - 1 && isStreaming && msg.role === 'assistant'" class="typing-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-area">
          <textarea
            v-model="chatInput"
            placeholder="和DeepSeek聊聊你的学习情况..."
            rows="2"
            @keydown.enter.exact="sendMessage()"
            :disabled="isStreaming"
          ></textarea>
          <button class="send-btn" @click="sendMessage()" :disabled="!chatInput.trim() || isStreaming">
            <i class="ri-send-plane-fill"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'

const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''
const activeTab = ref('overview')
const chatInput = ref('')
const isStreaming = ref(false)
const messages = ref([])
const profileData = ref(null)
const chatListRef = ref(null)

const quickPrompts = [
  '帮我构建学习画像',
  '我的学习目标是准备考研',
  '我在学Java和Spring，基础还行但高级特性不太熟',
  '我平时喜欢晚上学习，一次能专注1小时左右'
]

const parsedKnowledge = computed(() => parseJson(profileData.value?.knowledgeMastery))
const parsedCognitive = computed(() => parseJson(profileData.value?.cognitiveStyle))
const parsedRhythm = computed(() => parseJson(profileData.value?.studyRhythm))
const parsedLearningGoal = computed(() => parseJson(profileData.value?.learningGoal))
const parsedErrors = computed(() => parseJson(profileData.value?.errorPattern))
const parsedResource = computed(() => parseJson(profileData.value?.resourcePreference))
const parsedFeedback = computed(() => parseJson(profileData.value?.feedbackPreference))

const levelTag = computed(() => {
  const l = profileData.value?.overallLevel
  if (!l) return ''
  if (l.includes('优秀') || l.includes('良好') || l.includes('高级')) return 'level-high'
  if (l.includes('中等') || l.includes('中级')) return 'level-mid'
  return 'level-low'
})

function parseJson(raw) {
  if (!raw) return null
  try { return typeof raw === 'string' ? JSON.parse(raw) : raw }
  catch { return raw }
}

function knowledgeTag(status) {
  if (!status) return ''
  if (status.includes('良好') || status.includes('熟悉') || status.includes('优秀')) return 'tag-good'
  if (status.includes('中等') || status.includes('了解')) return 'tag-mid'
  return 'tag-bad'
}

function formatList(arr) {
  if (!arr || !Array.isArray(arr)) return '-'
  return arr.join('、')
}

const stripProfileJson = (content) => {
  if (!content) return content
  return content.replace(/<PROFILE_JSON>[\s\S]*?<\/PROFILE_JSON>/g, '')
}

const extractProfileJson = (content) => {
  if (!content) return null
  const match = content.match(/<PROFILE_JSON>([\s\S]*?)<\/PROFILE_JSON>/)
  if (!match) return null
  try { return JSON.parse(match[1]) }
  catch { return null }
}

onMounted(() => { loadProfile() })

function loadProfile() {
  fetch(`${API_BASE}/profile`, { headers: { 'Authorization': token() } })
    .then(r => r.json())
    .then(data => {
      if (data.code === 200 && data.data) profileData.value = data.data
    })
    .catch(() => {})
}

function resetChat() {
  messages.value = []
}

async function scrollToBottom() {
  await nextTick()
  if (chatListRef.value) {
    chatListRef.value.scrollTop = chatListRef.value.scrollHeight
  }
}

async function sendMessage(prefill) {
  const text = prefill || chatInput.value.trim()
  if (!text || isStreaming.value) return
  chatInput.value = ''

  messages.value.push({ role: 'user', content: text })
  const aiIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  isStreaming.value = true
  await scrollToBottom()

  try {
    const history = messages.value
      .filter(m => m.role !== 'assistant' || m.content)
      .slice(0, -1)
      .map(m => ({ role: m.role, content: stripProfileJson(m.content) }))

    const response = await fetch(`${API_BASE}/profile/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify({ prompt: text, history })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value, { stream: true })
      const lines = text.split(/\r?\n/)

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const event = JSON.parse(line.substring(6))
            if (event.type === 'chunk') {
              fullContent += event.content
              messages.value[aiIndex].content = fullContent
            } else if (event.type === 'done') {
              fullContent = event.fullContent || fullContent
              messages.value[aiIndex].content = fullContent

              const profileJson = extractProfileJson(fullContent)
              if (profileJson?.profile) {
                await saveProfileData(profileJson.profile)
              }
            } else if (event.type === 'error') {
              messages.value[aiIndex].content = '抱歉，AI服务暂时不可用: ' + (event.content || '未知错误')
            }
          } catch (e) { /* skip parse error */ }
        }
      }
    }

    if (!messages.value[aiIndex].content) {
      messages.value[aiIndex].content = fullContent || '(AI未返回内容)'
    }
  } catch (e) {
    messages.value[aiIndex].content = '连接失败，请检查后端服务是否启动。'
  } finally {
    isStreaming.value = false
    await scrollToBottom()
  }
}

async function saveProfileData(profile) {
  try {
    const res = await fetch(`${API_BASE}/profile/save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify(profile)
    })
    const data = await res.json()
    if (data.code === 200) {
      profileData.value = data.data
      messages.value.push({
        role: 'assistant',
        content: '✅ 你的学习画像已构建完成！切换到「画像概览」查看详情。'
      })
      await scrollToBottom()
    }
  } catch (e) {
    console.error('保存画像失败', e)
  }
}
</script>

<style scoped>
.profile-page { display: flex; flex-direction: column; gap: 20px; }

.page-header { display: flex; justify-content: space-between; align-items: center; }

.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }

.tab-bar { display: flex; gap: 4px; background: var(--color-bg-card); border-radius: var(--radius-md); padding: 3px; border: 1px solid var(--color-border-light); }

.tab-btn { flex: 1; padding: 10px 0; border: none; background: none; border-radius: var(--radius-sm); font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-tertiary); cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; transition: all 0.2s; }

.tab-btn.active { background: var(--color-primary); color: #fff; box-shadow: var(--shadow-sm); }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }

.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid var(--color-border-light); }

.card-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.empty-card { padding: 60px 40px; text-align: center; }

.empty-icon { font-size: 48px; color: var(--color-text-placeholder); margin-bottom: 16px; display: block; }

.empty-content h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); margin-bottom: 8px; }

.empty-content p { font-size: var(--font-size-sm); color: var(--color-text-tertiary); margin-bottom: 24px; max-width: 480px; margin-inline: auto; line-height: 1.6; }

.summary-card { padding: 24px; }

.summary-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }

.summary-badge { display: flex; align-items: center; gap: 10px; }

.badge-pill { padding: 4px 14px; border-radius: 20px; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); }

.badge-pill.complete { background: var(--color-success-bg); color: var(--color-success); }

.dimension-count { font-size: var(--font-size-sm); color: var(--color-text-tertiary); }

.summary-bar-wrap { margin-bottom: 16px; }

.summary-bar { height: 8px; background: var(--color-border-light); border-radius: 4px; overflow: hidden; }

.summary-fill { height: 100%; background: var(--color-primary-gradient); border-radius: 4px; transition: width 0.6s ease; }

.summary-grid { display: flex; gap: 24px; }

.summary-item { display: flex; flex-direction: column; gap: 4px; }

.s-label { font-size: var(--font-size-xs); color: var(--color-text-tertiary); text-transform: uppercase; letter-spacing: 0.5px; }

.s-value { font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.level-tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: var(--font-size-xs); }

.level-high { background: var(--color-success-bg); color: var(--color-success); }
.level-mid { background: var(--color-warning-bg); color: var(--color-warning); }
.level-low { background: var(--color-danger-bg); color: var(--color-danger); }

.dimensions-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 16px; }

.dim-card { padding: 20px; }

.dim-diagnosis { grid-column: 1 / -1; }

.dim-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }

.dim-icon { font-size: 16px; color: var(--color-primary); }

.dim-title { font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.dim-text { font-size: var(--font-size-sm); color: var(--color-text-secondary); line-height: 1.6; }

.dim-text-lg { font-size: var(--font-size-base); }

.dim-tags { display: flex; flex-wrap: wrap; gap: 6px; }

.tag-small { padding: 3px 10px; border-radius: 12px; font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); }

.tag-good { background: var(--color-success-bg); color: var(--color-success); }
.tag-mid { background: var(--color-warning-bg); color: var(--color-warning); }
.tag-bad { background: var(--color-danger-bg); color: var(--color-danger); }
.tag-error { background: var(--color-danger-bg); color: var(--color-danger); }

.tag-score { opacity: 0.7; }

.ai-chat { height: calc(100vh - var(--topbar-height) - 140px); min-height: 500px; }

.chat-card { display: flex; flex-direction: column; height: 100%; }

.chat-header-left { display: flex; align-items: center; gap: 12px; }

.ai-badge { display: flex; align-items: center; gap: 6px; padding: 4px 12px; background: var(--color-primary-bg); border-radius: 20px; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-primary); }

.ai-status { font-size: var(--font-size-xs); color: var(--color-success); display: flex; align-items: center; gap: 4px; }

.ai-status::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--color-success); }

.ai-status.streaming::before { background: var(--color-warning); animation: dotPulse 1s infinite; }

@keyframes dotPulse { 0%,100%{opacity:1} 50%{opacity:0.3} }

.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; background: var(--color-bg-page); }

.chat-welcome { text-align: center; padding: 40px 20px; }

.welcome-icon { width: 60px; height: 60px; border-radius: 50%; background: var(--color-primary-bg); display: inline-flex; align-items: center; justify-content: center; font-size: 28px; color: var(--color-primary); margin-bottom: 16px; }

.welcome-icon i { font-size: 32px; }

.chat-welcome h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); margin-bottom: 6px; }

.chat-welcome p { font-size: var(--font-size-sm); color: var(--color-text-tertiary); margin-bottom: 20px; }

.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }

.quick-btn { padding: 8px 16px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-bg-card); font-size: var(--font-size-sm); color: var(--color-text-secondary); cursor: pointer; transition: all 0.2s; }

.quick-btn:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }

.chat-msg { display: flex; gap: 10px; }

.chat-msg.user { flex-direction: row-reverse; }

.msg-avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }

.chat-msg.assistant .msg-avatar { background: var(--color-primary-bg); color: var(--color-primary); }

.chat-msg.user .msg-avatar { background: var(--color-primary); color: #fff; }

.msg-bubble { max-width: 75%; padding: 12px 16px; border-radius: 16px; font-size: var(--font-size-sm); line-height: 1.7; white-space: pre-wrap; word-break: break-word; }

.user-bubble { background: var(--color-primary); color: #fff; border-radius: 16px 16px 4px 16px; }

.ai-bubble { background: var(--color-bg-card); color: var(--color-text-primary); border: 1px solid var(--color-border-light); border-radius: 16px 16px 16px 4px; }

.typing-dots { display: flex; gap: 4px; padding: 6px 0 2px; }

.typing-dots span { width: 5px; height: 5px; border-radius: 50%; background: var(--color-text-placeholder); animation: typingBounce 1.4s infinite ease-in-out; }

.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typingBounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-4px)} }

.chat-input-area { display: flex; gap: 10px; padding: 14px 16px; border-top: 1px solid var(--color-border-light); align-items: flex-end; }

.chat-input-area textarea { flex: 1; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-sm); font-family: inherit; resize: none; background: var(--color-bg-page); line-height: 1.5; }

.chat-input-area textarea:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(22,93,255,0.08); outline: none; }

.send-btn { width: 42px; height: 42px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: #fff; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; flex-shrink: 0; }

.send-btn:hover:not(:disabled) { background: var(--color-primary-light); transform: scale(1.05); }

.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
