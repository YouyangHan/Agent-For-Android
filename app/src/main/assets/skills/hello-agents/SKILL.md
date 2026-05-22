---
name: hello-agents
description: 智能体(Agent)开发完整指南 — 涵盖6种Agent范式、工具系统(ToolChain/AsyncExecutor)、记忆系统、上下文工程、通信协议(MCP/A2A/ANP)和强化学习训练。当你需要创建Agent、选择Agent架构、设计工具或理解Agent范式时使用。
---

# HelloAgents — 智能体开发完整指南

## 一、核心架构与基础概念

### 整体架构
```
用户输入 → Agent.run() → LLM思考 → 工具调用 → 响应
                ↓
         上下文构建(GSSC) ← 记忆系统 ← 工具结果
```

### 核心设计理念
除了Agent基类，**一切皆为工具(Tool)**。Memory、RAG、RL、MCP 都被统一抽象为工具，Agent 通过"调用工具"这一核心逻辑完成所有任务。没有额外的抽象层，学习曲线极低。

### Agent基类（所有Agent的共同祖先）
```python
from hello_agents.core.agent import Agent  # 抽象基类

# 所有Agent共享的接口：
agent.name           # str — Agent名称
agent.llm            # HelloAgentsLLM — LLM实例
agent.system_prompt  # str — 系统提示词
agent.config         # Config — 配置对象

# 所有Agent共享的方法：
agent.run(input_text)             # 核心方法：运行Agent
agent.add_message(Message(...))   # 手动添加消息到历史
agent.clear_history()             # 清空对话历史
agent.get_history()               # 获取对话历史列表
```

### Message消息系统
```python
from hello_agents import Message

msg = Message(content="你好", role="user")
# role 可选: "user" | "assistant" | "system" | "tool"
# 自动记录 timestamp 和 metadata

msg.to_dict()  # → {"role": "user", "content": "你好"}  适配OpenAI API格式
```

### Config配置
```python
from hello_agents import Config

config = Config(
    temperature=0.7,
    max_tokens=None,
    max_history_length=100,  # 历史消息上限
    debug=False
)
# 或从环境变量加载
config = Config.from_env()
```

### 异常体系
```python
from hello_agents.core.exceptions import (
    HelloAgentsException,  # 基础异常
    LLMException,          # LLM调用异常
    AgentException,        # Agent执行异常
    ConfigException,       # 配置异常
    ToolException,         # 工具异常
)
```

---

## 二、6种Agent范式（按复杂度排序）

### 1. SimpleAgent — 基础对话+可选工具
最简单也最常用的Agent。支持对话、流式输出，以及可选的工具调用。

```python
from hello_agents import SimpleAgent, HelloAgentsLLM

llm = HelloAgentsLLM()
agent = SimpleAgent(name="助手", llm=llm, system_prompt="你是一个有用的AI助手")

# 同步运行
response = agent.run("你好！")

# 流式输出
for chunk in agent.stream_run("介绍人工智能"):
    print(chunk, end="", flush=True)

# 也可添加工具（非必需）
agent.add_tool(SearchTool())          # 注册Tool对象
agent.remove_tool("search")           # 移除工具
agent.list_tools()                    # 列出所有工具
agent.has_tools()                     # 检查是否有工具
```

**工具调用机制**：SimpleAgent通过解析LLM输出中的`[TOOL_CALL:tool_name:params]`标记来调用工具，支持多轮迭代（默认最多3轮），且支持多种参数格式（json/key=value/简单文本）。

**适用场景**：简单问答、闲聊、入门级工具调用
**核心能力**：对话历史管理、流式输出、`[TOOL_CALL:xxx:yyy]`标记式工具调用

### 2. FunctionCallAgent — 原生函数调用（生产推荐）
基于OpenAI原生function calling/tool_use机制，JSON Schema参数定义，最可靠。

```python
from hello_agents import FunctionCallAgent

agent = FunctionCallAgent(
    name="函数助手", llm=llm, tool_registry=registry,
    max_tool_iterations=3,        # 最大工具调用轮次
    default_tool_choice="auto"    # "auto" | "none" | "required" | {"type":"function","function":{"name":"xxx"}}
)

response = agent.run("搜索最新AI新闻")
# 内部流程：构建tool schemas → 调用OpenAI原生tool_use → 解析tool_calls → 执行 → 返回
```

**与SimpleAgent的工具调用对比**：
- SimpleAgent：用正则解析`[TOOL_CALL:xxx:yyy]`文本标记，依赖LLM遵守格式
- FunctionCallAgent：使用OpenAI原生tools/tool_choice参数，模型直接输出结构化tool_calls，可靠性远高于文本解析

**适用场景**：需要可靠工具调用、生产环境、OpenAI兼容API
**核心能力**：原生function calling、JSON Schema参数验证、自动类型转换

### 3. ToolAwareSimpleAgent — 可监听工具的执行
SimpleAgent子类，增加工具调用监听回调，用于日志记录、调试追踪、性能分析。

```python
from hello_agents import ToolAwareSimpleAgent

def tool_listener(call_info: dict):
    # call_info 包含: agent_name, tool_name, raw_parameters, parsed_parameters, result
    logger.info(f"[{call_info['agent_name']}] 调用 {call_info['tool_name']}({call_info['parsed_parameters']})")
    metrics.record_tool_call(call_info['tool_name'], call_info['result'])

agent = ToolAwareSimpleAgent(
    name="调试助手", llm=llm, tool_registry=registry,
    tool_call_listener=tool_listener
)

# 还支持流式+工具调用
for chunk in agent.stream_run("搜索并总结AI新闻"):
    print(chunk, end="", flush=True)
```

**适用场景**：需要追踪工具调用链、监控Agent行为、调试工具调用参数
**核心能力**：监听回调、增强的参数解析（支持嵌套括号、字符串引号）、流式+工具调用

### 4. ReActAgent — 推理+行动循环
Thought(思考) → Action(行动) → Observation(观察) 循环，最经典的Agent范式。

```
用户问题 → Thought(分析) → Action(工具调用) → Observation(观察结果) → 循环...
                                                                    ↓
                                                            Finish(最终答案)
```

```python
from hello_agents import ReActAgent, ToolRegistry, search, calculate

registry = ToolRegistry()
registry.register_function("search", "网页搜索引擎，用于查询实时信息", search)
registry.register_function("calculate", "数学计算工具，支持表达式求值", calculate)

# 默认配置
agent = ReActAgent(name="研究助手", llm=llm, tool_registry=registry, max_steps=5)
response = agent.run("搜索GPT-4最新进展，计算相比GPT-3的参数量增长倍数")

# 自定义提示词模板
custom_prompt = """
你是专业研究助手。可用工具：{tools}
研究问题：{question}
已完成的研究：{history}

请按格式：
Thought: 分析需要什么信息
Action: tool_name[参数] 或 Finish[最终结论]
"""
agent = ReActAgent(name="研究助手", llm=llm, tool_registry=registry,
                   custom_prompt=custom_prompt, max_steps=5)
```

**关键参数**：`max_steps`(最大步数,默认5), `custom_prompt`(自定义提示词，占位符：`{tools}`, `{question}`, `{history}`)
**适用场景**：需要多步推理+外部信息获取的任务，适合透明推理过程

### 5. ReflectionAgent — 自我反思迭代优化
执行→反思→优化→循环。三个角色轮流：执行者 → 评审员 → 优化者。

```
初始执行 → 自我评估(反思) → 发现不足 → 改进优化 → 再评估 → 最终输出
                                                      ↓
                                               "无需改进" → 停止
```

```python
from hello_agents import ReflectionAgent

# 默认配置（通用任务）
agent = ReflectionAgent(name="反思助手", llm=llm, max_iterations=3)
result = agent.run("解释什么是递归算法，并给出例子")

# 自定义三阶段提示词
agent = ReflectionAgent(name="代码专家", llm=llm, max_iterations=2, custom_prompts={
    "initial": "你是一位资深程序员。请根据要求编写代码：\n{task}",
    "reflect": "请审查以下代码的质量（算法效率、可读性、错误处理）：\n{content}\n如果满意请回复'无需改进'。",
    "refine": "根据评审意见优化代码。原任务：{task}。反馈：{feedback}"
})
code = agent.run("编写高效的素数筛选算法")
```

**内置Memory**：ReflectionAgent内部有一个简单Memory类，记录每次执行和反思的轨迹（trajectory），供后续迭代参考。

**适用场景**：代码生成/审查、文档写作、分析报告等需要迭代优化的任务
**关键参数**：`max_iterations`(最大迭代次数), `custom_prompts`(含`initial`/`reflect`/`refine`三个键)

### 6. PlanAndSolveAgent — 先规划再执行
两阶段架构：Planner(规划器)分解问题 → Executor(执行器)逐步求解。

```
复杂问题 → Planner分解为Python列表 → Executor逐步执行 → 整合最终答案
           ["步骤1", "步骤2", ...]      每步有历史上下文
```

```python
from hello_agents import PlanAndSolveAgent

# 默认配置
agent = PlanAndSolveAgent(name="规划助手", llm=llm)
answer = agent.run("一家公司第一年营收100万，每年增长20%，3年总利润？")

# 自定义规划器和执行器提示词
agent = PlanAndSolveAgent(name="数学专家", llm=llm, custom_prompts={
    "planner": "你是数学问题分解专家，将问题分解为清晰的计算步骤：\n{question}\n请以Python列表格式输出计划。",
    "executor": "按计划执行计算。\n原始问题：{question}\n计划：{plan}\n已完成的步骤：{history}\n当前步骤：{current_step}\n只输出当前步骤的计算结果。"
})
```

**注意**：与ReActAgent不同，PlanAndSolveAgent不使用工具，而是用不同的系统提示分别做规划和执行，每个执行步骤都能看到之前的结果。

**适用场景**：复杂多步骤问题、数学应用题、项目规划、逻辑推理
**关键参数**：`custom_prompts`(含`planner`和`executor`两个键)

---

## 三、Agent选择速查表

| 场景 | 推荐Agent | 原因 |
|------|----------|------|
| 简单聊天/问答 | SimpleAgent | 最轻量，无额外开销 |
| 需要工具+可靠性 | FunctionCallAgent | 原生function calling，结构化tool_calls |
| 需要调试/监控工具链 | ToolAwareSimpleAgent | 有工具调用监听回调 |
| 搜索+计算+多步推理 | ReActAgent | 透明的Thought→Action→Observation循环 |
| 代码生成/写作/迭代优化 | ReflectionAgent | 执行→反思→改进三阶段，内置轨迹记忆 |
| 复杂多步骤/数学题/规划 | PlanAndSolveAgent | Planner分解+Executor逐步执行 |
| 生产环境 | FunctionCallAgent | 最标准化，兼容所有OpenAI兼容API |
| 快速原型 | SimpleAgent | 零配置即可开始 |

---

## 四、工具系统

### 两种注册方式对比

```python
from hello_agents import ToolRegistry

registry = ToolRegistry()

# 方式1：注册Tool对象（推荐，支持参数定义、类型转换、OpenAI Schema导出）
from hello_agents.tools.builtin import SearchTool, CalculatorTool
registry.register_tool(SearchTool())        # 自动提取参数定义和类型
registry.register_tool(CalculatorTool())

# 方式2：注册普通函数（简便，函数签名固定为 (input_text: str) -> str）
def my_tool(input_text: str) -> str:
    return f"处理结果: {input_text}"
registry.register_function("my_tool", "我的自定义工具描述", my_tool)
```

### Tool基类 — 自定义工具的标准方式

```python
from hello_agents.tools.base import Tool, ToolParameter, tool_action

# 方式A：继承Tool，手动定义参数
class MyTool(Tool):
    def __init__(self):
        super().__init__(name="my_tool", description="我的工具")

    def get_parameters(self):
        return [
            ToolParameter(name="query", type="string", description="查询关键词", required=True),
            ToolParameter(name="limit", type="integer", description="返回数量", required=False, default=10),
        ]

    def run(self, parameters: dict) -> str:
        query = parameters["query"]
        limit = parameters.get("limit", 10)
        return f"搜索'{query}'的结果，共{limit}条"

# 方式B：可展开工具（一个类生成多个子工具）
class MemoryTools(Tool):
    def __init__(self):
        super().__init__(name="memory", description="记忆工具集", expandable=True)

    @tool_action(name="memory_add", description="添加新记忆")
    def _add(self, content: str, importance: float = 0.5) -> str:
        """添加记忆到系统

        Args:
            content: 记忆内容
            importance: 重要性分数(0-1)
        """
        return f"已添加记忆: {content} (重要性: {importance})"

    @tool_action(name="memory_search", description="搜索记忆")
    def _search(self, query: str, limit: int = 5) -> str:
        """搜索相关记忆

        Args:
            query: 搜索关键词
            limit: 返回数量限制
        """
        return f"搜索'{query}'的结果"

# 使用可展开工具：注册MemoryTools时自动展开为memory_add和memory_search两个独立工具
registry.register_tool(MemoryTools())
```

### 工具链 — ToolChain & ToolChainManager
串联多个工具调用，前一步的输出作为下一步的输入。

```python
from hello_agents import ToolChain, ToolChainManager

chain = ToolChain("research_chain", "研究工具链")
chain.add_step("search", "AI发展趋势", step_id="step1")     # 第1步：搜索
chain.add_step("calculate", "$step1.result.length", step_id="step2")  # 第2步：用上一步结果
# 占位符：$step_id.result 引用前序步骤的输出

manager = ToolChainManager(registry)
manager.register_chain(chain)
result = manager.execute_chain("research_chain", "起始输入")
```

### 异步并行执行 — AsyncToolExecutor

```python
from hello_agents import AsyncToolExecutor
import asyncio

async def main():
    executor = AsyncToolExecutor(registry, max_workers=3)
    tasks = [
        {"tool_name": "calculate", "input_data": "10 + 5"},
        {"tool_name": "calculate", "input_data": "20 * 3"},
        {"tool_name": "calculate", "input_data": "100 / 4"},
    ]
    results = await executor.execute_tools_parallel(tasks)
    # 三个计算并行执行
    for r in results:
        print(f"{r['input_data']} = {r['result']} (status: {r['status']})")
    executor.close()

asyncio.run(main())
```

### 全局工具注册表（便捷引用）

```python
from hello_agents import global_registry
# 全局单例，跨模块共享工具
global_registry.register_function("search", "搜索", search)
```

### 工具调用流程（SimpleAgent内部机制）

1. Agent在system_prompt中注入工具列表和`[TOOL_CALL:tool_name:params]`格式说明
2. LLM判断需要工具时，输出包含`[TOOL_CALL:xxx:yyy]`标记的文本
3. Agent用正则解析出tool_name和parameters
4. 智能参数解析：支持JSON格式、`key=value`格式、简单文本
5. 根据工具定义的参数类型自动转换（string→number, boolean等）
6. 工具结果注入回对话，LLM基于结果生成最终回答
7. 最多迭代`max_tool_iterations`轮（默认3轮）

---

## 五、记忆系统

### 四种记忆类型对比

| 类型 | 类比 | 特点 | 用途 |
|------|------|------|------|
| WorkingMemory | 大脑的"工作台" | 容量10条，TTL 2h | 当前对话上下文 |
| EpisodicMemory | "日记本" | 具体事件，时间序列 | 用户行为记录、里程碑 |
| SemanticMemory | "百科全书" | 抽象知识，概念关联 | 用户画像、知识图谱 |
| PerceptualMemory | "相册+录音机" | 多模态(文本/图像/音频/视频) | 跨模态数据存储 |

### MemoryManager — 统一管理接口

```python
from hello_agents.memory.manager import MemoryManager
from hello_agents.memory.base import MemoryConfig

config = MemoryConfig(
    max_capacity=100,
    importance_threshold=0.1,
    decay_factor=0.95,
    working_memory_capacity=10,
    working_memory_tokens=2000,
    working_memory_ttl_minutes=120,
)
manager = MemoryManager(config=config, user_id="user_001",
                        enable_working=True, enable_episodic=True,
                        enable_semantic=True, enable_perceptual=False)

# 添加记忆（自动分类到合适的记忆类型）
manager.add_memory("用户张三，Python初学者", auto_classify=True)
manager.add_memory("用户昨天完成了第一个爬虫项目", memory_type="episodic", importance=0.8)

# 跨类型检索
memories = manager.retrieve_memories("Python", memory_types=["working", "semantic"], limit=10, min_importance=0.5)

# 记忆整合（重要的工作记忆提升为长期情景记忆）
manager.consolidate_memories(from_type="working", to_type="episodic", importance_threshold=0.7)

# 记忆遗忘
manager.forget_memories(strategy="importance_based", threshold=0.1, max_age_days=30)

# 统计与清理
manager.get_memory_stats()
manager.clear_all_memories()
```

### MemoryTool — 以工具形式暴露给Agent

```python
from hello_agents.tools import MemoryTool

memory = MemoryTool(user_id="user_001", memory_types=["working", "episodic", "semantic"])

# Agent通过以下action调用记忆：
memory.run({"action": "add", "content": "用户偏好通过实例学习", "memory_type": "semantic", "importance": 0.7})
memory.run({"action": "search", "query": "学习偏好", "limit": 5, "min_importance": 0.5})
memory.run({"action": "stats"})
memory.run({"action": "summary", "limit": 10})
memory.run({"action": "consolidate", "from_type": "working", "to_type": "episodic", "importance_threshold": 0.7})
memory.run({"action": "forget", "strategy": "importance_based", "threshold": 0.3})
memory.run({"action": "update", "memory_id": "xxx", "content": "新内容", "importance": 0.9})
memory.run({"action": "remove", "memory_id": "xxx"})
```

---

## 六、RAG知识检索

```python
from hello_agents.tools import RAGTool

rag = RAGTool(knowledge_base_path="./my_kb", embedding_model="local")

# 添加知识
rag.run({"action": "add_document", "file_path": "manual.pdf"})     # 支持PDF（增强段落重组）
rag.run({"action": "add_text", "text": "Python是...", "document_id": "py_intro"})
rag.run({"action": "add_text", "text": "机器学习是...", "namespace": "ml"})  # 命名空间隔离

# 检索
rag.run({"action": "search", "query": "Python基础", "limit": 5})                  # 简单搜索
rag.run({"action": "get_context", "query": "深度学习", "limit": 3})               # 获取上下文文本
rag.run({"action": "ask", "question": "什么是Python？", "include_citations": True})  # 搜索+LLM生成

# 管理
rag.run({"action": "stats"})
rag.run({"action": "update_document", "document_id": "py_intro", "text": "新内容"})
rag.run({"action": "remove_document", "document_id": "py_intro"})
rag.run({"action": "clear"})
```

**嵌入模型自动降级链**：
```
sentence-transformers (384维, 本地首选)
  → huggingface (备选)
    → tfidf (最终兜底, 纯Python无网络依赖)
```

---

## 七、上下文工程（GSSC流水线）

解决"上下文窗口有限，信息太多"的核心问题。

```
[Gather] 收集多源 → [Select] 筛选排序 → [Structure] 结构化 → [Compress] 预算压缩
  候选信息          相关性+多样性        分级模板          截断/摘要
```

```python
from hello_agents.context.builder import ContextBuilder, ContextConfig, ContextPacket

config = ContextConfig(
    max_tokens=8000,        # 总token预算
    reserve_ratio=0.15,     # 预留15%给LLM生成
    min_relevance=0.3,      # 低于此相关性直接丢弃
    enable_mmr=True,        # MMR多样性算法（避免信息冗余）
    mmr_lambda=0.7,         # 0=纯多样性, 1=纯相关性
    enable_compression=True # 超预算时自动截断
)

builder = ContextBuilder(memory_tool=memory, rag_tool=rag, config=config)
context = builder.build(
    user_query="用户的问题",
    conversation_history=[...],
    system_instructions="你是一个专业助手...",
    additional_packets=[ContextPacket(content="额外信息", metadata={"type": "custom"})]
)
```

**Gather阶段收集的4类信息**（按优先级排列）：
- P0 — 系统指令（强约束，固定保留）
- P1 — 记忆中的任务状态和关键结论
- P2 — RAG检索到的事实证据
- P3 — 最近10条对话历史（辅助材料）

**Structure阶段输出的模板结构**：
```
[Role & Policies]  ← 系统指令，角色定义
[Task]             ← 当前用户问题
[State]            ← 关键进展和未决问题
[Evidence]         ← 事实与引用（记忆+RAG+工具结果）
[Context]          ← 对话历史与背景
[Output]           ← 输出格式约束（结论/依据/风险/行动建议）
```

**Compress阶段**：按行截断，优先保留前面的结构化段落。

---

## 八、通信协议

### 协议全景对比

| 特性 | MCP | A2A | ANP |
|------|-----|-----|-----|
| 主要用途 | 工具/资源调用 | Agent间协作 | 网络管理 |
| 通信模式 | Client-Server | Peer-to-Peer | 拓扑网络 |
| 规模 | 单工具集成 | 中小型团队 | 大规模分布式 |
| 标准化 | 高(Anthropic官方) | 中(社区标准) | 低(概念性) |
| 实现 | MCPTool封装 | A2AServer/A2ATool | ANPDiscovery/ANPNetwork |

### MCP — 连接外部工具和数据源

```python
from hello_agents.tools.builtin.protocol_tools import MCPTool

# 连接MCP服务器
weather_tool = MCPTool(server_command=["python", "weather_server.py"], name="天气工具")
filesystem_tool = MCPTool(
    server_command=["npx", "@modelcontextprotocol/server-filesystem", "."],
    name="文件系统工具"
)

# 直接添加到Agent
agent.add_tool(weather_tool)
agent.add_tool(filesystem_tool)
# Agent即可调用get_weather、list_directory等MCP工具

# 也可手动连接（支持多种传输方式）
from hello_agents.protocols.mcp.client import MCPClient
client = MCPClient("python weather_server.py")              # Stdio
client = MCPClient("http://localhost:8000")                 # HTTP
client = MCPClient("http://localhost:8000", transport="sse") # SSE实时
```

**传输方式选择**：Stdio(本地开发) → HTTP(远程生产) → SSE(实时推送场景)

### A2A — 多智能体协作通信

```
研究员Agent ──→ 撰写员Agent ──→ 编辑Agent ──→ 最终输出
    ↓ 技能共享      ↓ 调用翻译技能    ↓ 质量审查
```

```python
from hello_agents.protocols.a2a.implementation import A2AServer

# 创建A2A智能体
calculator = A2AServer(name="calculator-agent", description="数学计算智能体")

@calculator.skill("add")
def add_numbers(query: str) -> str:
    """加法计算技能"""
    # ...解析query并计算
    return "计算结果: 15"

@calculator.skill("multiply")
def multiply_numbers(query: str) -> str:
    """乘法计算技能"""
    return "计算结果: 42"

# 技能可被其他Agent发现和调用
```

### ANP — 大规模智能体网络管理

```python
from hello_agents.protocols.anp.implementation import ANPDiscovery, ServiceInfo

discovery = ANPDiscovery()

# 注册服务
discovery.register_service(ServiceInfo(
    service_id="weather-service", service_type="weather",
    endpoint="http://localhost:8001",
    capabilities=["weather_query", "forecast"]
))

# 服务发现
services = discovery.find_services_by_type("weather")
services = discovery.find_services_by_capability("translate")
```

**核心能力**：服务注册/发现、网络拓扑管理、负载均衡（轮询/加权/最少连接/响应时间）、消息路由、健康检查

---

## 九、LLM配置

### HelloAgentsLLM — 统一LLM接口

```python
from hello_agents import HelloAgentsLLM

# 方式1：零配置自动检测（推荐）
# 从.env文件读取 LLM_MODEL_ID / LLM_API_KEY / LLM_BASE_URL
llm = HelloAgentsLLM()
print(llm.provider)  # 查看自动检测到的provider

# 方式2：手动指定
llm = HelloAgentsLLM(
    model="gpt-4",
    api_key="sk-xxx",
    base_url="https://api.openai.com/v1",
    provider="openai",       # 可选，不传则自动检测
    temperature=0.7,
    max_tokens=2000,
    timeout=60
)

# 两种调用方式
response = llm.invoke(messages)              # 非流式，返回完整字符串
for chunk in llm.think(messages):            # 流式，逐个yield文本块
    print(chunk, end="", flush=True)
```

### .env配置
```env
LLM_MODEL_ID=your-model-name
LLM_API_KEY=your-api-key
LLM_BASE_URL=your-api-base-url
LLM_TIMEOUT=60
```

### Provider自动检测逻辑（优先级从高到低）
1. 检查特定Provider的环境变量（`OPENAI_API_KEY`, `DEEPSEEK_API_KEY`, `DASHSCOPE_API_KEY`等）
2. 根据API Key格式判断（`ms-`开头→ModelScope, `sk-`长串→可能是OpenAI/DeepSeek/Kimi）
3. 根据Base URL域名判断（`api.deepseek.com` → DeepSeek, `dashscope.aliyuncs.com` → 通义千问）
4. 默认兜底为`auto`，使用通用配置

### 支持的Provider及默认模型

| Provider | 自动检测 | 默认模型 | Base URL特征 |
|----------|---------|---------|-------------|
| OpenAI | ✅ | gpt-3.5-turbo | api.openai.com |
| DeepSeek | ✅ | deepseek-chat | api.deepseek.com |
| 通义千问 | ✅ | qwen-plus | dashscope.aliyuncs.com |
| ModelScope | ✅ | Qwen/Qwen2.5-72B-Instruct | api-inference.modelscope.cn |
| Kimi(月之暗面) | ✅ | moonshot-v1-8k | api.moonshot.cn |
| 智谱GLM | ✅ | glm-4 | open.bigmodel.cn |
| Ollama | ✅ | llama3.2 | localhost:11434 |
| vLLM | ✅ | meta-llama/Llama-2-7b | localhost:8000 |
| 本地部署 | ✅ | local-model | localhost:8080/7860 |

---

## 十、强化学习训练（Agentic RL）

完整训练流程：**SFT（监督微调） → GRPO（强化学习） → 评估**

```python
from hello_agents.tools import RLTrainingTool

tool = RLTrainingTool()

# 1. SFT训练（学习基础推理格式）
tool.run({
    "action": "train", "algorithm": "sft",
    "model_name": "Qwen/Qwen3-0.6B",
    "output_dir": "./output/sft",
    "max_samples": 100, "num_epochs": 3, "batch_size": 4,
    "use_lora": True,        # LoRA参数高效微调
    "lora_r": 16, "lora_alpha": 32
})

# 2. GRPO训练（强化学习优化推理能力）
tool.run({
    "action": "train", "algorithm": "grpo",
    "model_name": "./output/sft",  # 在SFT模型基础上继续训练
    "output_dir": "./output/grpo",
    "max_samples": 50, "num_epochs": 1, "batch_size": 2,
    "use_lora": True
})

# 3. 评估模型
result = tool.run({
    "action": "evaluate",
    "model_path": "./output/grpo",
    "max_samples": 50, "use_lora": True
})

# 加载和查看数据集
tool.run({"action": "load_dataset", "format_type": "sft", "max_samples": 5})  # 对话格式
tool.run({"action": "load_dataset", "format_type": "rl", "max_samples": 5})   # 强化学习格式

# 创建自定义奖励函数
tool.run({"action": "create_reward", "reward_type": "accuracy"})               # 准确率奖励
tool.run({"action": "create_reward", "reward_type": "length_penalty", "penalty_weight": 0.01})  # 长度惩罚
tool.run({"action": "create_reward", "reward_type": "step", "step_bonus": 0.1})  # 步骤奖励
```

**LoRA配置速查**：
- 快速实验：r=8, alpha=16, batch_size=8
- 标准配置：r=16, alpha=32, batch_size=4
- 高质量：r=32, alpha=64, batch_size=2

---

## 十一、最佳实践

### 1. 系统提示词设计公式
```
好的系统提示词 = 角色定义 + 行为约束 + 工具使用指南 + 输出格式
```

**示例**：
```python
system_prompt = """你是一个专业的Python编程助手。

行为准则：
1. 提供清晰、可直接运行的代码
2. 包含必要的注释
3. 考虑边界情况和错误处理

工具使用：
- 当需要搜索实时信息时，使用 [TOOL_CALL:search:关键词]
- 当需要数学计算时，使用 [TOOL_CALL:calculate:表达式]

回答格式：
1. 分析问题
2. 给出代码
3. 解释关键点
"""
```

### 2. 温度参数选择
- `0.1-0.3`：代码生成、数学计算、事实性问答
- `0.5-0.7`：通用对话、信息检索（**默认值**）
- `0.8-1.0`：创意写作、头脑风暴

### 3. 工具设计原则
- **单一职责**：一个工具只做一件事，名字清晰描述功能
- **描述即文档**：工具描述要让LLM准确判断何时调用
- **参数明确**：类型、是否必需、默认值必须清楚
- **错误友好**：工具异常返回有意义的错误信息，不抛异常

### 4. 记忆系统策略
- **工作记忆**：当前会话临时信息，2小时自动过期
- **情景记忆**：用户交互事件，按时间线存储
- **语义记忆**：领域知识、用户画像，跨会话持久化
- **定期整合**：`consolidate(importance_threshold=0.7)` 将重要短期记忆提升为长期
- **定期遗忘**：`forget(strategy="importance_based", threshold=0.1)` 清理低价值记忆

### 5. 上下文预算分配
| 区域 | 占比 | 内容 |
|------|------|------|
| 系统指令 | ~15% | 角色定义、行为约束 |
| 证据材料 | ~40% | 记忆结果、RAG检索、工具输出 |
| 对话历史 | ~30% | 最近N条对话 |
| 生成余量 | ~15% | 留给LLM生成回答 |

### 6. 错误处理
```python
from hello_agents.core.exceptions import HelloAgentsException

try:
    response = agent.run("你的问题")
except HelloAgentsException as e:
    print(f"Agent执行失败: {e}")
```

---

## 十二、快速上手模板

### 模板1：带记忆+知识的全能助手
```python
from hello_agents import SimpleAgent, HelloAgentsLLM, ToolRegistry
from hello_agents.tools import MemoryTool, RAGTool

llm = HelloAgentsLLM()
registry = ToolRegistry()
registry.register_tool(MemoryTool(user_id="user_001"))
registry.register_tool(RAGTool(knowledge_base_path="./kb"))

agent = SimpleAgent(
    name="全能助手", llm=llm, tool_registry=registry,
    system_prompt="你是有记忆和知识检索能力的助手。主动记录用户信息，遇到专业问题先搜索知识库再回答。"
)
agent.run("你好！")
```

### 模板2：代码审查专家
```python
from hello_agents import ReflectionAgent, HelloAgentsLLM

llm = HelloAgentsLLM(temperature=0.3)  # 代码生成用低温度
agent = ReflectionAgent(name="代码专家", llm=llm, max_iterations=2, custom_prompts={
    "initial": "你是资深程序员，请编写代码：{task}",
    "reflect": "审查以下代码的质量（算法效率、可读性、错误处理）。如果满意回复'无需改进'。\n{content}",
    "refine": "根据评审意见优化代码。\n原始任务：{task}\n评审意见：{feedback}"
})
code = agent.run("实现线程安全的单例模式")
```

### 模板3：研究+计算求解器
```python
from hello_agents import ReActAgent, ToolRegistry, search, calculate, HelloAgentsLLM

llm = HelloAgentsLLM()
registry = ToolRegistry()
registry.register_function("search", "网页搜索引擎，查询实时信息", search)
registry.register_function("calculate", "数学表达式计算器，支持+ - * / sqrt sin等", calculate)

agent = ReActAgent(name="求解器", llm=llm, tool_registry=registry, max_steps=5)
result = agent.run("搜索最新中国GDP数据，计算近5年年均增长率")
```

### 模板4：复杂数学题分解求解
```python
from hello_agents import PlanAndSolveAgent, HelloAgentsLLM

llm = HelloAgentsLLM(temperature=0.2)  # 数学计算用极低温度
agent = PlanAndSolveAgent(name="数学专家", llm=llm)
answer = agent.run("小明买3个苹果和5个橙子花了35元，小红买2个苹果和8个橙子花了40元，求每个苹果和橙子的价格。")
```

---

## 十三、故障排查

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| Agent不调用工具 | system_prompt未描述工具用法 | 在system_prompt中明确写出工具名称、用途和调用格式 |
| 工具调用参数格式错误 | SimpleAgent依赖正则解析文本标记 | 改用FunctionCallAgent（原生tool_calls,结构化参数） |
| ReAct陷入死循环 | max_steps太小 或 工具返回信息不足以得出结论 | 1)增大max_steps 2)优化工具返回内容 3)在提示词中引导及时Finish |
| Reflection不停止 | reflect模板缺少停止条件判断 | 在reflect模板中写明"如果满意请回答'无需改进'" |
| 上下文截断丢失关键信息 | 默认截断从尾部开始 | 调整ContextConfig的reserve_ratio，或手动管理历史长度 |
| 记忆检索不到结果 | importance阈值过高 或 关键词不匹配 | 降低min_importance阈值，使用更多样化的搜索词 |
| LLM响应超时 | 网络问题 或 模型推理慢 | 增大LLM_TIMEOUT环境变量，或切换到更快的模型 |
| Provider检测错误 | 同时设置多个Provider的环境变量 | 检查.env中是否有冲突的环境变量，或手动指定provider参数 |
| 嵌入模型下载失败 | 首次运行sentence-transformers需下载模型 | 等待自动降级到tfidf，或手动预先下载模型 |
| PDF处理内容混乱 | 默认PDF解析按页处理破坏段落 | RAGTool内置了增强PDF处理（智能段落重组），升级到最新版 |
