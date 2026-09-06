# X WMS 三端前端架构设计文档

> 版本：V1.0 | 日期：2026-08-18 | 状态：已确认

## 一、架构总览

### 1.1 技术决策（已确认）

| 决策项 | 选型 | 理由 |
|--------|------|------|
| 三端统一框架 | React 19 | 代码复用率75%，团队技术栈统一 |
| Monorepo管理 | pnpm 10 + Turborepo 2 | 共享包+并行构建+远程缓存 |
| 构建工具 | Vite 6 | Rolldown引擎，冷启动<500ms |
| 语言 | TypeScript 5.7 | 类型安全 |
| 样式方案 | Tailwind CSS 4 | 原子化CSS，零运行时，Oxide引擎 |
| 组件库 | shadcn/ui + Radix UI | 可复制源码，高度可定制 |
| 表格 | TanStack Table 8 | 虚拟滚动，10万+数据流畅 |
| 表单 | React Hook Form 8 + Zod 4 | 非受控，性能最优 |
| 状态管理 | Zustand 5 | 轻量(1KB)，API简洁 |
| 数据请求 | TanStack Query 5 | 缓存/重试/乐观更新 |
| 路由 | React Router 7 | Loader/Action数据加载 |
| Web图表 | ECharts 6 | 大数据量+高级图表+数据大屏 |
| PDA/打包台图表 | Recharts 3 | 轻量，React原生 |
| PDA打包 | Capacitor 6 + PWA | APK打包+离线缓存 |
| 打包台桌面 | Tauri 2 | Rust后端，安装包<10MB |

### 1.2 项目结构

```
frontend/
├── package.json              # 根配置（pnpm+turbo）
├── pnpm-workspace.yaml
├── turbo.json
├── tsconfig.base.json
├── apps/
│   ├── web-admin/            # Web后台(PC) - 端口3000
│   ├── pda-mobile/           # PDA移动端 - 端口3001
│   └── pack-station/         # 打包台客户端 - 端口3002 + Tauri
└── packages/
    ├── ui/                   # 共享组件库（shadcn+WMS业务组件）
    ├── api/                  # 共享API层（Axios+TanStack Query）
    └── shared/               # 共享类型/常量/工具
```

## 二、三端架构详解

### 2.1 Web后台（PC管理端）

**定位**：仓库管理员、运营、财务、系统管理员使用
**设备**：PC浏览器，1920×1080+
**核心特性**：
- 多标签页管理（可关闭/拖拽）
- 高级数据表格（虚拟滚动/列固定/筛选/导出）
- 可拖拽仪表盘（react-grid-layout）
- 全局搜索（Cmd+K命令面板）
- 暗黑模式
- ECharts数据大屏

**页面模块**：
- 工作台（Dashboard + KPI卡片 + 趋势图）
- 入库管理（入库单列表/详情/收货/上架）
- 出库管理（出库单/波次/分配/拣货/复核/发货）
- 库存管理（库存查询/事务/移动/调整/冻结/盘点/批次追踪）
- 基础数据（产品/客户/货主/仓库/库位/批次属性）
- 业务规则（12类规则引擎配置）
- 报表&KPI（出入库报表/库龄分析/ABC分析/自定义查询）
- 系统管理（用户/角色/权限/配置/日志）

### 2.2 PDA移动端

**定位**：仓库一线作业人员（拣货/收货/上架/盘点）
**设备**：PDA手持终端（Android，4-6寸，物理扫码键）
**核心特性**：
- 扫码优先（自动聚焦+回车触发+物理键映射）
- 大按钮/大字体（最小48px点击区域）
- PWA离线可用（Service Worker缓存）
- Capacitor打包APK（调用原生扫码/摄像头）
- 任务驱动模式（"获取下一任务"自动分配）
- 语音提示（Web Speech API）
- HashRouter（适配APK本地文件）

**页面模块**：
- 入库作业（标准收货/混托盘收货/盲收/标准上架/批量上架/质检）
- 出库作业（订单拣货/标签拣货/波次合拣/装箱/复核/播种/装车/发货）
- 库存作业（移库/库存查询/冻结解冻/盘点/补货/养护）
- 任务管理（获取下一任务/任务调度/导航）

### 2.3 打包台客户端

**定位**：打包台作业人员
**设备**：打包台专用终端（触摸屏PC+电子秤+热敏打印机+扫码枪）
**核心特性**：
- Tauri 2桌面端（Rust后端调用串口/USB）
- 电子秤实时读数（串口通信）
- 热敏打印机ESC/POS指令
- 重量校验（实时vs理论，差异报警）
- 全屏Kiosk模式
- 离线打印（本地缓存，断网可用）

**页面模块**：
- 打包主界面（订单信息+称重+扫码+打包操作）
- 快递单管理（批量获取单号/批量打印/打印记录）
- 系统设置（电子秤配置/打印机配置/打包台绑定）

## 三、共享层设计

### 3.1 @xwms/ui（共享组件库）

**基础组件（shadcn风格）**：
Button, Card, Badge, Input, Select, Dialog, Form, Table, Tabs, DropdownMenu, Sonner(Toast), DataTable

**WMS业务组件**：
- StatusBadge：状态标签（自动映射颜色+中文）
- ScanInput：扫码输入框（自动聚焦+回车触发+扫描动画）
- KpiCard：KPI指标卡片（趋势+图标+颜色）

### 3.2 @xwms/api（共享API层）

- Axios实例（拦截器：Token注入/X-Owner-Code/401刷新/统一错误处理）
- API端点：inbound/outbound/inventory/master/auth
- TanStack Query hooks（缓存/重试/乐观更新）

### 3.3 @xwms/shared（共享类型/常量/工具）

- 类型定义：InboundOrder/OutboundOrder/Inventory/Product/Customer等
- 常量：订单状态/单据类型/库位类型/字典编码
- 工具：条码校验/本地存储封装/权限判断/文件大小格式化

## 四、状态管理与数据流

### 4.1 状态分层

```
服务端状态（Oracle/Redis）
    ↓ TanStack Query（缓存5分钟，变更自动失效）
服务端缓存（React Query Cache）
    ↓ Zustand（客户端UI状态）
客户端状态（用户信息/Token/当前仓库/侧边栏/标签页）
```

### 4.2 为什么用Zustand而非Redux

- 体积1KB vs Redux 10KB+
- 无模板代码（slice/action/reducer）
- 精确订阅，无re-render问题
- WMS客户端状态主要是UI状态，不需要Redux复杂度

## 五、性能优化策略

### 5.1 首屏加载
- 路由懒加载（React.lazy + Suspense）
- Vite manualChunks分离vendor
- PWA Service Worker缓存静态资源
- 骨架屏

### 5.2 运行时
- TanStack Table虚拟滚动（10万+数据）
- React.memo列表项
- useDeferredValue搜索延迟
- Web Worker大数据计算
- 请求防抖/取消

### 5.3 构建
- Turborepo远程缓存
- Vite Rolldown（Rust构建引擎）
- Tree Shaking

## 六、与后端对接

- RESTful + OpenAPI 3.0（后端Knife4j自动生成文档）
- JWT认证（Access 2h + Refresh 7d）
- 多货租户：X-Owner-Code请求头
- 实时数据：WebSocket/SSE（库存更新/任务推送/大屏监控）

## 七、部署方案

| 端 | 部署方式 |
|----|---------|
| Web后台 | Nginx静态资源 + 反向代理到后端 |
| PDA | PWA（浏览器访问）+ Capacitor APK（企业应用商店） |
| 打包台 | Tauri打包Windows安装包（<10MB）+ 自动更新 |

## 八、开发规范

- ESLint + Prettier + Husky + lint-staged
- Conventional Commits提交规范
- 组件必须写Props类型，禁止any
- 列表超过100条必须虚拟滚动
- 命名：组件PascalCase，hooks use前缀，常量UPPER_SNAKE_CASE

## 九、实施计划

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 | Monorepo搭建+共享包+三端项目骨架 | ✅ 已完成 |
| P1 | Web后台核心模块（入库/出库/库存/基础数据） | 🔄 进行中（骨架已搭） |
| P2 | PDA移动端核心作业（收货/上架/拣货/盘点） | 🔄 进行中（骨架已搭） |
| P3 | 打包台客户端（称重/复核/打印） | 🔄 进行中（骨架已搭） |
| P4 | 报表/KPI/大屏/系统管理 | ⏳ 待开发 |
| P5 | 性能优化/测试/上线 | ⏳ 待开发 |

## 十、项目统计

- 共享组件：15+个
- API端点：5大模块30+接口
- 类型定义：20+业务实体
- Web后台页面：10+（骨架）
- PDA页面：8+（骨架）
- 打包台页面：3个（完整）
- Tauri Rust命令：7个（电子秤+打印机）
