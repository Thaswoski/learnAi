<template>
  <div class="tutor-page">
    <div class="tutor-header">
      <div class="header-left">
        <div class="ai-badge">
          <i class="ri-robot-2-line"></i>
          <span>{{ modelDisplayName }}</span>
        </div>
        <span class="ai-status" :class="{ streaming: isStreaming }">
          {{ isStreaming ? '正在回复...' : '在线' }}
        </span>
      </div>
      <div class="header-right">
        <button class="btn btn-outline btn-sm" @click="goBack" title="返回">
          <i class="ri-arrow-left-line"></i> 返回
        </button>
        <select class="model-select" v-model="activeModel" @change="switchModel" :disabled="isStreaming">
          <option value="qwen">千问 (qwen-plus)</option>
          <option value="spark">讯飞星火 (X2)</option>
        </select>
        <button class="btn btn-outline btn-sm" @click="toggleHistory" title="历史记录">
          <i class="ri-history-line"></i>
        </button>
        <button class="btn btn-outline btn-sm" @click="startNewSession" :disabled="isStreaming">
          <i class="ri-refresh-line"></i> 新对话
        </button>
      </div>
    </div>

    <div class="tutor-layout">
      <Transition name="slide">
        <div v-if="showHistory" class="history-panel">
          <div class="history-title">历史对话</div>
          <div v-if="sessions.length === 0" class="history-empty">暂无历史记录</div>
          <div
            v-for="sid in sessions"
            :key="sid"
            class="history-item"
            :class="{ active: sessionId === sid }"
            @click="loadSession(sid)"
          >
            <i class="ri-chat-3-line"></i>
            <span class="history-id">{{ formatSessionId(sid) }}</span>
            <button class="history-del" @click.stop="deleteSession(sid)" title="删除"><i class="ri-delete-bin-line"></i></button>
          </div>
        </div>
      </Transition>

      <div class="chat-card card">
        <div class="chat-messages" ref="chatRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <div class="welcome-icon"><i class="ri-robot-2-line"></i></div>
            <h3>智多星 · AI 智能辅导</h3>
            <p>我是你的专属学习导师，可以帮你解答课程疑问、讲解算法概念、辅导习题、分析题目截图。随时向我提问或上传图片！左上角切换模型，注：星火spark-x模型暂时不支持图片理解</p>
            <div class="quick-prompts">
              <button v-for="q in quickPrompts" :key="q" class="quick-btn" @click="send(q)">{{ q }}</button>
            </div>
          </div>

          <div v-for="(msg, i) in messages" :key="i" class="chat-msg" :class="msg.role">
            <div class="msg-avatar">
              <i v-if="msg.role === 'user'" class="ri-user-3-fill"></i>
              <i v-else class="ri-robot-2-fill"></i>
            </div>
            <div class="msg-bubble" :class="msg.role === 'user' ? 'user-bubble' : 'ai-bubble'">
              <div v-if="msg.imageUrl" class="msg-image">
                <img :src="getImageSrc(msg.imageUrl)" alt="上传的图片" />
              </div>
              <div class="msg-content" v-html="renderContent(msg.content)"></div>
              <div v-if="i === messages.length - 1 && isStreaming && msg.role === 'assistant'" class="typing-cursor">|</div>
            </div>
          </div>
        </div>

        <div class="selected-image" v-if="selectedImage">
          <img :src="selectedImage" alt="待上传" />
          <button class="img-remove" @click="selectedImage = null; selectedImageBase64 = null"><i class="ri-close-line"></i></button>
          <span class="img-label">图片已选择</span>
        </div>

        <div class="chat-input-area">
          <label v-if="supportsImage" class="upload-btn" title="上传图片">
            <i class="ri-image-add-line"></i>
            <input type="file" accept="image/*" hidden @change="handleImageSelect" />
          </label>
          <textarea
            v-model="chatInput"
            placeholder="输入你的问题，或上传图片让AI分析..."
            rows="2"
            @keydown.enter.exact="send()"
            :disabled="isStreaming"
            ref="inputRef"
          ></textarea>
          <button class="send-btn" @click="send()" :disabled="(!chatInput.trim() && !selectedImageBase64) || isStreaming">
            <i class="ri-send-plane-fill"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''
const chatInput = ref('')
const isStreaming = ref(false)
const messages = ref([])
const chatRef = ref(null)
const inputRef = ref(null)
const sessionId = ref(String(Date.now()))
const showHistory = ref(false)
const sessions = ref([])
const selectedImage = ref(null)
const selectedImageBase64 = ref(null)
const activeModel = ref('qwen')
const modelDisplayName = ref('千问 AI')
const supportsImage = ref(true)

function goBack() {
  router.push('/app/tutor')
}

const quickPrompts = [
  '解释一下什么是梯度下降算法？',
  '帮我分析这段代码的时间复杂度',
  'Java中HashMap和TreeMap有什么区别？',
  '如何提高学习效率？有什么方法推荐？',
  '请帮我讲解二叉树的遍历方式',
  '线性回归和逻辑回归的区别是什么？'
]

onMounted(() => { loadSessions(); fetchModelInfo(); initLive2D() })

function initLive2D() {
  const existing = document.getElementById('waifu')
  if (existing) {
    existing.style.display = ''
    const toggle = document.getElementById('waifu-toggle')
    if (toggle) toggle.style.display = ''
    return
  }
  if (document.getElementById('live2d-autoload')) return
  const script = document.createElement('script')
  script.id = 'live2d-autoload'
  script.src = '/live2d/autoload.js'
  document.head.appendChild(script)
}

onBeforeUnmount(() => {
  const toggle = document.getElementById('waifu-toggle')
  if (toggle) toggle.style.display = 'none'
  const waifu = document.getElementById('waifu')
  if (waifu) waifu.style.display = 'none'
})

async function fetchModelInfo() {
  try {
    const res = await fetch(`${API_BASE}/tutor/model`, { headers: { 'Authorization': token() } })
    const data = await res.json()
    if (data.code === 200) {
      activeModel.value = data.data.active
      modelDisplayName.value = data.data.name
      supportsImage.value = data.data.supportsImage
    }
  } catch (e) {}
}

async function switchModel() {
  try {
    const res = await fetch(`${API_BASE}/tutor/model`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify({ model: activeModel.value })
    })
    const data = await res.json()
    if (data.code === 200) {
      modelDisplayName.value = data.data.name
      supportsImage.value = data.data.supportsImage
      if (!supportsImage.value) {
        selectedImage.value = null
        selectedImageBase64.value = null
      }
    }
  } catch (e) {}
}

function renderContent(text) {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/# (.+)/g, '<h2>$1</h2>')
    .replace(/^- (.+)/gm, '<li>$1</li>')
    .replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
    .replace(/\n/g, '<br>')
  return html
}

async function scrollToBottom() {
  await nextTick()
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}

function getImageSrc(imageUrl) {
  if (!imageUrl) return ''
  if (imageUrl.startsWith('data:')) return imageUrl
  return imageUrl
}

function handleImageSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (ev) => {
    const dataUrl = ev.target.result
    selectedImage.value = dataUrl
    selectedImageBase64.value = dataUrl.split(',')[1]
  }
  reader.readAsDataURL(file)
}

async function loadSessions() {
  try {
    const res = await fetch(`${API_BASE}/tutor/sessions`, { headers: { 'Authorization': token() } })
    const data = await res.json()
    if (data.code === 200) sessions.value = data.data || []
  } catch (e) {}
}

async function loadSession(sid) {
  try {
    const res = await fetch(`${API_BASE}/tutor/messages/${sid}`, { headers: { 'Authorization': token() } })
    const data = await res.json()
    if (data.code === 200 && data.data.length) {
      messages.value = data.data.map(m => ({
        role: m.role,
        content: m.content,
        imageUrl: m.imageUrl || null
      }))
      sessionId.value = sid
    }
  } catch (e) {}
}

async function deleteSession(sid) {
  await fetch(`${API_BASE}/tutor/sessions/${sid}`, { method: 'DELETE', headers: { 'Authorization': token() } })
  if (sessionId.value === sid) messages.value = []
  loadSessions()
}

function startNewSession() {
  messages.value = []
  sessionId.value = String(Date.now())
  selectedImage.value = null
  selectedImageBase64.value = null
}

function toggleHistory() { showHistory.value = !showHistory.value; if (showHistory.value) loadSessions() }

function formatSessionId(sid) {
  try { return new Date(Number(sid)).toLocaleString('zh-CN') } catch { return sid }
}

async function send(prefill) {
  const text = prefill || chatInput.value.trim()
  if ((!text && !selectedImageBase64.value) || isStreaming.value) return
  chatInput.value = ''

  const img = supportsImage.value ? selectedImageBase64.value : null
  messages.value.push({ role: 'user', content: text, imageUrl: img ? selectedImage.value : null })
  selectedImage.value = null
  selectedImageBase64.value = null

  const aiIndex = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  isStreaming.value = true
  await scrollToBottom()

  try {
    const history = messages.value
      .filter(m => m.role !== 'assistant' || m.content)
      .slice(0, -1)
      .map(m => ({ role: m.role, content: m.content }))

    const response = await fetch(`${API_BASE}/tutor/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token() },
      body: JSON.stringify({ prompt: text, image: img, sessionId: sessionId.value, history })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const t = decoder.decode(value, { stream: true })
      const lines = t.split(/\r?\n/)
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
              if (event.sessionId) sessionId.value = event.sessionId
              loadSessions()
            } else if (event.type === 'error') {
              messages.value[aiIndex].content = '抱歉，AI服务暂时不可用: ' + (event.content || '未知错误')
            }
          } catch (e) {}
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
</script>

<style scoped>
.tutor-page { display: flex; flex-direction: column; height: calc(100vh - var(--topbar-height) - 56px); gap: 12px; }
.tutor-header { display: flex; justify-content: space-between; align-items: center; padding: 0 4px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-right { display: flex; gap: 8px; align-items: center; }

.model-select { padding: 4px 8px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-xs); background: var(--color-bg-card); color: var(--color-text-secondary); cursor: pointer; font-family: inherit; }
.model-select:focus { border-color: var(--color-primary); outline: none; }

.ai-badge { display: flex; align-items: center; gap: 6px; padding: 4px 12px; background: var(--color-primary-bg); border-radius: 20px; font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-primary); }
.ai-status { font-size: var(--font-size-xs); color: var(--color-success); display: flex; align-items: center; gap: 4px; }
.ai-status::before { content: ''; width: 6px; height: 6px; border-radius: 50%; background: var(--color-success); }
.ai-status.streaming::before { background: var(--color-warning); animation: dotPulse 1s infinite; }
@keyframes dotPulse { 0%,100%{opacity:1} 50%{opacity:0.3} }

.tutor-layout { display: flex; gap: 12px; flex: 1; min-height: 0; }

.history-panel { width: 220px; background: var(--color-bg-card); border-radius: var(--radius-md); border: 1px solid var(--color-border-light); padding: 12px; overflow-y: auto; flex-shrink: 0; }
.history-title { font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: 10px; }
.history-empty { font-size: var(--font-size-xs); color: var(--color-text-placeholder); text-align: center; padding: 20px 0; }
.history-item { display: flex; align-items: center; gap: 6px; padding: 8px; border-radius: var(--radius-md); cursor: pointer; font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-bottom: 4px; transition: all 0.15s; }
.history-item:hover { background: var(--color-bg-hover); }
.history-item.active { background: var(--color-primary-bg); color: var(--color-primary); }
.history-id { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.history-del { display: none; background: none; border: none; color: var(--color-text-tertiary); cursor: pointer; font-size: 14px; padding: 2px; }
.history-item:hover .history-del { display: block; }
.history-del:hover { color: var(--color-danger); }

.slide-enter-active, .slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { width: 0; padding: 0; opacity: 0; overflow: hidden; }

.chat-card { flex: 1; display: flex; flex-direction: column; overflow: hidden; background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }
.chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; background: var(--color-bg-page); }

.chat-welcome { text-align: center; padding: 48px 20px; margin: auto 0; }
.welcome-icon { width: 64px; height: 64px; border-radius: 50%; background: var(--color-primary-bg); display: inline-flex; align-items: center; justify-content: center; font-size: 32px; color: var(--color-primary); margin-bottom: 16px; }
.chat-welcome h3 { font-size: var(--font-size-xl); color: var(--color-text-primary); margin-bottom: 10px; font-weight: var(--font-weight-bold); }
.chat-welcome p { font-size: var(--font-size-sm); color: var(--color-text-tertiary); margin-bottom: 22px; max-width: 520px; margin-inline: auto; line-height: 1.6; }
.quick-prompts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.quick-btn { padding: 8px 16px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-bg-card); font-size: var(--font-size-sm); color: var(--color-text-secondary); cursor: pointer; transition: all 0.2s; font-family: inherit; }
.quick-btn:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }

.chat-msg { display: flex; gap: 10px; }
.chat-msg.user { flex-direction: row-reverse; }
.msg-avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.chat-msg.assistant .msg-avatar { background: var(--color-primary-bg); color: var(--color-primary); }
.chat-msg.user .msg-avatar { background: var(--color-primary); color: #fff; }
.msg-bubble { max-width: 75%; padding: 12px 16px; border-radius: 16px; font-size: var(--font-size-sm); line-height: 1.7; word-break: break-word; }
.user-bubble { background: var(--color-primary); color: #fff; border-radius: 16px 16px 4px 16px; }
.ai-bubble { background: var(--color-bg-card); color: var(--color-text-primary); border: 1px solid var(--color-border-light); border-radius: 16px 16px 16px 4px; }
.msg-image { margin-bottom: 8px; }
.msg-image img { max-width: 100%; max-height: 300px; border-radius: var(--radius-md); }

.msg-content :deep(h2) { font-size: var(--font-size-lg); font-weight: var(--font-weight-bold); margin: 12px 0 6px; }
.msg-content :deep(h3) { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); margin: 10px 0 4px; }
.msg-content :deep(h4) { font-size: var(--font-size-base); font-weight: var(--font-weight-semibold); margin: 8px 0 4px; }
.msg-content :deep(ul) { padding-left: 18px; margin: 4px 0; }
.msg-content :deep(li) { margin: 2px 0; }
.msg-content :deep(strong) { font-weight: var(--font-weight-semibold); }
.msg-content :deep(em) { font-style: italic; }
.msg-content :deep(.code-block) { background: #1a1a2e; color: #cdd6f4; border-radius: var(--radius-md); padding: 14px 16px; margin: 8px 0; overflow-x: auto; font-family: 'Consolas', 'Courier New', monospace; font-size: 13px; line-height: 1.6; }
.msg-content :deep(.inline-code) { background: var(--color-border-light); color: var(--color-danger); padding: 2px 6px; border-radius: 4px; font-family: 'Consolas', 'Courier New', monospace; font-size: 13px; }
.typing-cursor { display: inline-block; color: var(--color-primary); animation: blink 1s step-end infinite; font-weight: bold; }
@keyframes blink { 0%,100%{opacity:1} 50%{opacity:0} }

.selected-image { display: flex; align-items: center; gap: 10px; padding: 8px 16px; border-top: 1px solid var(--color-border-light); background: var(--color-bg-page); }
.selected-image img { width: 48px; height: 48px; object-fit: cover; border-radius: var(--radius-sm); }
.img-label { font-size: var(--font-size-xs); color: var(--color-success); }
.img-remove { background: var(--color-danger); color: #fff; border: none; border-radius: 50%; width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 12px; }

.chat-input-area { display: flex; gap: 10px; padding: 14px 16px; border-top: 1px solid var(--color-border-light); align-items: flex-end; }
.upload-btn { width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; border: 1px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; color: var(--color-text-tertiary); font-size: 20px; transition: all 0.2s; flex-shrink: 0; }
.upload-btn:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }
.chat-input-area textarea { flex: 1; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-sm); font-family: inherit; resize: none; background: var(--color-bg-page); line-height: 1.5; }
.chat-input-area textarea:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(22,93,255,0.08); outline: none; }
.send-btn { width: 42px; height: 42px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: #fff; font-size: 18px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; flex-shrink: 0; }
.send-btn:hover:not(:disabled) { background: var(--color-primary-light); transform: scale(1.05); }
.send-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
