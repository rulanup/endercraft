# LLMEra

[中文说明](README.zh_cn.md)

LLMEra is a Create addon for Minecraft 1.21.1 on NeoForge. It adds intelligent network blocks that make it possible to connect large language models to Create-style automation.

## Features

- Intelligent Transmitter: creates an intelligent network, stores the model endpoint, API key, AI name, and system prompt.
- Tool Link Station: binds blocks to an intelligent network and exposes switch, pulse, inventory read, or programmable actions.
- Skill Board: stores reusable skill descriptions and ordered steps for AI-assisted automation.
- Create integration: detects nearby Create Blaze Burners and supports a Create-centered automation workflow.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Create 6.0.10 or compatible 6.0.x build
- Java 21

## Build

```powershell
.\gradlew.bat build
```

The built jar is generated under `build/libs/`.

## Version

- Mod name: LLMEra
- Version: 1.12
- License: GNU GPLv3
