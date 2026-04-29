# 前端运行说明

前端使用 Vue 3、Vite、Pinia、Vue Router、Axios 和 Element Plus，提供系统总览、咨询工作台、知识库、订单管理、日志中心和 AI 测试页面。

## 启动

先启动后端 `http://localhost:8081`，再启动前端：

```powershell
cd web
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

## 接口代理

开发环境使用 Vite 代理，默认把 `/api` 转发到后端：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081'
    }
  }
}
```

前端请求基础路径在 `.env.development` 中配置：

```env
VITE_API_BASE_URL=/api
```

## 验证

构建：

```powershell
npm run build
```

浏览器烟测：

```powershell
npm run test:browser
```

测试前需要保证后端和前端开发服务都已启动。

