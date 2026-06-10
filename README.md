# Navipilot (CP搭子) - 智能导航辅助应用

<div align="center">

[![Version](https://img.shields.io/badge/version-v260530-blue.svg)](https://github.com/navipilot/CPlink/releases)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-orange.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1-purple.svg)](https://kotlinlang.org)

</div>

---

## 缘起：一个中国玩家的探索之路

我是**全中国第一个**研究和实现这套导航辅助驾驶方案的人。

最初是在研究 CP（comma openpilot 社区）时，发现韩国社区可以通过导航软件的数据分享，实现对车辆的导航控制。这让我非常感兴趣——既然韩国可以，那中国是否也可以？

于是我开始了漫长的探索和研究。我到处查找资料、分析代码、反复尝试。直到研究到**高德地图车机版**时，发现它具备一个**广播功能**，可以通过系统广播输出导航数据。

经过深入分析和逆向研究，我成功提取了高德地图的导航广播数据，并完成了封装、翻译和映射，最终将数据传递到 comma3 设备——**这条路走通了**。

2025 年初开始研究，春节前后就实现了第一步的连通：车速控制、转弯指令等核心功能全部跑通。此后不断迭代完善，陆续加入了**自动超车功能**、**条件实验模式**、**LED 蓝牙彩屏**等众多特性。

目前全中国已有 **600-700 人**体验过这个软件，其中 **200 多人**给予了赞助支持。感谢大家一路的陪伴与信任。

这个项目已经开发到了我能力的天花板，个人精力有限，不再想继续折腾。既然如此，不如开源出来给大家。

---

## 项目现状

在 AI 的加持下，这些功能其实**非常容易实现和扩展**。你只需要把代码下载下来，告诉 AI 你想要什么功能需求，它基本可以快速帮你做出来，生成安装包后软件就能正常使用。

项目已经非常完善，具备众多亮点功能，现在开源出来，希望有想法的朋友继续迭代优化。

---

## 🌟 核心功能

- **📡 多源导航集成**：支持高德车机版（免费）、高德手机 SDK、Google Navigation SDK、腾讯导航 SDK
- **🖥️ 高德投射模式**：MediaProjection 实时投射高德车机版导航画面，不额外占用屏幕
- **📹 摄像头实时预览**：WebSocket 直连 comma3 摄像头，H264 硬解码 20fps 实时显示
- **🤖 智能超车系统**：基于设备感知的自动超车决策，支持三帧防抖、TBT 方向偏好
- **📊 驾驶评分系统**：五维评分引擎（平稳性、预判力、接管依赖、节能、NOO稳定度）
- **🎛️ 设备远程管理**：SSH 连接管理、模型下载上传、参数配置、条件实验模式
- **💡 LED 彩屏联动**：通过蓝牙连接 LED 灯带，动态显示导航信息
- **🔌 丰富扩展性**：支持 WiFi 连接、OBD 数据集成、实时画面投屏等

---

## 🚀 快速开始

### 前置条件

1. **Android 设备**：Android 8.0+ 手机或平板
2. **comma3 设备**：运行 openpilot 的 comma3 设备（在同一局域网）
3. **导航 App**（可选）：
   - 高德地图车机版（免费，推荐）
   - 或内置高德手机 SDK / Google Navigation SDK

### 安装步骤

1. **下载 APK**：从 [Releases](https://github.com/navipilot/CPlink/releases) 下载最新版本
2. **安装应用**：允许"未知来源"安装
3. **首次启动**：
   - 授予位置、蓝牙、通知等权限
   - 浏览新手引导（5 页）了解核心功能
4. **连接设备**：
   - 应用自动发现局域网内的 comma3 设备（mDNS）
   - 或手动输入设备 IP 地址
5. **开始导航**：
   - 使用高德车机版或内置导航开始导航
   - 应用自动将导航数据发送至 comma3

---

## 🗺️ 导航模式

| 导航模式 | 状态 | 坐标系 | 集成方式 | 成本 | 推荐场景 |
|---------|------|--------|----------|------|---------|
| **AMAP（高德车机版）** | ✅ 生产就绪 | GCJ-02 | 广播接收器 | 🆓 免费 | 日常使用（默认） |
| **AMAP_MOBILE（高德手机 SDK）** | ✅ 生产就绪 | GCJ-02 | AMapNaviView 内嵌 | 🆓 免费 | 完整导航体验 |
| **GOOGLE** | ✅ 生产就绪 | WGS-84 | Google Navigation SDK | 💰 需 API Key | 海外/高精度需求 |
| **TENCENT** | ✅ 完整实现 | GCJ-02 | 腾讯导航 SDK | 💰 需授权 | 国内商业场景 |
| **OSM** | ⚠️ 框架就绪 | WGS-84 | MapLibre GL | 🆓 免费 | 离线场景 |

---

## 📡 通信协议

| 端口/协议 | 方向 | 用途 | 频率 |
|----------|------|------|------|
| **UDP 7706** | → comma3 | 实时导航数据（GPS、限速、TBT、电子眼） | 5 Hz |
| **TCP 7709** | → comma3 | 路线规划成功后的路线点坐标 | 一次性 |
| **WebSocket 7000** | ↔ comma3 | 实时车辆/摄像头数据（主要通道） | 实时 |
| **HTTP 7000** | ↔ comma3 | 参数读写 REST API | 按需 |
| **ZMQ 7710** | → comma3 | 控制命令（超车变道指令） | 按需 |

---

## 🛠️ 构建说明

### 环境要求

- **JDK**：JDK 11+
- **Android Studio**：Arctic Fox (2020.3.1) 或更高版本
- **Gradle**：7.5+ (使用 Gradle Wrapper)
- **Kotlin**：2.1+

### 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（需在 local.properties 配置签名密钥）
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 代码检查
./gradlew detekt
```

---

## 🔧 依赖技术

| 类别 | 技术 |
|------|------|
| 语言/UI | Kotlin 2.1, Jetpack Compose + Material 3 |
| 异步 | Kotlin Coroutines + Flow + Channel |
| DI | Koin 3.5.3 |
| 网络 | OkHttp 4.12 + Gson + JeroMQ 0.6.0 |
| 地图 | MapLibre GL 11.8 (OSM), 高德合并 JAR, 腾讯导航 SDK 7.5.0, Google Navigation SDK 7.0.0 |
| SSH | SSHJ 0.38 + BouncyCastle 1.77 |
| 测试 | JUnit 5 + Google Truth + MockK |
| 存储 | EncryptedSharedPreferences + DataStore |
| 日志 | Timber 5.0.1 |
| 播放 | Media3 ExoPlayer 1.2.1 |

---

## 📄 开源协议

本项目采用 **MIT License** 开源协议。

- ✅ 允许商业使用
- ✅ 允许修改和分发
- ✅ 允许私有使用
- ⚠️ 需保留版权声明
- ⚠️ 软件按"原样"提供，不提供任何保证

---

## 💬 关于二次开发

这个软件已经开源，任何人都可以下载代码进行二次开发。

- 如果有朋友在二次开发后进行**收费或高价买断**销售，那是你的选择，你可以自行决定是否付费
- 如果不愿意付费，**完全可以自己下载代码使用**，包括之前所有版本都可以正常使用
- 我是全中国第一个研究并实现这套方案的人，现在把它开源，希望有朋友接手继续迭代优化
- 有兴趣、有想法、有时间的朋友可以进行二次开发，比如适配到自己的车机、集成实时画面、连接 WiFi/OBD 等各种玩法
- 在 AI 时代，这些功能都非常容易实现，有什么想法尽管去做

---

## 🙏 致谢

感谢所有赞助和支持这个项目的朋友，感谢 [comma.ai](https://comma.ai/) 的 openpilot 开源精神，感谢所有开源社区的贡献者。

---

<div align="center">

**如果这个项目对你有帮助，请给我们一个 ⭐️ Star！**

Made with ❤️ by Navipilot

</div>