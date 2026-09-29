# 修复 VSCode 运行问题

## 问题原因
Gradle 缓存中混用了不同 Java 版本编译的类文件（class file major version 69 表示 Java 21）

## 解决步骤

### 1. 清除 Gradle 缓存
在 CMD 中运行：
```cmd
rd /s /q "%USERPROFILE%\.gradle\caches"
```

### 2. 重启 VSCode
完全关闭 VSCode 后重新打开

### 3. 等待 Gradle 同步
打开项目后，等待 VSCode 右下角显示 "Gradle: Build successful"

### 4. 运行项目
按 `F5` 或点击左侧运行按钮运行 `runClient`

## 如果仍有问题

### 检查 Java 版本
确保系统只使用 Java 17：
```cmd
java -version
```
应该显示 `openjdk version "17.0.x"`

### 手动指定 Java 路径
在 `gradle.properties` 中已设置：
```properties
org.gradle.java.home=D:\\Java\\jdk-17
```

### 重新生成运行配置
```cmd
cd JournalMod
gradlew.bat clean
gradlew.bat genVSCodeRuns
```

## 项目文件说明

- `.vscode/settings.json` - VSCode Java 配置（已设置 Java 17 路径）
- `gradle.properties` - Gradle 配置（已设置 Java 17 路径）
