<template>
  <div class="landing">
    <nav class="landing-nav">
      <div class="nav-brand">智多星</div>
      <div class="nav-links">
        <a v-for="link in navLinks" :key="link.label" class="nav-link" @click="handleNav(link.to)">
          {{ link.label }}
        </a>
      </div>
      <button class="nav-cta" @click="goApp">{{ isLoggedIn ? '进入系统' : '立即体验' }}</button>
    </nav>

    <section class="hero">
      <div class="hero-bg">
        <img
          class="hero-bg-image"
          src="https://ancient-art-data.oss-cn-guangzhou.aliyuncs.com/upload/pexels-markus-winkler-1430818-4101416.jpg"
          alt=""
        />
        <div class="hero-gradient"></div>
        <div class="hero-grid"></div>
      </div>

      <div class="hero-overlay"></div>

      <div class="hero-content">
        <h1 class="hero-title">
          智多星
          <span class="hero-title-accent"> AI</span>
        </h1>
        <p class="hero-subtitle">AI 驱动个性化学习</p>
        <p class="hero-desc">
          基于大模型的个性化学习系统，智能规划你的专属学习路径。AI 自动生成海量定制化学习资源，实时追踪每日答题进度与知识点掌握分布。提供全流程智能辅导与学习评估，让学习更高效、更精准、更有针对性。
        </p>
        <div class="hero-actions">
          <button class="btn-hero btn-hero-primary" @click="goApp">立即开始学习</button>
          <button class="btn-hero btn-hero-secondary" @click="goApp">查看功能演示</button>
        </div>
        <p class="hero-footer-text">智能学习解决方案提供商。已服务 10000 + 学习者，覆盖全学科全学段学习场景。</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const isLoggedIn = ref(localStorage.getItem('isLoggedIn') === 'true')

onMounted(() => {
  document.body.classList.add('landing-page-body')
})
onUnmounted(() => {
  document.body.classList.remove('landing-page-body')
})

const navLinks = [
  { label: '学习仪表盘', to: '/app/dashboard' },
  { label: '资源生成', to: '/app/resource' },
  { label: '学习路径', to: '/app/path' },
  { label: '智能辅导', to: '/app/tutor' },
  { label: '联系我们', to: '#contact' }
]

function goApp() {
  if (isLoggedIn.value) {
    router.push('/app/dashboard')
  } else {
    router.push('/login')
  }
}

function handleNav(to) {
  if (to === '#contact') return
  if (isLoggedIn.value) {
    router.push(to)
  } else {
    router.push('/login')
  }
}
</script>

<style>
.landing-page-body {
  background-color: #141414 !important;
}

@keyframes fadeUp {
  0% {
    opacity: 0;
    transform: translateY(20px);
    filter: blur(4px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}
</style>

<style scoped>
.landing {
  min-height: 100vh;
  height: 100vh;
  background: #141414;
  color: #f5f5f5;
  font-family: 'Sora', 'Inter', sans-serif;
}

.landing-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px;
}

@media (min-width: 1024px) {
  .landing-nav {
    padding: 20px 64px;
  }
}

.nav-brand {
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: #f5f5f5;
}

.nav-links {
  display: none;
  align-items: center;
  gap: 32px;
}

@media (min-width: 768px) {
  .nav-links {
    display: flex;
  }
}

.nav-link {
  font-size: 14px;
  color: #999;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: color 0.2s;
}

.nav-link:hover {
  color: #f5f5f5;
}

.nav-cta {
  background: #2a2a2a;
  color: #f5f5f5;
  border: none;
  padding: 10px 24px;
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
  font-family: inherit;
  letter-spacing: 0.02em;
}

@media (max-width: 767px) {
  .nav-cta {
    padding: 8px 16px;
    font-size: 13px;
  }
}

.nav-cta:hover {
  background: #3a3a3a;
}

.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
}

.hero-bg-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 80% 60% at 50% 40%, rgba(37, 99, 235, 0.25) 0%, transparent 60%),
              radial-gradient(ellipse 60% 50% at 80% 70%, rgba(37, 99, 235, 0.12) 0%, transparent 50%),
              linear-gradient(180deg, rgba(20, 20, 20, 0.4) 0%, rgba(20, 20, 20, 0.15) 50%, rgba(20, 20, 20, 0.5) 100%);
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 30%, transparent 70%);
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1;
  pointer-events: none;
}

.hero-content {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 640px;
  padding: 0 24px 40px;
}

@media (min-width: 768px) {
  .hero-content {
    max-width: 720px;
    padding: 0 40px 40px;
  }
}

@media (min-width: 1024px) {
  .hero-content {
    max-width: 768px;
    padding: 0 40px 40px;
  }
}

.hero-title {
  font-size: clamp(3rem, 8vw, 6rem);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.05em;
  color: #f5f5f5;
  margin: 0 0 8px;
  animation: fadeUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.2s both;
}

.hero-title-accent {
  color: #3b82f6;
}

.hero-subtitle {
  font-size: clamp(1.125rem, 2.5vw, 1.875rem);
  font-weight: 300;
  color: rgba(245, 245, 245, 0.8);
  margin: 0 0 12px;
  animation: fadeUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.4s both;
}

.hero-desc {
  font-size: clamp(0.875rem, 1.5vw, 1.25rem);
  font-weight: 300;
  color: #999;
  margin: 0 0 16px;
  line-height: 1.7;
  animation: fadeUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.55s both;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-weight: 700;
  animation: fadeUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.7s both;
}

.btn-hero {
  padding: 14px 32px;
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
  border: none;
  font-family: inherit;
  font-weight: 600;
  transition: all 0.15s;
}

.btn-hero:active {
  transform: scale(0.97);
}

.btn-hero-primary {
  background: #3b82f6;
  color: #0a0a0a;
}

.btn-hero-primary:hover {
  filter: brightness(1.1);
}

.btn-hero-secondary {
  background: #fff;
  color: #141414;
}

.btn-hero-secondary:hover {
  filter: brightness(0.9);
}

.hero-footer-text {
  font-size: 12px;
  font-weight: 300;
  color: rgba(153, 153, 153, 0.6);
  margin-top: 16px;
  animation: fadeUp 0.7s cubic-bezier(0.16, 1, 0.3, 1) 0.85s both;
}
</style>
