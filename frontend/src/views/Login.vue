<template>
  <div class="auth-page">
    <div class="auth-bg">
      <div class="bg-circle c1"></div>
      <div class="bg-circle c2"></div>
      <div class="bg-circle c3"></div>
      <div class="bg-dots"></div>
    </div>

    <div class="auth-container">
      <div class="auth-left">
        <div class="brand-area">
          <div class="brand-icon">
            <i class="ri-brain-line"></i>
          </div>
          <h1 class="brand-name">智多星<span class="brand-dot">.</span></h1>
          <p class="brand-tagline">基于大模型的个性化学习系统</p>
        </div>
      </div>

      <div class="auth-right">
        <div class="auth-card" :class="{ flipping: isFlipping }">
          <div class="auth-header">
            <div class="auth-tabs">
              <button
                class="auth-tab"
                :class="{ active: mode === 'login' }"
                @click="switchMode('login')"
              >登录</button>
              <button
                class="auth-tab"
                :class="{ active: mode === 'register' }"
                @click="switchMode('register')"
              >注册</button>
            </div>
            <div class="tab-indicator" :class="{ right: mode === 'register' }"></div>
          </div>

          <form class="auth-form" @submit.prevent="handleSubmit" autocomplete="off">
            <Transition name="form-slide" mode="out-in">
              <div v-if="mode === 'login'" key="login" class="form-fields">
                <div class="form-group">
                  <label>邮箱 / 用户名</label>
                  <div class="input-wrap">
                    <i class="ri-mail-line input-icon"></i>
                    <input
                      v-model="loginForm.email"
                      type="text"
                      placeholder="请输入邮箱或用户名"
                      :class="{ error: loginErrors.email }"
                    />
                  </div>
                  <span v-if="loginErrors.email" class="field-error">{{ loginErrors.email }}</span>
                </div>

                <div class="form-group">
                  <div class="label-row">
                    <label>密码</label>
                    <a href="#" class="forgot-link">忘记密码？</a>
                  </div>
                  <div class="input-wrap">
                    <i class="ri-lock-line input-icon"></i>
                    <input
                      v-model="loginForm.password"
                      :type="showLoginPwd ? 'text' : 'password'"
                      placeholder="请输入密码"
                      :class="{ error: loginErrors.password }"
                    />
                    <button type="button" class="pwd-toggle" @click="showLoginPwd = !showLoginPwd">
                      <i :class="showLoginPwd ? 'ri-eye-off-line' : 'ri-eye-line'"></i>
                    </button>
                  </div>
                  <span v-if="loginErrors.password" class="field-error">{{ loginErrors.password }}</span>
                </div>

                <div class="form-options">
                  <label class="remember-me">
                    <input type="checkbox" v-model="loginForm.remember" />
                    <span class="checkmark"></span>
                    记住我
                  </label>
                </div>

                <button type="submit" class="submit-btn" :disabled="isSubmitting">
                  <span v-if="!isSubmitting">登 录</span>
                  <span v-else class="spinner"></span>
                </button>
              </div>

              <div v-else key="register" class="form-fields">
                <div class="form-row">
                  <div class="form-group half">
                    <label>姓名</label>
                    <div class="input-wrap">
                      <i class="ri-user-line input-icon"></i>
                      <input
                        v-model="registerForm.name"
                        type="text"
                        placeholder="你的姓名"
                        :class="{ error: registerErrors.name }"
                      />
                    </div>
                    <span v-if="registerErrors.name" class="field-error">{{ registerErrors.name }}</span>
                  </div>
                  <div class="form-group half">
                    <label>角色</label>
                    <div class="input-wrap">
                      <i class="ri-user-settings-line input-icon"></i>
                      <select v-model="registerForm.role">
                        <option value="">请选择</option>
                        <option value="student">学生</option>
                        <option value="teacher">教师</option>
                        <option value="researcher">研究者</option>
                      </select>
                    </div>
                    <span v-if="registerErrors.role" class="field-error">{{ registerErrors.role }}</span>
                  </div>
                </div>

                <div class="form-group">
                  <label>邮箱地址</label>
                  <div class="input-wrap">
                    <i class="ri-mail-line input-icon"></i>
                    <input
                      v-model="registerForm.email"
                      type="email"
                      placeholder="请输入邮箱"
                      :class="{ error: registerErrors.email }"
                    />
                  </div>
                  <span v-if="registerErrors.email" class="field-error">{{ registerErrors.email }}</span>
                </div>

                <div class="form-group">
                  <label>密码</label>
                  <div class="input-wrap">
                    <i class="ri-lock-line input-icon"></i>
                    <input
                      v-model="registerForm.password"
                      :type="showRegPwd ? 'text' : 'password'"
                      placeholder="至少6位字符"
                      :class="{ error: registerErrors.password }"
                    />
                    <button type="button" class="pwd-toggle" @click="showRegPwd = !showRegPwd">
                      <i :class="showRegPwd ? 'ri-eye-off-line' : 'ri-eye-line'"></i>
                    </button>
                  </div>
                  <span v-if="registerErrors.password" class="field-error">{{ registerErrors.password }}</span>
                </div>

                <div class="form-group">
                  <label>确认密码</label>
                  <div class="input-wrap">
                    <i class="ri-lock-line input-icon"></i>
                    <input
                      v-model="registerForm.confirmPassword"
                      :type="showRegPwd ? 'text' : 'password'"
                      placeholder="请再次输入密码"
                      :class="{ error: registerErrors.confirmPassword }"
                    />
                  </div>
                  <span v-if="registerErrors.confirmPassword" class="field-error">{{ registerErrors.confirmPassword }}</span>
                </div>

                <div class="form-options">
                  <label class="remember-me">
                    <input type="checkbox" v-model="registerForm.agree" />
                    <span class="checkmark"></span>
                    我已阅读并同意 <a href="#" class="terms-link">《用户协议》</a> 和 <a href="#" class="terms-link">《隐私政策》</a>
                  </label>
                </div>
                <span v-if="registerErrors.agree" class="field-error agree-error">{{ registerErrors.agree }}</span>

                <button type="submit" class="submit-btn" :disabled="isSubmitting">
                  <span v-if="!isSubmitting">注 册</span>
                  <span v-else class="spinner"></span>
                </button>
              </div>
            </Transition>
          </form>

          <div class="auth-divider">
            <span>或</span>
          </div>

          <div class="social-login">
            <button class="social-btn" title="微信登录">
              <i class="ri-wechat-fill"></i>
            </button>
            <button class="social-btn" title="QQ登录">
              <i class="ri-qq-fill"></i>
            </button>
          </div>

          <div class="auth-switch">
            <template v-if="mode === 'login'">
              还没有账号？<a href="#" @click.prevent="switchMode('register')">立即注册</a>
            </template>
            <template v-else>
              已有账号？<a href="#" @click.prevent="switchMode('login')">立即登录</a>
            </template>
          </div>


        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const mode = ref('login')
const isFlipping = ref(false)
const isSubmitting = ref(false)
const showLoginPwd = ref(false)
const showRegPwd = ref(false)

const loginForm = reactive({
  email: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  name: '',
  role: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: false
})

const loginErrors = reactive({ email: '', password: '' })
const registerErrors = reactive({ name: '', role: '', email: '', password: '', confirmPassword: '', agree: '' })

function switchMode(target) {
  if (mode.value === target) return
  isFlipping.value = true
  setTimeout(() => {
    mode.value = target
    clearErrors()
    setTimeout(() => {
      isFlipping.value = false
    }, 50)
  }, 150)
}

function clearErrors() {
  Object.keys(loginErrors).forEach(k => (loginErrors[k] = ''))
  Object.keys(registerErrors).forEach(k => (registerErrors[k] = ''))
}

function validateLogin() {
  let valid = true
  clearErrors()
  if (!loginForm.email.trim()) {
    loginErrors.email = '请输入邮箱或用户名'
    valid = false
  }
  if (!loginForm.password.trim()) {
    loginErrors.password = '请输入密码'
    valid = false
  } else if (loginForm.password.length < 3) {
    loginErrors.password = '密码长度不能少于3位'
    valid = false
  }
  return valid
}

function validateRegister() {
  let valid = true
  clearErrors()
  if (!registerForm.name.trim()) {
    registerErrors.name = '请输入姓名'
    valid = false
  }
  if (!registerForm.role) {
    registerErrors.role = '请选择角色'
    valid = false
  }
  if (!registerForm.email.trim()) {
    registerErrors.email = '请输入邮箱地址'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) {
    registerErrors.email = '邮箱格式不正确'
    valid = false
  }
  if (!registerForm.password.trim()) {
    registerErrors.password = '请输入密码'
    valid = false
  } else if (registerForm.password.length < 6) {
    registerErrors.password = '密码长度至少6位'
    valid = false
  }
  if (!registerForm.confirmPassword.trim()) {
    registerErrors.confirmPassword = '请确认密码'
    valid = false
  } else if (registerForm.password !== registerForm.confirmPassword) {
    registerErrors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  if (!registerForm.agree) {
    registerErrors.agree = '请阅读并同意用户协议和隐私政策'
    valid = false
  }
  return valid
}

const API_BASE = '/api'

function handleSubmit() {
  if (mode.value === 'login') {
    if (!validateLogin()) return
    isSubmitting.value = true
    fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: loginForm.email, password: loginForm.password })
    })
      .then(res => res.json())
      .then(data => {
        isSubmitting.value = false
        if (data.code === 200) {
          localStorage.setItem('isLoggedIn', 'true')
          localStorage.setItem('token', data.data.token)
          localStorage.setItem('userId', data.data.userId)
          localStorage.setItem('userName', data.data.name)
          localStorage.setItem('userEmail', data.data.email)
          localStorage.setItem('userAvatar', data.data.avatar || '')
          router.push('/app/dashboard')
        } else {
          alert(data.message || '登录失败')
        }
      })
      .catch(() => {
        isSubmitting.value = false
        alert('服务器连接失败，请检查后端是否启动')
      })
  } else {
    if (!validateRegister()) return
    isSubmitting.value = true
    fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: registerForm.name,
        email: registerForm.email,
        password: registerForm.password,
        role: registerForm.role
      })
    })
      .then(res => res.json())
      .then(data => {
        isSubmitting.value = false
        if (data.code === 200) {
          localStorage.setItem('isLoggedIn', 'true')
          localStorage.setItem('token', data.data.token)
          localStorage.setItem('userId', data.data.userId)
          localStorage.setItem('userName', data.data.name)
          localStorage.setItem('userEmail', data.data.email)
          localStorage.setItem('userAvatar', data.data.avatar || '')
          router.push('/app/dashboard')
        } else {
          alert(data.message || '注册失败')
        }
      })
      .catch(() => {
        isSubmitting.value = false
        alert('服务器连接失败，请检查后端是否启动')
      })
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-page);
  position: relative;
  overflow: hidden;
  font-family: var(--font-family);
}

.auth-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
}

.bg-circle.c1 {
  width: 600px;
  height: 600px;
  background: var(--color-primary);
  top: -200px;
  right: -100px;
}

.bg-circle.c2 {
  width: 400px;
  height: 400px;
  background: var(--color-primary);
  bottom: -100px;
  left: -80px;
}

.bg-circle.c3 {
  width: 200px;
  height: 200px;
  background: var(--color-success);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0.04;
}

.bg-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(var(--color-border) 1px, transparent 1px);
  background-size: 28px 28px;
  opacity: 0.4;
}

.auth-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 980px;
  min-height: 600px;
  background: var(--color-bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
}

.auth-left {
  width: 420px;
  flex-shrink: 0;
  background: var(--color-primary-gradient);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.auth-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.04'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
  pointer-events: none;
}

.brand-area {
  position: relative;
  z-index: 1;
}

.brand-icon {
  width: 52px;
  height: 52px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
  margin-bottom: 20px;
}

.brand-name {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.02em;
  margin-bottom: 8px;
}

.brand-dot {
  color: rgba(255, 255, 255, 0.7);
}

.brand-tagline {
  font-size: var(--font-size-sm);
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.5;
}



.auth-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
}

.auth-card {
  width: 100%;
  max-width: 380px;
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.auth-card.flipping {
  opacity: 0;
  transform: translateY(8px);
}

.auth-header {
  position: relative;
  margin-bottom: 32px;
}

.auth-tabs {
  display: flex;
  gap: 0;
  position: relative;
  z-index: 1;
}

.auth-tab {
  flex: 1;
  padding: 10px 0;
  background: none;
  border: none;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: color 0.25s;
  text-align: center;
}

.auth-tab.active {
  color: var(--color-text-primary);
}

.tab-indicator {
  height: 2px;
  width: 50%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: -2px;
  position: relative;
  z-index: 1;
}

.tab-indicator.right {
  transform: translateX(100%);
}

.auth-form {
  overflow: hidden;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group.half {
  flex: 1;
}

.form-group label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.forgot-link {
  font-size: var(--font-size-sm);
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.forgot-link:hover {
  text-decoration: underline;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 12px;
  font-size: 16px;
  color: var(--color-text-placeholder);
  z-index: 1;
  pointer-events: none;
}

.input-wrap input,
.input-wrap select {
  width: 100%;
  padding: 10px 12px 10px 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  transition: all 0.2s;
  font-family: inherit;
}

.input-wrap input:focus,
.input-wrap select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.08);
  background: var(--color-bg-card);
}

.input-wrap input.error,
.input-wrap select.error {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px rgba(245, 63, 63, 0.06);
}

.input-wrap input::placeholder {
  color: var(--color-text-placeholder);
}

.pwd-toggle {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  color: var(--color-text-placeholder);
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
}

.pwd-toggle:hover {
  color: var(--color-text-tertiary);
}

.field-error {
  font-size: var(--font-size-xs);
  color: var(--color-danger);
  line-height: 1;
}

.agree-error {
  margin-top: -8px;
}

.form-options {
  display: flex;
  align-items: center;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  user-select: none;
}

.remember-me input[type="checkbox"] {
  display: none;
}

.checkmark {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-border);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.remember-me input:checked + .checkmark {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.remember-me input:checked + .checkmark::after {
  content: '';
  width: 5px;
  height: 9px;
  border: 2px solid #fff;
  border-top: none;
  border-left: none;
  transform: rotate(45deg) translateY(-1px);
}

.terms-link {
  color: var(--color-primary);
  text-decoration: none;
}

.terms-link:hover {
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: all 0.25s;
  letter-spacing: 4px;
  margin-top: 6px;
  font-family: inherit;
  position: relative;
  overflow: hidden;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-light);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.auth-divider {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 24px 0;
}

.auth-divider::before,
.auth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border-light);
}

.auth-divider span {
  font-size: var(--font-size-sm);
  color: var(--color-text-placeholder);
}

.social-login {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.social-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-card);
  color: var(--color-text-secondary);
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.social-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-bg);
  transform: translateY(-2px);
}

.auth-switch {
  text-align: center;
  margin-top: 20px;
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
}

.auth-switch a {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.auth-switch a:hover {
  text-decoration: underline;
}

.form-slide-enter-active,
.form-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-slide-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.form-slide-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}

@media (max-width: 1024px) {
  .auth-container {
    width: 92vw;
    flex-direction: column;
    min-height: auto;
    max-height: 95vh;
    overflow-y: auto;
  }

  .auth-left {
    width: 100%;
    padding: 32px 28px;
  }

  .auth-right {
    padding: 32px 28px;
  }
}
</style>
