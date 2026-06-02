<template>
  <div class="video-generate-page">
    <h2 class="page-title">教学视频生成</h2>

    <div class="card">
      <div class="card-header">
        <h3 class="card-title">AI 视频生成</h3>
      </div>
      <div class="card-content">
        <div id="cozeapp-page" class="coze-container"></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'VideoGenerate',
  mounted() {
    this.initCozeApp();

    window.addEventListener('resize', this.handleResize);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    initCozeApp() {
      if (window.CozeWebSDK) {
        this.createCozeInstance();
      } else {
        const script = document.createElement('script');
        script.src = 'https://lf-cdn.coze.cn/obj/unpkg/flow-platform/builder-web-sdk/0.1.1-beta.1/dist/umd/index.js';
        script.onload = () => this.createCozeInstance();
        document.head.appendChild(script);
      }
    },
    createCozeInstance() {
      new window.CozeWebSDK.AppWebSDK({
        token: 'sat_3nsiaxDtUi4iJ3anvUupDDiTy9yckqTVRCVvxWcU4Fj7LHhGvb0yyEOGfz74p7Wu',
        appId: '7645930305290993679',
        container: '#cozeapp-page',
        userInfo: {
          id: this.getUserId(),
          url: this.getUserAvatar(),
          nickname: this.getUserNickname(),
        },
        ui: {
          className: 'coze-app-sdk',
        }
      });
    },
    handleResize() {
      // Coze SDK 通常会自动响应容器大小变化
      // 如果需要手动触发，可以重新初始化
      // 这里不做处理，让 SDK 自己处理
    },
    getUserId() {
      return localStorage.getItem('userId') || 'guest';
    },
    getUserAvatar() {
      return localStorage.getItem('avatar') || 'https://example.com/default.png';
    },
    getUserNickname() {
      return localStorage.getItem('nickname') || '智学用户';
    }
  }
};
</script>

<style scoped>
.video-generate-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: calc(100vh - var(--topbar-height) - 56px);
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
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.card-title {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.card-content {
  flex: 1;
  padding: 20px;
  overflow: hidden;
  min-height: 0;
}

.coze-container {
  width: 100%;
  height: 100%;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* 确保 Coze SDK 的内容填满容器 */
:deep(.coze-app-sdk) {
  width: 100% !important;
  height: 100% !important;
}
</style>
