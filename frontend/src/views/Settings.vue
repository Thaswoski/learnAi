<template>
  <div class="settings-page">
    <h2 class="page-title">个人信息设置</h2>

    <div class="settings-grid">
      <div class="card">
        <div class="card-header">
          <h3 class="card-title">头像设置</h3>
        </div>
        <div class="card-body avatar-section">
          <div class="avatar-preview">
            <img :src="avatarPreview" alt="用户头像" />
          </div>
          <div class="avatar-actions">
            <label class="btn btn-outline upload-label">
              <i class="ri-upload-2-line"></i> 上传头像
              <input type="file" accept="image/*" hidden @change="handleAvatarChange" />
            </label>
            <button class="btn btn-primary btn-sm" @click="uploadAvatar" :disabled="!avatarFile || uploading">
              {{ uploading ? '上传中...' : '保存头像' }}
            </button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3 class="card-title">基本信息</h3>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label>姓名</label>
            <input v-model="profileForm.name" type="text" placeholder="请输入姓名" />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="profileForm.email" type="email" placeholder="请输入邮箱" />
          </div>
          <div class="form-group">
            <label>角色</label>
            <input :value="roleLabel" disabled class="input-disabled" />
          </div>
          <button class="btn btn-primary" @click="saveProfile" :disabled="savingProfile">
            {{ savingProfile ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3 class="card-title">修改密码</h3>
        </div>
        <div class="card-body">
          <div class="form-group">
            <label>原密码</label>
            <input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" />
          </div>
          <div class="form-group">
            <label>新密码</label>
            <input v-model="pwdForm.newPassword" type="password" placeholder="至少6位字符" />
          </div>
          <div class="form-group">
            <label>确认新密码</label>
            <input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" />
          </div>
          <button class="btn btn-primary" @click="changePassword" :disabled="changingPwd">
            {{ changingPwd ? '修改中...' : '修改密码' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'

const API_BASE = '/api'
const token = () => localStorage.getItem('token') || ''

const avatarPreview = ref('')
const avatarFile = ref(null)
const uploading = ref(false)
const savingProfile = ref(false)
const changingPwd = ref(false)

const profileForm = reactive({
  name: '',
  email: ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const userRole = ref('')

const roleMap = { student: '学生', teacher: '教师', researcher: '研究者' }
const roleLabel = computed(() => roleMap[userRole.value] || userRole.value)

onMounted(() => {
  loadProfile()
})

function loadProfile() {
  fetch(`${API_BASE}/user/profile`, { headers: { 'Authorization': token() } })
    .then(r => r.json())
    .then(data => {
      if (data.code === 200) {
        profileForm.name = data.data.name
        profileForm.email = data.data.email
        userRole.value = data.data.role
        avatarPreview.value = data.data.avatar || ''
        localStorage.setItem('userName', data.data.name)
        localStorage.setItem('userEmail', data.data.email)
        localStorage.setItem('userAvatar', data.data.avatar || '')
        window.dispatchEvent(new Event('user-updated'))
      }
    })
    .catch(() => {})
}

function handleAvatarChange(e) {
  const file = e.target.files[0]
  if (!file) return
  avatarFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => { avatarPreview.value = ev.target.result }
  reader.readAsDataURL(file)
}

function uploadAvatar() {
  if (!avatarFile.value) return
  uploading.value = true
  const formData = new FormData()
  formData.append('file', avatarFile.value)
  fetch(`${API_BASE}/user/avatar`, {
    method: 'POST',
    headers: { 'Authorization': token() },
    body: formData
  })
    .then(r => r.json())
    .then(data => {
      uploading.value = false
      if (data.code === 200) {
        avatarFile.value = null
        const avatarUrl = data.data
        avatarPreview.value = avatarUrl.startsWith('/') ? avatarUrl : '/' + avatarUrl
        localStorage.setItem('userAvatar', avatarPreview.value)
        window.dispatchEvent(new Event('user-updated'))
        alert('头像上传成功')
      } else {
        alert(data.message || '上传失败')
      }
    })
    .catch(() => {
      uploading.value = false
      alert('上传失败，请检查后端')
    })
}

function saveProfile() {
  if (!profileForm.name.trim()) return alert('姓名不能为空')
  if (!profileForm.email.trim()) return alert('邮箱不能为空')

  savingProfile.value = true
  fetch(`${API_BASE}/user/profile`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'Authorization': token() },
    body: JSON.stringify({
      name: profileForm.name,
      email: profileForm.email,
      avatar: avatarPreview.value
    })
  })
    .then(r => r.json())
    .then(data => {
      savingProfile.value = false
      if (data.code === 200) {
        localStorage.setItem('userName', profileForm.name)
        localStorage.setItem('userEmail', profileForm.email)
        localStorage.setItem('userAvatar', data.data.avatar || '')
        window.dispatchEvent(new Event('user-updated'))
        alert('个人信息更新成功')
      } else {
        alert(data.message || '保存失败')
      }
    })
    .catch(() => {
      savingProfile.value = false
      alert('保存失败，请检查后端')
    })
}

function changePassword() {
  if (!pwdForm.oldPassword) return alert('请输入原密码')
  if (!pwdForm.newPassword) return alert('请输入新密码')
  if (pwdForm.newPassword.length < 6) return alert('新密码至少6位')
  if (pwdForm.newPassword !== pwdForm.confirmPassword) return alert('两次输入的新密码不一致')

  changingPwd.value = true
  fetch(`${API_BASE}/user/password`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'Authorization': token() },
    body: JSON.stringify({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
  })
    .then(r => r.json())
    .then(data => {
      changingPwd.value = false
      if (data.code === 200) {
        pwdForm.oldPassword = ''
        pwdForm.newPassword = ''
        pwdForm.confirmPassword = ''
        alert('密码修改成功')
      } else {
        alert(data.message || '密码修改失败')
      }
    })
    .catch(() => {
      changingPwd.value = false
      alert('密码修改失败，请检查后端')
    })
}
</script>

<style scoped>
.settings-page { display: flex; flex-direction: column; gap: 24px; }

.page-title { font-size: var(--font-size-2xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }

.settings-grid { display: flex; flex-direction: column; gap: 20px; }

.card { background: var(--color-bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--color-border-light); }

.card-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 24px; border-bottom: 1px solid var(--color-border-light); }

.card-title { font-size: var(--font-size-md); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }

.card-body { padding: 24px; display: flex; flex-direction: column; gap: 16px; }

.avatar-section { display: flex; align-items: center; gap: 28px; }

.avatar-preview { width: 96px; height: 96px; border-radius: 50%; overflow: hidden; border: 3px solid var(--color-primary-bg); flex-shrink: 0; }

.avatar-preview img { width: 100%; height: 100%; object-fit: cover; }

.avatar-actions { display: flex; flex-direction: column; gap: 10px; }

.upload-label { cursor: pointer; position: relative; display: inline-flex; align-items: center; gap: 6px; }

.form-group { display: flex; flex-direction: column; gap: 6px; }

.form-group label { font-size: var(--font-size-sm); font-weight: var(--font-weight-medium); color: var(--color-text-primary); }

.form-group input { padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-sm); background: var(--color-bg-page); color: var(--color-text-primary); font-family: inherit; transition: border-color 0.2s; }

.form-group input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(22,93,255,0.08); outline: none; }

.input-disabled { background: var(--color-border-light) !important; color: var(--color-text-tertiary) !important; cursor: not-allowed; }
</style>
