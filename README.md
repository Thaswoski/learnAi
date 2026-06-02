# 智多星 (LearnAI)

> 基于大模型的多智能体协同个性化学习系统  
> Multi-Agent Collaborative Personalized Learning System

---

## 项目简介

智多星是一个面向高等教育的**多智能体协同学习系统**，以 C 语言程序设计为切入点，融合 DeepSeek、讯飞星火、通义千问等大模型以及豆包 Seedream 多模态生成能力，通过 **6 个专业 Agent 协同工作**，为学生提供从学习画像构建、多模态资源生成、个性化路径规划、智能答疑辅导到学习效果评估的全链路个性化学习支持。

### 核心亮点

- **多智能体事件驱动协同** — 基于 `AgentEventBus` 发布/订阅机制实现 Agent 间双向通信，支持审核→修正→再审的迭代优化循环
- **9 维动态学生画像** — 通过自然语言对话自动抽取，随学随新
- **6 类多模态资源生成** — PPT课件、知识文档、思维导图、练习题、拓展阅读、教学视频
- **C 语言在线判题** — GCC 编译 + 沙箱执行 + 结果比对
- **5 维能力雷达评估** — 理论理解 / 编程实现 / 问题解决 / 知识应用 / 创新思维

---

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│                    Vue 3 Frontend                     │
│   Dashboard │ Profile │ Resource │ Path │ Tutor      │
│   Evaluation │ QuestionBank │ Community │ Settings   │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP/SSE
┌──────────────────────▼──────────────────────────────┐
│              Spring Boot 3.2 Backend                  │
│                                                       │
│  ┌─────────────────────────────────────────────┐     │
│  │       CollaborativeOrchestrator               │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐   │     │
│  │  │ Planner   │  │ Search   │  │ Content   │   │     │
│  │  │ Agent     │  │ Agent    │  │ Agent     │   │     │
│  │  ├──────────┤  ├──────────┤  ├──────────┤   │     │
│  │  │ Reviewer │  │ Path     │  │ PPT       │   │     │
│  │  │ Agent    │  │ Planner  │  │ Agent     │   │     │
│  │  └──────────┘  └──────────┘  └──────────┘   │     │
│  │         ┌────────────────────┐               │     │
│  │         │   AgentEventBus    │               │     │
│  │         │  (发布/订阅事件)    │               │     │
│  │         └────────────────────┘               │     │
│  └─────────────────────────────────────────────┘     │
└──────┬───────────────────────────────┬──────────────┘
       │                               │
       ▼                               ▼
┌──────────────┐            ┌──────────────────┐
│   MySQL 8.0  │            │  Python FastAPI   │
│   Database   │            │  ppt-service:5050 │
└──────────────┘            │  PPT/DOCX 生成    │
                            └──────────────────┘
```

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| **前端** | Vue 3 + Vite 5 + Vue Router 4 | SPA 单页应用，Hash 路由 |
|    | ECharts 5 + vue-echarts | 数据可视化图表 |
|    | @kangc/v-md-editor | Markdown 编辑与渲染 |
| **后端** | Spring Boot 3.2.5 + JDK 17 | RESTful API + SSE 流式推送 |
|    | MyBatis 3.0 | 数据持久层 |
|    | Apache POI 5.2 | Java 端 PPT 生成 |
|    | Graphviz Java 0.18 | 思维导图 PNG 渲染 |
|    | BCrypt | 密码加密 |
| **Python** | FastAPI + uvicorn | 多模态资源生成微服务 |
|    | python-pptx 1.0 + python-docx 1.1 | PPT/DOCX 文件渲染 |
|    | Pygments 2.18 | 代码语法高亮 |
| **AI 模型** | DeepSeek V4 Pro | 课程规划、内容生成、学习评估 |
|    | 讯飞星火 Spark X2 | 智能答疑、备用生成模型 |
|    | 通义千问 Qwen-Plus | 多模态答疑（支持图片理解） |
|    | 豆包 Seedream 4.0 | 教育配图生成 |
|    | 讯飞 ONE SEARCH | 全网教育资源搜索 |
| **数据库** | MySQL 8.0 | 用户、题库、画像、评估等 |

---

## 项目结构

```
learnAi/
├── 一键启动.bat              # 一键启动脚本（Windows）
├── frontend/                 # Vue 3 前端
│   ├── src/
│   │   ├── router/index.js   # 路由配置
│   │   ├── views/            # 页面组件
│   │   │   ├── Dashboard.vue  # 学习仪表盘
│   │   │   ├── Profile.vue    # AI 对话学习画像
│   │   │   ├── Resource.vue   # 多模态资源生成
│   │   │   ├── Path.vue       # 个性化学习路径
│   │   │   ├── TutorChat.vue  # 智能答疑辅导
│   │   │   ├── Evaluation.vue # 学习效果评估
│   │   │   ├── QuestionBank.vue # C 语言题库
│   │   │   ├── Community.vue  # 社区讨论
│   │   │   └── ...
│   │   └── components/       # 公共组件
│   └── package.json
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/griya/learn/
│   │   ├── agent/            # ★ 多智能体核心
│   │   │   ├── Agent.java            # Agent 接口
│   │   │   ├── AgentContext.java     # 共享上下文
│   │   │   ├── AgentEvent.java       # Agent 事件类型
│   │   │   ├── AgentEventBus.java    # 事件总线（发布/订阅）
│   │   │   ├── CollaborativeOrchestrator.java  # 协同编排器
│   │   │   ├── MultiAgentOrchestrator.java     # 基础编排器（降级保留）
│   │   │   ├── WorkflowEngine.java  # 工作流引擎
│   │   │   ├── PlannerAgent.java    # 课程规划师
│   │   │   ├── SearchAgent.java     # 讯飞搜索员
│   │   │   ├── ContentAgent.java    # 内容创作者
│   │   │   ├── ReviewerAgent.java   # 质量审核员
│   │   │   ├── PathPlannerAgent.java # 路径规划师
│   │   │   └── PPTAgent.java        # PPT 生成器
│   │   ├── service/          # 业务服务
│   │   │   ├── AiService.java        # AI 接口
│   │   │   ├── DeepSeekService.java  # DeepSeek 实现
│   │   │   ├── SparkAiService.java   # 讯飞星火实现
│   │   │   ├── QwenAiService.java    # 通义千问实现
│   │   │   ├── XfSearchService.java  # 讯飞搜索实现
│   │   │   ├── MindMapService.java   # 思维导图生成
│   │   │   ├── CodeJudgeService.java # C 语言在线判题
│   │   │   ├── EvaluationService.java # 学习效果评估
│   │   │   ├── DashboardService.java # 仪表盘数据
│   │   │   └── ...
│   │   ├── controller/       # REST API 控制器
│   │   ├── entity/           # 数据库实体
│   │   └── config/           # Spring 配置
│   ├── src/main/resources/
│   │   ├── application.yml   # 主配置文件
│   │   └── db/               # 数据库脚本
│   └── pom.xml
├── ppt-service/              # Python 多模态生成微服务
│   ├── main.py               # FastAPI 服务主文件
│   └── requirements.txt      # Python 依赖
└── backend/downloadData/     # 生成文件存储
    ├── ppt/                  # PPT 课件
    ├── xmind/                # 思维导图 PNG
    └── exploreReading/       # 拓展阅读 DOCX
```

---

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.6+ |
| Node.js | 18+ |
| Python | 3.10+ |
| MySQL | 8.0+ |
| GCC (判题) | 任意版本 |

---

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < backend/src/main/resources/db/schema.sql
mysql -u root -p ai_learn < backend/src/main/resources/db/ai_learn.sql
```

### 2. 安装依赖

```bash
# 后端依赖
cd backend && mvn install -DskipTests

# 前端依赖
cd frontend && npm install

# Python 依赖
cd ppt-service && pip install -r requirements.txt
```

### 3. 配置 API Key

编辑 `backend/src/main/resources/application.yml`，填入你的 API Key：

```yaml
deepseek:
  api-key: your-deepseek-api-key

qwen:
  api-key: your-qwen-api-key

spark:
  app-id: your-spark-app-id
  api-key: your-spark-api-key
  api-secret: your-spark-api-secret
```

编辑 `ppt-service/main.py`，填入 Python 侧的 API Key（文件头部常量）：
- `DEEPSEEK_API_KEY`
- `SPARK_API_KEY`
- `DOUBAO_API_KEY`
- `XF_SEARCH_API_PASSWORD`

### 4. 一键启动

双击项目根目录的 `一键启动.bat`，将同时启动三个窗口：

| 服务 | 端口 | 启动命令 |
|------|------|---------|
| Python PPT 服务 | 5050 | `python main.py` |
| Java 后端 | 6060 | `mvn spring-boot:run` |
| Vue 前端 | 5173 | `npm run dev` |

浏览器访问 **http://localhost:5173** 即可使用。

---

## 功能模块

### 1. 学习仪表盘

实时展示今日答题数、累计正确率、生成资源数、学习进度，以及答题趋势折线图和知识点掌握分布饼图，每日学习任务从学习路径自动同步。

### 2. AI 对话学习画像（9 维度）

通过与 DeepSeek AI 自然语言对话自动构建 9 维学生画像：

| 维度 | 说明 |
|------|------|
| 知识点掌握 | 各知识点的掌握等级与得分 |
| 整体水平 | 优秀/良好/中等/入门 |
| 诊断报告 | AI 生成的综合分析 |
| 学习节奏 | 时段偏好、专注时长、碎片化/整块化 |
| 认知偏好 | 视频/文字/图表偏好、先实例后理论等 |
| 学习目标 | 考研/就业/兴趣、每周投入时长 |
| 易错类型 | 错误类型、频率、原因分析 |
| 资源偏好 | 难度、内容长度、拓展接受度 |
| 反馈偏好 | 答案风格、反馈频率 |

### 3. 多智能体协同资源生成（6 类）

| 资源类型 | 格式 | 生成流程 |
|---------|------|---------|
| PPT 课件 | PPTX | Planner → Search → Python(python-pptx) → Review |
| 知识讲解文档 | DOCX | Planner → ContentAgent → Python(python-docx) → Review |
| 思维导图 | PNG | AI生成树结构 → Graphviz 渲染 |
| 练习题 | Markdown | Planner → Search → AI出题 → Review |
| 拓展阅读 | DOCX | Search(讯飞ONE SEARCH) → AI整理 → python-docx |
| 教学视频 | Coze 嵌入 | 第三方 Coze 平台 |

所有生成过程通过 **SSE 流式推送**实时进度，支持**审核→修正→再审**的迭代优化循环（最多 3 轮，评分低于 70 分自动触发重生成）。

### 4. 个性化学习路径

结合学生画像（学习节奏、认知偏好、学习目标）+ 答题数据（知识点掌握率），AI 自动规划：
- **分阶段学习路径**（3-5 个阶段，含状态标记）
- **7 天周计划**（每天 1-2 个任务，总时长 ≤ 3 小时）
- **推荐资源列表**（根据薄弱点推荐合适的资源类型）

### 5. 智能答疑辅导

- 支持**多模型切换**（通义千问 / 讯飞星火）
- 支持**图片上传**（截图提问，千问支持多模态理解）
- Markdown + 代码高亮渲染
- 对话历史保存与多会话管理

### 6. 学习效果评估

- **5 维度能力雷达图**：理论理解 / 编程实现 / 问题解决 / 知识应用 / 创新思维
- 综合得分 + 正确率 + 知识掌握率
- 周趋势折线图
- 知识点掌握进度条
- AI 生成个性化改进建议（3-4 条）

### 7. C 语言题库与在线判题

- 100 道 C 语言编程题，覆盖 12 个知识点
- 在线代码编辑 → GCC 编译 → 沙箱执行 → 结果比对
- 支持按难度/知识点筛选、随机组卷
- 答题历史追踪

### 8. 社区讨论

- Markdown 发帖/评论
- 点赞/点踩
- 文章百科（Markdown 富文本）

---

## API 接口概览

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/login` | POST | 用户登录 |
| `/api/profile` | GET | 获取学生画像 |
| `/api/profile/chat` | POST(SSE) | AI 对话构建画像 |
| `/api/agent/generate` | POST(SSE) | 多智能体资源生成 |
| `/api/agent/path` | POST(SSE) | 学习路径规划 |
| `/api/tutor/chat` | POST(SSE) | 智能答疑（支持图片） |
| `/api/tutor/model` | GET/POST | 切换模型 |
| `/api/evaluation` | GET | 学习效果评估 |
| `/api/quiz/questions` | GET | 题库列表 |
| `/api/quiz/judge` | POST | 在线判题 |
| `/api/quiz/history` | GET | 答题历史 |
| `/api/dashboard` | GET | 仪表盘数据 |
| `/api/mindmap/generate` | POST | 思维导图生成 |
| `/api/resource/*` | GET | 资源下载 |
| `/api/community/posts` | GET/POST | 社区帖子 |
| `/api/community/articles` | GET/POST | 百科文章 |

---

## 多智能体协同机制

系统核心协同能力基于三个层次：

### L1 — AgentEventBus 事件总线

```java
// AgentContext 提供便捷的事件发射接口
context.emit(AgentEvent.Type.SEARCH_INSUFFICIENT, "SearchAgent",
    Map.of("count", 2, "query", "指针"));

// Orchestrator 注册事件监听器协同响应
eventBus.on(AgentEvent.Type.SEARCH_INSUFFICIENT, ev -> {
    rawParams.put("searchModel", "deepseek");  // 自动切换搜索策略
});
```

支持 10 种事件类型：`SEARCH_COMPLETED`、`SEARCH_INSUFFICIENT`、`CONTENT_GENERATED`、`REVIEW_COMPLETED`、`REVIEW_LOW_SCORE`、`REVIEW_PASSED`、`PLAN_ADJUST_NEEDED`、`RETRY_REQUEST`、`WORKFLOW_DONE`、`WORKFLOW_ERROR`

### L2 — 迭代优化循环

```
ReviewAgent 审核内容
├── score ≥ 85 → 通过，结束
├── 70 ≤ score < 85 → 接受结果
└── score < 70 → 自动触发重生成
    └── 将问题列表注入 prompt → Python 重新生成 → 再次审核（最多 3 轮）
```

### L3 — 动态策略切换

- 搜索结果 < 3 条 → 自动切换为宽泛搜索策略
- 搜索结果与教学计划存在冲突 → 发射 `PLAN_ADJUST_NEEDED` 触发计划调整

---

## 开源协议声明

本项目使用了以下开源项目，在此致谢：

| 项目 | 协议 | 用途 |
|------|------|------|
| Spring Boot 3.2 | Apache 2.0 | 后端框架 |
| MyBatis 3.0 | Apache 2.0 | ORM |
| Apache POI 5.2 | Apache 2.0 | PPT 生成 |
| Graphviz Java | Apache 2.0 | 思维导图渲染 |
| Vue 3 | MIT | 前端框架 |
| Vite 5 | MIT | 构建工具 |
| ECharts 5 | Apache 2.0 | 数据图表 |
| FastAPI | MIT | Python Web 框架 |
| python-pptx | MIT | PPT 文件生成 |
| python-docx | MIT | DOCX 文件生成 |
| Pygments | BSD | 代码高亮 |

AI 模型与工具：

| 服务 | 供应商 | 用途 |
|------|--------|------|
| DeepSeek V4 Pro | DeepSeek | 课程规划、内容生成 |
| 讯飞星火 Spark X2 | 科大讯飞 | 智能答疑、备用生成 |
| 通义千问 Qwen-Plus | 阿里云 | 多模态答疑 |
| 豆包 Seedream 4.0 | 字节跳动 | 教育配图生成 |
| 讯飞 ONE SEARCH | 科大讯飞 | 教育资源搜索 |

---

## 开发说明

- AI Coding 辅助工具：Trae IDE
- 开发语言：Java 17 + JavaScript ES6 + Python 3.10
- 构建工具：Maven 3.x + Vite 5 + pip
- 代码规范：遵循各语言社区通用最佳实践

---

## License

本项目仅用于学术竞赛用途。
