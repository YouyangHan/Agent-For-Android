---
name: auto-permit
description: 配置所有命令默认自动执行，不再逐一询问权限
---

# 自动执行权限配置

此 skill 用于将 Claude Code 配置为所有命令默认自动执行，无需逐一手动确认。

## 操作步骤

### 1. 先读取当前权限配置

读取项目级 `.claude/settings.json` 和用户级 `~/.claude/settings.json`，了解已有权限。

### 2. 添加 Bash 权限白名单

使用 `update-config` skill 或直接编辑 settings.json，添加以下权限：

**项目级 (`.claude/settings.json`)**：
```json
{
  "permissions": {
    "allow": [
      "Bash(curl:*)",
      "Bash(git:*)",
      "Bash(npm:*)",
      "Bash(npx:*)",
      "Bash(yarn:*)",
      "Bash(pnpm:*)",
      "Bash(pip:*)",
      "Bash(python:*)",
      "Bash(node:*)",
      "Bash(ls:*)",
      "Bash(cat:*)",
      "Bash(echo:*)",
      "Bash(mkdir:*)",
      "Bash(rm:*)",
      "Bash(cp:*)",
      "Bash(mv:*)",
      "Bash(chmod:*)",
      "Bash(cd:*)",
      "Bash(pwd:*)",
      "Bash(du:*)",
      "Bash(df:*)",
      "Bash(find:*)",
      "Bash(ps:*)",
      "Bash(kill:*)",
      "Bash(taskkill:*)",
      "Bash(tasklist:*)",
      "Bash(wget:*)",
      "Bash(tar:*)",
      "Bash(unzip:*)",
      "Bash(zip:*)",
      "Bash(docker:*)",
      "Bash(docker-compose:*)",
      "Bash(gh:*)",
      "Bash(cargo:*)",
      "Bash(go:*)",
      "Bash(java:*)",
      "Bash(mvn:*)",
      "Bash(gradle:*)",
      "Bash(adb:*)",
      "Bash(winget:*)",
      "Bash(choco:*)",
      "Bash(scoop:*)",
      "Bash(pwsh:*)",
      "Bash(powershell:*)",
      "Bash(where:*)",
      "Bash(which:*)",
      "Bash(whoami:*)",
      "Bash(netstat:*)",
      "Bash(ipconfig:*)",
      "Bash(ping:*)",
      "Bash(nslookup:*)",
      "Bash(set:*)",
      "Bash(export:*)",
      "Bash(source:*)",
      "Bash(.\\:*)",
      "Bash(./:*)",
      "Bash(claude:*)",
      "Bash(code:*)",
      "Bash(code-insiders:*)",
      "Bash(dir:*)",
      "Bash(type:*)",
      "Bash(copy:*)",
      "Bash(xcopy:*)",
      "Bash(robocopy:*)",
      "Bash(net:*)",
      "Bash(sc:*)",
      "Bash(reg:*)",
      "Bash(msbuild:*)",
      "Bash(dotnet:*)",
      "Bash(cmake:*)",
      "Bash(gcc:*)",
      "Bash(g++:*)",
      "Bash(make:*)",
      "Bash(bash:*)",
      "Bash(sh:*)",
      "Bash(tsc:*)",
      "Bash(eslint:*)",
      "Bash(prettier:*)",
      "Bash(jest:*)",
      "Bash(vitest:*)",
      "Bash(pytest:*)",
      "Bash(cargo:*)",
      "Bash(rustc:*)",
      "Bash(deno:*)",
      "Bash(bun:*)"
    ]
  }
}
```

**用户级 (`~/.claude/settings.json`)** — 同上结构，让所有项目都生效。

### 3. 如果需要更宽松的权限

可以使用通配符允许所有 Bash 命令：
```json
{
  "permissions": {
    "allow": [
      "Bash(*:*)"
    ]
  }
}
```

### 4. 验证配置

配置完成后，运行任意命令验证是否不再弹出权限确认。

## 注意事项

- 项目级配置只对当前项目生效
- 用户级配置对所有项目生效
- 自动允许所有命令存在安全风险，请确保你信任当前工作环境
- 可随时通过删除 allow 条目来恢复权限确认
