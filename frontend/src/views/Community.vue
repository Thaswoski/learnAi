<template>
  <div class="community-page">
    <div class="page-header">
      <h2 class="page-title">社区讨论</h2>
      <button class="btn btn-primary" @click="showEditor = true">
        <i class="ri-add-line"></i> 发帖
      </button>
    </div>

    <div class="community-layout">
      <div class="posts-list">
        <div v-if="posts.length === 0" class="card empty-card">
          <i class="ri-chat-3-line empty-icon"></i>
          <h3>暂无帖子</h3>
          <p>成为第一个发起讨论的人吧</p>
        </div>

        <div v-for="post in posts" :key="post.id" class="card post-card">
          <div class="post-top" @click="openPost(post)">
            <h3 class="post-title">{{ post.title }}</h3>
            <div class="post-stats">
              <span class="stat agree"><i class="ri-thumb-up-line"></i> {{ post.agreeCount }}</span>
              <span class="stat"><i class="ri-thumb-down-line"></i> {{ post.disagreeCount }}</span>
              <span class="stat"><i class="ri-eye-line"></i> {{ post.viewCount }}</span>
            </div>
          </div>
          <p class="post-excerpt" @click="openPost(post)">{{ stripMd(post.content) }}</p>
          <div class="post-meta">
            <span class="meta-author"><i class="ri-user-line"></i> {{ post.authorName || '匿名' }}</span>
            <div class="meta-right">
              <span class="meta-time">{{ formatTime(post.createdAt) }}</span>
              <button v-if="isPostOwner(post)" class="btn-del btn-del-text" @click.stop="confirmDeletePost(post.id)" title="删除帖子">
                <i class="ri-delete-bin-line"></i>
              </button>
            </div>
          </div>
        </div>

        <div class="pagination" v-if="totalPages > 1">
          <button class="btn btn-outline btn-sm" :disabled="page <= 1" @click="page--; loadPosts()">上一页</button>
          <span class="page-info">{{ page }} / {{ totalPages }}</span>
          <button class="btn btn-outline btn-sm" :disabled="page >= totalPages" @click="page++; loadPosts()">下一页</button>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="dialog">
        <div v-if="showEditor" class="dialog-overlay" @click.self="showEditor = false">
          <div class="dialog-card dialog-editor">
            <div class="dialog-header">
              <h3>{{ editingPost ? '编辑帖子' : '发布帖子' }}</h3>
              <button class="dialog-close" @click="closeEditor"><i class="ri-close-line"></i></button>
            </div>
            <div class="dialog-body">
              <div class="form-group">
                <label>标题</label>
                <input v-model="form.title" placeholder="请输入标题" maxlength="100" />
              </div>
              <div class="form-group">
                <label>内容 (支持Markdown语法)</label>
                <div class="editor-wrap">
                  <textarea
                    v-model="form.content"
                    class="editor-textarea"
                    placeholder="支持Markdown语法：**加粗** `代码` ![图片](url) ..."
                  ></textarea>
                  <div class="editor-preview" v-if="showPreview">
                    <v-md-preview :text="form.content" />
                  </div>
                </div>
                <div class="editor-toolbar">
                  <div class="md-buttons">
                    <button type="button" @click="insertMd('**', '**')" title="加粗">B</button>
                    <button type="button" @click="insertMd('*', '*')" title="斜体"><i>I</i></button>
                    <button type="button" @click="insertMd('~~', '~~')" title="删除线"><del>S</del></button>
                    <button type="button" @click="insertMd('`', '`')" title="代码">&lt;/&gt;</button>
                    <button type="button" @click="insertMd('\n- ', '')" title="列表">•</button>
                  </div>
                  <div class="editor-actions">
                    <label class="btn btn-outline btn-sm upload-label">
                      <i class="ri-image-add-line"></i> 插入图片
                      <input type="file" accept="image/*" hidden @change="handleImageUpload" ref="imgInput" multiple />
                    </label>
                    <span class="upload-tip" v-if="uploadingImg">上传中...</span>
                    <button class="btn btn-outline btn-sm" @click="showPreview = !showPreview">
                      <i :class="showPreview ? 'ri-edit-line' : 'ri-eye-line'"></i>
                      {{ showPreview ? '编辑' : '预览' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="dialog-footer">
              <button class="btn btn-outline" @click="closeEditor">取消</button>
              <button class="btn btn-primary" @click="submitPost" :disabled="!form.title.trim() || !form.content.trim()">发布</button>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="dialog">
        <div v-if="detailPost" class="dialog-overlay" @click.self="detailPost = null">
          <div class="dialog-card dialog-wide">
            <div class="dialog-header">
              <h3>{{ detailPost.title }}</h3>
              <div class="dialog-header-actions">
                <button v-if="detailPost.authorId === currentUserId" class="btn btn-outline btn-sm btn-danger-outline" @click="confirmDeletePost(detailPost.id)">
                  <i class="ri-delete-bin-line"></i> 删除帖子
                </button>
                <button class="dialog-close" @click="detailPost = null"><i class="ri-close-line"></i></button>
              </div>
            </div>
            <div class="dialog-body">
              <div class="detail-content">
                <v-md-preview :text="detailPost.content" />
              </div>
              <div class="detail-actions">
                <button class="attitude-btn" :class="{ active: myAttitude === 1 }" @click="vote(1)">
                  <i class="ri-thumb-up-line"></i> 赞同 {{ detailPost.agreeCount }}
                </button>
                <button class="attitude-btn" :class="{ active: myAttitude === -1 }" @click="vote(-1)">
                  <i class="ri-thumb-down-line"></i> 反对 {{ detailPost.disagreeCount }}
                </button>
                <span class="detail-author">
                  <i class="ri-user-line"></i> {{ detailPost.authorName }} · {{ formatTime(detailPost.createdAt) }}
                </span>
              </div>

              <div class="comments-section">
                <h4 class="comments-title">评论 ({{ commentCount }})</h4>
                <div v-if="comments.length === 0" class="comments-empty">暂无评论，来说点什么吧</div>

                <div v-for="c in comments" :key="c.id" class="comment-card">
                  <div class="comment-body">
                    <span class="comment-author">{{ c.authorName }}</span>
                    <p class="comment-text">{{ c.content }}</p>
                    <div class="comment-footer">
                      <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                      <button class="reply-link" @click="setReply(c.id, c.authorName)"><i class="ri-reply-line"></i> 回复</button>
                      <button v-if="c.authorId === currentUserId" class="reply-link del-link" @click="confirmDeleteComment(c.id, detailPost.id)">
                        <i class="ri-delete-bin-line"></i> 删除
                      </button>
                    </div>
                  </div>
                  <div v-if="c.replies?.length" class="replies">
                    <div v-for="r in c.replies" :key="r.id" class="reply-item">
                      <span class="comment-author">{{ r.authorName }}</span>
                      <span v-if="r.toUsername" class="reply-to"> 回复 @{{ r.toUsername }}</span>
                      <p class="comment-text">{{ r.content }}</p>
                      <div class="comment-footer">
                        <span class="comment-time">{{ formatTime(r.createdAt) }}</span>
                        <button class="reply-link" @click="setReply(c.id, r.authorName)"><i class="ri-reply-line"></i> 回复</button>
                        <button v-if="r.authorId === currentUserId" class="reply-link del-link" @click="confirmDeleteComment(r.id, detailPost.id)">
                          <i class="ri-delete-bin-line"></i> 删除
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="comment-input-row">
                  <span v-if="replyTarget" class="replying-badge">
                    回复 @{{ replyTarget.name }}
                    <button @click="replyTarget = null"><i class="ri-close-line"></i></button>
                  </span>
                  <div class="comment-input-wrap">
                    <textarea v-model="commentText" rows="2" placeholder="写下你的评论..."></textarea>
                    <button class="btn btn-primary btn-sm" @click="submitComment" :disabled="!commentText.trim()">发表</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>

      <Transition name="dialog">
        <div v-if="showDeleteConfirm" class="dialog-overlay" @click.self="showDeleteConfirm = false">
          <div class="dialog-card dialog-confirm">
            <div class="dialog-header">
              <h3>确认删除</h3>
            </div>
            <div class="dialog-body">
              <p>{{ deleteTarget === 'post' ? '确定要删除这个帖子吗？删除后无法恢复。' : '确定要删除这条评论吗？删除后无法恢复。' }}</p>
            </div>
            <div class="dialog-footer">
              <button class="btn btn-outline" @click="showDeleteConfirm = false">取消</button>
              <button class="btn btn-danger" @click="executeDelete">确认删除</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import VMdPreview from '@kangc/v-md-editor/lib/preview'
import '@kangc/v-md-editor/lib/style/preview.css'
import vuepressTheme from '@kangc/v-md-editor/lib/theme/vuepress.js'
import '@kangc/v-md-editor/lib/theme/style/vuepress.css'

VMdPreview.use(vuepressTheme)

const API = '/api/community'
const posts = ref([])
const page = ref(1)
const pageSize = ref(10)
const totalPosts = ref(0)
const totalPages = computed(() => Math.ceil(totalPosts.value / pageSize.value) || 1)

const showEditor = ref(false)
const showPreview = ref(false)
const uploadingImg = ref(false)
const imgInput = ref(null)
const editingPost = ref(null)
const form = ref({ title: '', content: '' })

const detailPost = ref(null)
const comments = ref([])
const commentText = ref('')
const replyTarget = ref(null)
const myAttitude = ref(0)
const showDeleteConfirm = ref(false)
const deleteTarget = ref('')
const deleteId = ref(null)
const deletePostId = ref(null)
const currentUserId = Number(localStorage.getItem('userId') || '0')
const commentCount = computed(() => {
  let n = comments.value.length
  comments.value.forEach(c => { if (c.replies) n += c.replies.length })
  return n
})

const currentUser = () => ({ userId: currentUserId, userName: localStorage.getItem('userName') || '用户' })

function isPostOwner(post) { return post.authorId === currentUserId }


onMounted(() => loadPosts())

async function loadPosts() {
  const res = await fetch(`${API}/posts?page=${page.value}&size=${pageSize.value}`)
  const data = await res.json()
  if (data.code === 200) {
    posts.value = data.data.records || []
    totalPosts.value = data.data.total || 0
  }
}

function insertMd(before, after) {
  const el = document.querySelector('.editor-textarea')
  if (!el) return
  const start = el.selectionStart, end = el.selectionEnd
  const selected = form.value.content.substring(start, end)
  form.value.content = form.value.content.substring(0, start) + before + selected + after + form.value.content.substring(end)
  nextTick(() => {
    el.focus()
    el.setSelectionRange(start + before.length, start + before.length + selected.length)
  })
}

async function handleImageUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  uploadingImg.value = true
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await fetch(`${API}/upload`, { method: 'POST', body: formData })
    const data = await res.json()
    if (data.code === 200 && data.data?.url) {
      form.value.content += `\n![${data.data.name || '图片'}](${data.data.url})\n`
    }
  } catch (err) {
    console.error('上传失败', err)
  } finally {
    uploadingImg.value = false
    if (imgInput.value) imgInput.value.value = ''
  }
}

async function submitPost() {
  const user = currentUser()
  const body = { ...form.value, authorId: user.userId, authorName: user.userName }
  if (editingPost.value) {
    await fetch(`${API}/posts/${editingPost.value.id}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  } else {
    await fetch(`${API}/posts`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  }
  closeEditor()
  loadPosts()
}

function closeEditor() {
  showEditor.value = false
  showPreview.value = false
  editingPost.value = null
  form.value = { title: '', content: '' }
}

async function openPost(post) {
  detailPost.value = post
  myAttitude.value = 0
  const res = await fetch(`${API}/posts/${post.id}`)
  const data = await res.json()
  if (data.code === 200) detailPost.value = data.data || post
  loadComments(post.id)
}

async function loadComments(postId) {
  const res = await fetch(`${API}/comments/post/${postId}`)
  const data = await res.json()
  comments.value = data.data || []
}

async function vote(attitude) {
  const user = currentUser()
  const res = await fetch(`${API}/posts/attitude`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ postId: detailPost.value.id, userId: user.userId, attitude })
  })
  const data = await res.json()
  if (data.code === 200) {
    myAttitude.value = data.data.userAttitude
    if (myAttitude.value === 0) { if (attitude === 1) detailPost.value.agreeCount--; else detailPost.value.disagreeCount-- }
    else if (attitude === 1) detailPost.value.agreeCount++
    else detailPost.value.disagreeCount++
  }
}

function setReply(id, name) { replyTarget.value = { id, name } }

async function submitComment() {
  const user = currentUser()
  const body = { postId: detailPost.value.id, authorId: user.userId, authorName: user.userName, content: commentText.value }
  if (replyTarget.value) { body.parentId = replyTarget.value.id; body.toUsername = replyTarget.value.name }
  await fetch(`${API}/comments`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
  commentText.value = ''
  replyTarget.value = null
  loadComments(detailPost.value.id)
}

function stripMd(text) {
  if (!text) return ''
  return text.replace(/[#*`>~\[\]()|!\-]/g, '').replace(/\n+/g, ' ').substring(0, 200)
}

function confirmDeletePost(postId) {
  deleteTarget.value = 'post'
  deleteId.value = postId
  showDeleteConfirm.value = true
}

function confirmDeleteComment(commentId, postId) {
  deleteTarget.value = 'comment'
  deleteId.value = commentId
  deletePostId.value = postId
  showDeleteConfirm.value = true
}

async function executeDelete() {
  showDeleteConfirm.value = false
  if (deleteTarget.value === 'post') {
    await fetch(`${API}/posts/${deleteId.value}`, { method: 'DELETE' })
    detailPost.value = null
    loadPosts()
  } else {
    await fetch(`${API}/comments/${deleteId.value}`, { method: 'DELETE' })
    if (deletePostId.value) loadComments(deletePostId.value)
  }
}

function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '' }
</script>

<style scoped>
.community-page { display: flex; flex-direction: column; gap: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }
.empty-card { padding: 60px 40px; text-align: center; }
.empty-icon { font-size: 48px; color: var(--color-text-placeholder); display: block; margin-bottom: 12px; }
.empty-card h3 { font-size: var(--font-size-lg); color: var(--color-text-primary); margin-bottom: 6px; }
.empty-card p { font-size: var(--font-size-sm); color: var(--color-text-tertiary); }

.post-card { padding: 20px; cursor: pointer; transition: all 0.2s; }
.post-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); border-color: var(--color-primary); }
.post-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 10px; }
.post-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); flex: 1; }
.post-stats { display: flex; gap: 12px; flex-shrink: 0; }
.stat { font-size: var(--font-size-xs); color: var(--color-text-tertiary); display: flex; align-items: center; gap: 3px; }
.stat.agree { color: var(--color-success); }
.post-excerpt { font-size: var(--font-size-sm); color: var(--color-text-secondary); line-height: 1.6; margin-bottom: 12px; }
.post-meta { display: flex; justify-content: space-between; }
.meta-author { font-size: var(--font-size-xs); color: var(--color-text-tertiary); display: flex; align-items: center; gap: 4px; }
.meta-right { display: flex; align-items: center; gap: 8px; }
.meta-time { font-size: var(--font-size-xs); color: var(--color-text-placeholder); }
.btn-del { opacity: 0; transition: opacity 0.15s, color 0.15s; color: var(--color-text-tertiary); cursor: pointer; }
.btn-del-text { background: none; border: none; padding: 2px; font-size: 14px; }
.post-card:hover .btn-del { opacity: 1; }
.btn-del:hover { color: var(--color-danger); }
.posts-list { display: flex; flex-direction: column; gap: 12px; }

.pagination { display: flex; justify-content: center; gap: 12px; align-items: center; margin-top: 16px; }
.page-info { font-size: var(--font-size-sm); color: var(--color-text-tertiary); }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 40px; }
.dialog-card { background: var(--color-bg-card); border-radius: var(--radius-lg); width: 100%; max-width: 560px; max-height: 85vh; overflow-y: auto; box-shadow: var(--shadow-xl); }
.dialog-editor { max-width: 860px; }
.dialog-wide { max-width: 780px; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid var(--color-border-light); position: sticky; top: 0; background: var(--color-bg-card); z-index: 1; }
.dialog-header h3 { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }
.dialog-header-actions { display: flex; align-items: center; gap: 8px; }
.dialog-close { background: none; border: none; font-size: 20px; color: var(--color-text-tertiary); cursor: pointer; padding: 4px; border-radius: var(--radius-sm); }
.dialog-close:hover { background: var(--color-bg-hover); }
.dialog-body { padding: 24px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 16px 24px; border-top: 1px solid var(--color-border-light); position: sticky; bottom: 0; background: var(--color-bg-card); }
.dialog-confirm { max-width: 420px; }

.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group label { font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); }
.form-group input { padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-sm); background: var(--color-bg-page); font-family: inherit; }
.form-group input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(22,93,255,0.08); outline: none; }

.editor-wrap { display: flex; gap: 0; border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden; min-height: 400px; }
.editor-textarea { flex: 1; border: none; padding: 16px; font-family: 'Consolas', 'Courier New', monospace; font-size: 14px; line-height: 1.7; resize: none; background: var(--color-bg-page); color: var(--color-text-primary); }
.editor-textarea:focus { outline: none; }
.editor-preview { flex: 1; border-left: 1px solid var(--color-border); padding: 16px; background: #fff; overflow-y: auto; }

.editor-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; gap: 8px; }

.md-buttons { display: flex; gap: 2px; }
.md-buttons button { width: 32px; height: 32px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-bg-card); font-size: 14px; font-weight: var(--font-weight-semibold); font-family: inherit; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.15s; }
.md-buttons button:hover { border-color: var(--color-primary); color: var(--color-primary); background: var(--color-primary-bg); }

.editor-actions { display: flex; align-items: center; gap: 8px; }
.upload-label { cursor: pointer; margin: 0; }
.upload-tip { font-size: var(--font-size-xs); color: var(--color-warning); }

.detail-content { font-size: var(--font-size-base); color: var(--color-text-primary); line-height: 1.8; margin-bottom: 16px; }
.detail-content :deep(img) { max-width: 100%; border-radius: var(--radius-md); margin: 8px 0; }
.detail-actions { display: flex; gap: 12px; padding: 12px 0; border-top: 1px solid var(--color-border-light); border-bottom: 1px solid var(--color-border-light); margin-bottom: 20px; align-items: center; }
.attitude-btn { display: flex; align-items: center; gap: 4px; padding: 6px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-bg-page); cursor: pointer; font-size: var(--font-size-sm); transition: all 0.2s; font-family: inherit; }
.attitude-btn:hover { border-color: var(--color-primary); }
.attitude-btn.active { background: var(--color-primary-bg); border-color: var(--color-primary); color: var(--color-primary); }
.detail-author { margin-left: auto; font-size: var(--font-size-sm); color: var(--color-text-tertiary); display: flex; align-items: center; gap: 4px; }

.comments-section { margin-top: 8px; }
.comments-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin-bottom: 16px; }
.comments-empty { text-align: center; padding: 24px; font-size: var(--font-size-sm); color: var(--color-text-placeholder); }

.comment-card { padding: 14px 0; border-bottom: 1px solid var(--color-border-light); }
.comment-author { font-size: var(--font-size-sm); font-weight: var(--font-weight-semibold); color: var(--color-primary); }
.comment-text { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin: 4px 0; line-height: 1.5; }
.comment-footer { display: flex; gap: 12px; margin-top: 4px; }
.comment-time { font-size: var(--font-size-xs); color: var(--color-text-placeholder); }
.reply-link { background: none; border: none; color: var(--color-text-tertiary); cursor: pointer; display: flex; align-items: center; gap: 3px; font-size: var(--font-size-xs); font-family: inherit; }
.reply-link:hover { color: var(--color-primary); }
.del-link:hover { color: var(--color-danger) !important; }

.replies { margin-left: 32px; padding-left: 16px; border-left: 2px solid var(--color-primary-bg); margin-top: 8px; }
.reply-item { padding: 8px 0; }
.reply-to { color: var(--color-text-tertiary); font-size: var(--font-size-xs); }

.comment-input-row { margin-top: 16px; }
.replying-badge { display: inline-flex; align-items: center; gap: 6px; font-size: var(--font-size-xs); color: var(--color-primary); background: var(--color-primary-bg); padding: 4px 10px; border-radius: 12px; margin-bottom: 8px; }
.replying-badge button { background: none; border: none; cursor: pointer; color: var(--color-text-tertiary); font-size: 14px; }
.comment-input-wrap { display: flex; flex-direction: column; gap: 8px; }
.comment-input-wrap textarea { padding: 10px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-sm); background: var(--color-bg-page); font-family: inherit; resize: vertical; }
.comment-input-wrap textarea:focus { border-color: var(--color-primary); outline: none; box-shadow: 0 0 0 3px rgba(22,93,255,0.08); }

.dialog-enter-active, .dialog-leave-active { transition: opacity 0.2s ease; }
.dialog-enter-active .dialog-card, .dialog-leave-active .dialog-card { transition: transform 0.2s ease, opacity 0.2s ease; }
.dialog-enter-from, .dialog-leave-to { opacity: 0; }
.dialog-enter-from .dialog-card { transform: scale(0.95) translateY(10px); }
.dialog-leave-to .dialog-card { transform: scale(0.95) translateY(10px); }
</style>
