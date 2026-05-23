# Agent Yang

> 🤖 由 C++ 工程师借助 AI Agent 从零构建的 Android AI 助手 — 多模型、可扩展、Skill 驱动

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.06-green.svg)](https://developer.android.com/compose)
[![API](https://img.shields.io/badge/API-26%2B-orange.svg)](https://developer.android.com/about/versions/oreo)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

Agent Yang 是一个运行在 Android 手机上的 AI Agent 应用。支持多模型 API 配置、流式对话、可插拔 Skill 系统、性格切换、手机工具调用。

**本项目由一名 C++ 工程师在 AI Agent（Claude Code）辅助下独立开发完成，零 Android 开发经验起步。**

---

## 快速开始

### 系统要求
- Android 8.0 (API 26) 或更高版本
- 至少一个 LLM API Key（支持 OpenAI 兼容 / Anthropic 兼容接口）

### 安装
```bash
# 编译 APK
./gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 配置模型
打开 App → 设置 → 模型设置 → 点击 + 添加模型配置：

| 字段 | 说明 | 示例 |
|------|------|------|
| 模型名称 | 自定义显示名称 | DeepSeek V4 |
| 模型 ID | API 模型标识 | deepseek-v4-pro |
| API Key | 你的密钥 | sk-xxx |
| Base URL | API 端点 | https://api.deepseek.com |
| API 类型 | OpenAI / Anthropic | OpenAI |

---

## 核心功能

### 1. 多模型支持
- 兼容 OpenAI Chat Completions API
- 兼容 Anthropic Messages API
- 支持 DeepSeek、Kimi、OpenRouter 等任意兼容接口
- 模型 ID 可选填，自动适配

### 2. Skill 系统
Skill 是扩展 AI 能力的插件，采用文件夹 + SKILL.md 格式：

```
agent_skills/
└── my-expert/
    └── SKILL.md     ← frontmatter + markdown 正文
```

- **内置 Skills**：10 个开箱即用的 Skill（自动权限、头脑风暴、计划执行等）
- **用户 Skills**：通过对话 `[TOOL:create_skill:...]` 创建，或从文件夹导入
- **性格 Skills**：标记为性格的 Skill 可在对话中切换，改变 AI 回复风格

### 3. 性格系统
- 内置「深情男」「魅力女」两个性格 Skill
- 用户可导入/创建自定义性格
- 聊天输入框上方一键切换，切换时自动清空上下文
- Default = 不使用任何性格

### 4. 手机工具
AI 可通过 `[TOOL:name:{...}]` 格式调用手机功能：

| 工具 | 功能 |
|------|------|
| `add_event` | 添加日历日程 |
| `launch_app` | 启动其他应用 |
| `create_skill` | 对话中创建新 Skill |

### 5. 对话功能
- 流式 SSE 响应，Markdown 渲染
- 支持流式中发送新消息（自动取消上一个）
- 清空上下文、滚动置底悬浮按钮
- 模型/性格切换一行操作

---

## 项目结构

```
AgentForAndroid/
├── app/
│   ├── build.gradle.kts                 # 依赖配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/skills/               # 内置 Skills (10个)
│       ├── res/                         # 资源文件
│       └── java/com/agentforandroid/
│           ├── AgentApp.kt              # Application 入口
│           ├── MainActivity.kt          # 单 Activity + 导航
│           ├── model/                   # 数据模型
│           ├── data/
│           │   ├── local/               # Room 数据库
│           │   └── remote/              # OkHttp SSE 客户端
│           ├── repository/              # 数据仓库层
│           ├── viewmodel/               # MVVM ViewModel
│           ├── ui/
│           │   ├── theme/               # Material 3 主题
│           │   ├── screens/             # 页面 (Chat/Settings/Skills)
│           │   └── components/          # UI 组件
│           ├── tool/                    # 手机工具执行器
│           └── skill/                   # Skill 解析与加载
├── docs/                                # 设计文档和计划
├── resource/                            # App 图标等
└── README.md
```

---

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Kotlin | 2.0 |
| UI | Jetpack Compose + Material 3 | BOM 2024.06 |
| 架构 | MVVM + Repository | — |
| 数据库 | Room | 2.6.1 |
| 网络 | OkHttp (SSE 流式) | 4.12 |
| JSON | Moshi | 1.15 |
| Markdown | Markwon | 4.6 |
| 异步 | Kotlin Coroutines + Flow | 1.8 |

---

## 开发背景

本项目由一名 **C++ 工程师** 借助 **Claude Code (AI Agent)** 从零开发完成。

- **开发者**：HanYouyang
- **开发方式**：通过自然语言与 AI Agent 协作，完成需求分析 → 架构设计 → 代码实现 → 测试验证全流程
- **开发周期**：2026 年 5 月
- **起点**：零 Android/Kotlin 基础
- **当前版本**：V1.0.0.0

> "作为一个 C++ 工程师，我从未写过 Android 应用。但在 AI Agent 的帮助下，从 Gradle 配置到 Compose UI，从 Room 数据库到 OkHttp 流式请求，全部通过对话完成。这不是 AI 取代程序员，而是 AI 让程序员超越了语言和平台的边界。"

---

## 参考与致谢

| 项目 | 说明 |
|------|------|
| [HelloAgents](https://github.com/datawhalechina/hello-agents) | Datawhale 开源的多智能体框架，提供了 Agent 范式、Skill 系统的核心设计理念 |
| [童锦程.skill](https://github.com/jjyaoao/skill-tongjincheng) | 深情男性格 Skill，童锦程"深情祖师爷"视角 |
| [性感风情御姐.skill](https://github.com/jjyaoao/charm-mature-skill) | 魅力女性格 Skill，优雅成熟女性魅力 |
| [agent-skill-creator](https://github.com/jjyaoao/agent-skill-creator) | Skill 创建与导入工具链参考 |

---

## 许可证

MIT License © 2026 HanYouyang
