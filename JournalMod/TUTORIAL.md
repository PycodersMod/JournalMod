# 日志书 Mod 开发教程 (Windows + VSCode)

## 快速开始

### 1. 环境准备
- 安装 Java 17
- 安装 VSCode + Extension Pack for Java

### 2. 初始化项目
```cmd
cd JournalMod
gradlew.bat genVSCodeRuns
```

### 3. 运行测试
- VSCode → 打开文件夹 → 选择 JournalMod
- 按 `F5` 运行 `runClient`

### 4. 构建 JAR
```cmd
gradlew.bat build
```

## 项目结构

```
JournalMod/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/main/
    ├── java/com/journalmod/
    └── resources/
```

## 配置文件

位置：`%appdata%\.minecraft\config\journalmod-common.toml`

```toml
[general]
journalBookName = "日志书"
coverTitle = "冒险日志"
introductionText = "这是一本记录冒险经历的日志书..."

[pages]
pageNames = ["第一章：启程", "第二章：探索"]
pageContents = ["内容1...", "内容2..."]
```
