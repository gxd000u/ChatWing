# ChatWing - AI 僚机助手

## 概述
ChatWing 是一个基于 Flutter 开发的安卓独立 APK 应用，旨在通过 AI 技术辅助用户在社交聊天（微信、牵手App等）中获得更好的沟通体验。

## 核心功能

### 1. 联系人管理与记忆系统
- 自动抓取微信/牵手App当前聊天界面的联系人信息
- 为每个联系人创建独立的长、期记忆库，互不干扰
- 支持手动修改备注、删除联系人及全部数据
- 主页分析：输入对方主页内容，自动生成破冰话题和兴趣图谱

### 2. 三种核心聊天模式

| 模式 | 名称 | 说明 |
|------|------|------|
| 全托管 | 赛博替身 | AI 全自动回复，支持强制终结、主动出击、发表情包 |
| 半自动 | 策略军师 | 生成 5-6 条风格迥异的回复候选，含心理分析 |
| 纯手动 | 灵感文案 | 粘贴即回，支持切换 5 种聊天风格 |

### 3. 高级交互功能
- 屏幕投射：利用 MediaProjection API 实时截取屏幕
- OCR 识别：自动识别截图中的聊天对话内容
- 悬浮窗：可拖拽的半透明悬浮球，点击展开助手面板
- 截图监听：自动响应系统截图事件

## 技术架构

```
ChatWing/
├── lib/                    # Flutter UI 层
│   ├── main.dart           # 入口
│   ├── models/             # 数据模型
│   ├── services/           # 服务封装（LLM、数据库、平台桥接）
│   ├── providers/          # 状态管理
│   ├── screens/            # 页面
│   └── widgets/            # 组件
├── android/                # Android 原生层
│   └── app/src/main/java/com/chatwing/
│       ├── service/        # 系统级服务
│       ├── engine/         # 聊天引擎
│       ├── llm/            # 大模型适配器
│       ├── db/             # Room 数据库
│       ├── screen/         # 屏幕投射与 OCR
│       └── platform/       # Flutter 桥接
```

## 大模型对接
- 统一接口：`LLMProvider` 接口
- 默认适配：DeepSeek API（deepseek-chat）
- 备用适配：智谱 GLM（glm-4-flash）、通义千问（qwen-turbo）
- 随时切换：运行时可切换 Provider

## 数据库设计
- 引擎：SQLite + Room（安卓）/ sqflite（Flutter 直接访问）
- 核心表：`contacts`（联系人）、`memories`（记忆）、`messages`（消息记录）
- 记忆策略：摘要+最近K轮对话的滑动窗口机制
- 数据隔离：每个联系人独立记忆，Foreign Key 级联删除

## 安全
- API Key 通过 `flutter_secure_storage`（Android KeyStore）加密存储
- 数据仅存本地 SQLite，不上传云端
- 悬浮窗和无障碍服务需要用户手动授权

## 构建要求
- Flutter SDK >= 3.0.0
- Android SDK 26+
- Kotlin 1.9.22
- Gradle 8.2.1

## 快速开始
```bash
cd ChatWing
flutter pub get
flutter run
```

## 权限说明
- `SYSTEM_ALERT_WINDOW` - 悬浮窗显示
- `BIND_ACCESSIBILITY_SERVICE` - 无障碍服务
- `FOREGROUND_SERVICE` - 后台服务
- `MEDIA_PROJECTION` - 屏幕捕获
- `INTERNET` - API 调用
- `POST_NOTIFICATIONS` - 前台服务通知

## 联系方式
ChatWing - 让你的聊天更有温度
