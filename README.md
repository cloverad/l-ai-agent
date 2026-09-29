# L-AI Agent · AI 旅行管家

基于 **Spring Boot 3 + Spring AI** 的智能旅行助手：多轮对话、知识库 RAG、工具调用、MCP 图片搜索，以及可自主规划的 **TravelManus** 智能体。本仓库为前后端一体（Monorepo），Vue 3 前端支持 SSE 流式对话、图片卡片预览与 PDF 下载。

---

## 功能亮点

| 能力 | 说明 |
|------|------|
| 旅行管家对话 | 结构化行程建议，相对时间感知，输出风格偏「即时管家」而非长 FAQ |
| RAG 知识库 | Markdown 文档入库 **PgVector**，检索增强回答 |
| 工具调用 | 联网搜索、网页抓取、图片搜索、文件/PDF、终端等 |
| 图片展示 | Pexels 搜图 → `media_gallery` → 前端卡片；失败自动降级 WebSearch |
| TravelManus | ReAct 多步智能体：通俗 Step 文案、思考过程可折叠、PDF 一键下载 |
| MCP 扩展 | 可选 stdio 拉起 `image-search-mcp-server` 提供图片工具 |

---

## 技术栈

**后端**

- Java 21 · Spring Boot 3.4 · Spring AI 1.0
- 模型：阿里云百炼 DashScope（OpenAI 兼容模式，默认 `qwen3.8-max`）
- 向量库：PostgreSQL + pgvector
- 文档：Knife4j / OpenAPI

**前端（`ai-frontend/`）**

- Vue 3 · Vite · TypeScript · Vue Router
- SSE（EventSource）流式输出；开发代理 `/api` → `http://localhost:8123`

---

## 仓库结构

```text
l-ai-agent/
├── l-ai-agent-server/          # 主服务（端口 8123，context-path /api）
├── image-search-mcp-server/    # 图片搜索 MCP 子服务（可选）
├── ai-frontend/                # Vue 3 前端（开发端口 5173）
├── .env.example                # 后端环境变量模板（勿提交真实 .env）
├── README.md
└── pom.xml                     # Maven 多模块父工程
```

---

## 环境要求

- JDK **21+**
- Maven **3.9+**（或使用仓库自带 `mvnw`）
- PostgreSQL **15+**，并安装 **[pgvector](https://github.com/pgvector/pgvector)** 扩展
- Node.js **20+**（运行前端）
- 阿里云百炼 API Key（环境变量名：`DASHSCOP_API_KEY`）

可选：

- `PEXELS_API_KEY`：目的地图片搜索
- `SEARCH_API_KEY`：SearchAPI（未配置时联网搜索会回退 DuckDuckGo）
- `APP_MCP_ENABLED=true`：启用 MCP 图片服务

---

## 快速开始

### 1. 准备数据库

创建数据库（示例名 `travel_agent`），并启用扩展：

```sql
CREATE DATABASE travel_agent;
\c travel_agent
CREATE EXTENSION IF NOT EXISTS vector;
```

默认连接：`localhost:5432`，用户/密码可通过环境变量覆盖（见下）。

### 2. 配置密钥

在仓库根目录复制环境变量模板：

```bash
cp .env.example .env
```

编辑 `.env`（**不要提交到 Git**）：

```env
POSTGRES_USER=root
POSTGRES_PASSWORD=root
DASHSCOP_API_KEY=你的百炼密钥
PEXELS_API_KEY=你的Pexels密钥
SEARCH_API_KEY=
APP_MCP_ENABLED=false
```

> 启动时会自动加载根目录 `.env`，并尝试回退读取本机用户环境变量。

### 3. 启动后端

在 **`l-ai-agent` 仓库根目录**执行（MCP 相对路径依赖此工作目录）：

```bash
# 可选：先打包 MCP 子模块
mvn -pl image-search-mcp-server -am package -DskipTests

# 编译并运行主服务
mvn -pl l-ai-agent-server -am spring-boot:run
```

服务地址：

- API：`http://localhost:8123/api`
- Swagger / Knife4j：`http://localhost:8123/api/swagger-ui.html`
- 健康检查：`http://localhost:8123/api/health`

### 4. 启动前端

另开终端，仍在本仓库内：

```bash
cd ai-frontend
npm install
npm run dev
```

浏览器打开 Vite 提示的地址（默认 `http://localhost:5173`）。  
开发环境下请求 `/api/*` 由 Vite 代理到后端 `8123`，一般无需改 `ai-frontend/.env.development`。

---

## 主要接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/travel_app/chat/sse` | 旅行管家 SSE（含工具调用） |
| GET | `/api/ai/travel_app/chat/sync` | 旅行管家同步对话 |
| GET | `/api/ai/travel_app/chat/rag` | 仅 RAG 问答 |
| GET | `/api/ai/manus/chat` | TravelManus 智能体 SSE |
| GET | `/api/files/download?name=` | 下载工具生成的 PDF 等文件 |

查询参数常见字段：`message`、`chatId`（管家对话用于多轮记忆）。

---

## 使用提示

1. **配图**：管家会调用 `searchImages`，前端将 `media_gallery` 渲染为图片卡片；无需本地下载即可预览。
2. **TravelManus**：界面展示「开始执行 → Step N」通俗步骤，思考完成后可折叠；生成 PDF 后出现下载卡片。
3. **MCP**：设置 `APP_MCP_ENABLED=true` 前，请先 `package` 图片 MCP 模块；`mcp-servers.json` 使用相对路径  
   `image-search-mcp-server/target/....jar`。
4. **知识库**：首次空库启动时，会将 `l-ai-agent-server/src/main/resources/document/` 下 Markdown 写入 PgVector。

---

## 配置说明

| 变量 / 配置 | 含义 |
|-------------|------|
| `DASHSCOP_API_KEY` | 百炼 API Key（注意拼写为 DASHSCOP） |
| `PEXELS_API_KEY` | Pexels 图片 API |
| `SEARCH_API_KEY` | SearchAPI.io（可选） |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | 数据库账号 |
| `APP_MCP_ENABLED` | 是否启用 MCP Client |
| `server.port` | 默认 `8123` |
| `app.chat-memory.dir` | 对话记忆目录（默认 `./tmp/chat-memory`） |
| `app.file-tool.dir` | 工具生成文件目录（默认 `./tmp/files`） |
| `VITE_API_BASE` | 前端 API 前缀（开发默认 `/api`） |

模型与 Embedding 见 `application-dashscope.yml`（可通过 profile 调整）。

---

## 安全须知

- 仓库已忽略 `.env`、`tmp/`、`target/`、`node_modules/`、密钥与本地媒体等，**切勿**强制添加真实密钥。
- 对外开源前请确认：未提交 API Key、聊天记录、生成的 PDF/图片。
- 默认数据库口令仅适合本地演示，生产环境请更换。

---

## 开发与测试

```bash
# 后端编译
mvn -pl l-ai-agent-server,image-search-mcp-server -am -DskipTests compile

# 后端指定测试（需已配置密钥的环境）
mvn -pl l-ai-agent-server -Dtest=ImageSearchToolIT test

# 前端类型检查 / 构建
cd ai-frontend
npm run build
```

---

## 路线图（可选扩展）

- [ ] 对话记忆持久化到数据库
- [ ] 知识库命中不足时与联网搜索的显式路由策略
- [ ] 前端生产构建产物由 Nginx 或后端静态资源托管

---

## License

本项目仅供学习与演示。若对外分发，请自行补充开源协议并遵守 DashScope、Pexels 等第三方服务条款。
