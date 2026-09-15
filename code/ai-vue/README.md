# 小光心理助手前端

基于 Vue 3、Vite 和 Element Plus 的前端项目。开发环境默认通过 Vite 将 `/api` 和 `/files` 代理到本机后端 `http://127.0.0.1:8080`。

## 本地运行

先确保后端已经监听 8080，再启动前端：

```powershell
cd E:\projects\心理健康助手\code\ai-vue
npm install
npm run dev
```

默认地址为 `http://localhost:5173`。如果端口被占用，Vite 可能自动使用其他端口，以终端输出为准。

## 构建与本地预览

对外临时展示时，优先使用构建后的预览服务，不要直接暴露 Vite 开发服务器：

```powershell
cd E:\projects\心理健康助手\code\ai-vue
npm run build
npm run preview -- --host 127.0.0.1 --port 4173 --strictPort
```

本地预览地址为 `http://127.0.0.1:4173`。

## Cloudflare 临时隧道

### 完整应用（前端和 API）

先启动后端 8080 和前端预览 4173，然后在另一个 PowerShell 窗口执行：

```powershell
& 'C:\Program Files (x86)\cloudflared\cloudflared.exe' tunnel --url http://127.0.0.1:4173 --no-autoupdate
```

终端会输出一个 `https://*.trycloudflare.com` 临时地址。该模式会通过前端代理公开登录、聊天、管理端等 API，只应用于受控的临时测试。

### 只公开静态前端

如果不希望公网访问后端，先以隔离模式启动预览：

```powershell
$env:PUBLIC_FRONTEND_ONLY = 'true'
npm run preview -- --host 127.0.0.1 --port 4173 --strictPort
```

再启动上面的 Cloudflare 隧道。隔离模式会对公网请求的 `/api/*` 和 `/files/*` 返回 403，因此无法登录或读取动态数据。

恢复完整代理前，可以关闭当前预览进程，然后清除本终端的临时变量：

```powershell
Remove-Item Env:PUBLIC_FRONTEND_ONLY -ErrorAction SilentlyContinue
```

## 如何关闭 Cloudflare 内网穿透

如果运行 `cloudflared` 的终端还开着，在该终端按 `Ctrl+C` 即可关闭。隧道关闭后，`trycloudflare.com` 地址会失效。

如果原终端已经找不到，先查询进程，再关闭明确的进程 ID：

```powershell
Get-Process cloudflared | Select-Object Id, ProcessName, StartTime
Stop-Process -Id <上一步看到的PID>
```

例如 PID 为 `12345` 时：

```powershell
Stop-Process -Id 12345
```

确认是否已经关闭：

```powershell
Get-Process cloudflared -ErrorAction SilentlyContinue
```

没有输出即表示本机已无 `cloudflared` 进程。注意：关闭隧道不会自动关闭前端预览或后端服务；它们需要在各自终端按 `Ctrl+C` 停止。

如需查找并关闭前端预览端口 4173：

```powershell
$preview = Get-NetTCPConnection -LocalPort 4173 -State Listen -ErrorAction SilentlyContinue
$preview | Select-Object LocalPort, OwningProcess
Stop-Process -Id $preview.OwningProcess
```

执行 `Stop-Process` 前应先核对 PID，避免关闭无关程序。

## 安全注意事项

- Quick Tunnel 没有稳定性保证，进程重启后通常会生成新地址，不适合作为生产部署。
- 完整隧道会让认证、聊天记录、情绪日志和管理接口可从公网访问，只在确有需要时开启。
- 不要把 DeepSeek Key、JWT Secret、数据库密码或真实用户数据写进 README、YAML、Git 提交和聊天记录。
- 平台密钥应通过 `DEEPSEEK_API_KEY` 环境变量或部署平台 Secret 注入。
- 对外测试结束后及时关闭 `cloudflared`，并确认公网地址已经无法访问。
- 正式上线应使用具名 Tunnel、固定域名、Cloudflare Access、HTTPS Cookie、安全响应头和严格的访问控制，不应使用 Quick Tunnel。

## 常见问题

### 公网能打开页面，但无法登录

检查是否使用了 `PUBLIC_FRONTEND_ONLY=true`。该模式有意阻止所有 API。还要确认后端 8080 正常运行，并检查浏览器网络面板中的 `/api/user/login` 响应。

### 页面正常，但聊天不能使用

先检查后端日志。若提示没有可用的模型密钥，需要通过安全环境变量配置新的 `DEEPSEEK_API_KEY`，不要重新使用已经暴露过的旧密钥。

### 管理端修改后仍显示旧结果

前端修改需要重新执行 `npm run build` 并重启预览服务；后端 Java/SQL 修改需要重启后端进程。只刷新浏览器不能加载尚未重启的后端代码。
