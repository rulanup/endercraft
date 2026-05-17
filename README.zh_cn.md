# LLMEra

[English README](README.md)

LLMEra 是一个面向 Minecraft 1.21.1 / NeoForge 的机械动力（Create）附属模组。它添加智能网络方块，让大语言模型可以接入 Minecraft 自动化流程。

## 功能

- 智能发报机：创建智能网络，保存模型地址、API Key、AI 名称和系统提示词。
- 工具链接站：将方块绑定到智能网络，并向 AI 暴露开关、脉冲、容器读取或可编程动作。
- 技能板：保存可复用的技能描述和步骤，供 AI 辅助自动化时参考。
- 机械动力集成：检测附近的 Create 烈焰人燃烧室，并围绕 Create 自动化流程工作。

## 运行需求

- Minecraft 1.21.1
- NeoForge 21.1.x
- Create 6.0.10 或兼容的 6.0.x 版本
- Java 21

## 构建

```powershell
.\gradlew.bat build
```

构建产物位于 `build/libs/`。

## 版本信息

- 模组名称：LLMEra
- 版本：1.22
- 许可证：GNU GPLv3
