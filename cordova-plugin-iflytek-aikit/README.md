# 讯飞 AIKit Ionic 插件

这是一个适用于 Ionic/Cordova 的讯飞语音 AI 能力插件，封装了讯飞 AIKit SDK 的功能，支持语音唤醒、语音合成、命令词识别等功能。

## 功能特性

-   ✅ **语音唤醒（IVW）** - 支持自定义唤醒词，实时唤醒检测
-   ✅ **语音合成（TTS）** - 支持轻量版 Aisound 和 XTTS 两种合成引擎，支持**动态缓存**
-   ✅ **命令词识别（ESR）** - 支持中英文命令词识别，修复乱码，支持超时自动停止
-   ✅ **音频播放** - 内置 PCM 音频播放功能，支持 16k/24k 采样率
-   ✅ **模块化架构** - 推荐使用 Service 模式，业务逻辑与 UI 完全解耦
-   ✅ **资源文件自动管理** - 资源文件已内置，初始化时自动检测并复制

## 安装

```bash
cordova plugin add cordova-plugin-iflytek-aikit
```

## 快速开始（推荐 Service 模式）

为了实现最佳的开发体验，建议在 Ionic 项目中创建一个 `IflytekService` 来统一管理语音逻辑。

### 1. 初始化并启动助手

```typescript
// 在 IflytekService 中
public async startAssistant() {
    const config = {
        appId: "your_id",
        apiKey: "your_key",
        apiSecret: "your_secret",
        workDir: "/sdcard/iflytek/",
        abilities: "e867a88f2;ece9d3c90;e75f07b62"
    };

    await IflytekAIKit.init(config);

    // 注册全局监听
    IflytekAIKit.registerListener((event) => {
        this.handleEvent(event);
    });
}
```

### 2. 实现智能交互流

```typescript
private handleEvent(event) {
    switch (event.event) {
        case 'wakeUpResult':
            // 监听到唤醒词，自动回复并开启录音
            this.speak("我在，请说", "reply_im_here").then(() => {
                IflytekAIKit.startCommandRecognition({ fsaPath: "...", timeoutSeconds: 5 });
            });
            break;
        case 'commandResult':
            // 识别到指令，动态播报并缓存
            const cmd = event.data.value;
            this.speak(`好的，已为您${cmd}`, `reply_${cmd}`);
            break;
    }
}
```

## 功能使用说明

### 1. 语音合成（TTS）

支持**动态缓存**：通过指定 `fileName`，插件会检查本地是否存在该文件，存在则直接播放，不存在则合成并保存，极大提升二次响应速度。

```typescript
IflytekAIKit.startTTS(
    {
        text: "你好，我是小迪",
        vcn: "xiaoyan",
        fileName: "hello.pcm", // 指定文件名实现缓存
    },
    success,
    error
)
```

### 2. 命令词识别（ESR）

-   **超时机制**：支持 `timeoutSeconds` 参数，超时后自动停止录音并触发 `commandTimeout` 事件。
-   **结果去重**：自动处理 `pgs`（渐进式）和 `plain`（最终）结果。

```typescript
IflytekAIKit.startCommandRecognition(
    {
        fsaPath: "/sdcard/iflytek/esr/fsa/cn_fsa.txt",
        languageType: 0,
        timeoutSeconds: 10, // 10秒超时
    },
    success,
    error
)
```

## 事件类型参考

| 事件名               | 说明                     | 数据格式                                         |
| :------------------- | :----------------------- | :----------------------------------------------- |
| `authSuccess`        | SDK 授权成功             | -                                                |
| `wakeUpResult`       | 检测到唤醒词             | `{ func_wake_up: string }`                       |
| `commandResult`      | 最终识别结果（成功）     | `{ value: string }`                              |
| `commandProgress`    | 识别中间进度（实时刷屏） | `{ value: string }`                              |
| `commandNoMatch`     | 未识别到有效语音         | `{ value: string, message: string }`             |
| `commandUnsupported` | 识别到语音但不支持此操作 | `{ value: string, message: string }`             |
| `commandTimeout`     | 识别超时                 | `{ message: string, timeoutSeconds: number }`    |
| `commandError`       | 识别错误                 | `{ code: number, message: string }`              |
| `ttsResult`          | 合成完成                 | `{ filePath: string }`                           |
| `ttsProgress`        | 合成进度                 | `{ pos: number, len: number, progress: number }` |

## 命令词识别结果处理逻辑

插件会根据识别结果自动判断并发送相应事件：

1. **有 `plain` 结果** → 发送 `commandResult`（识别成功，命令词在列表中）
2. **有 `pgs` 但没有 `plain`** → 发送 `commandUnsupported`（识别到语音但不在命令词列表中）
3. **只有 `vad` 事件** → 发送 `commandNoMatch`（未识别到有效语音）
4. **识别超时** → 发送 `commandTimeout`（超时自动停止）

### 完整事件处理示例

```typescript
IflytekAIKit.registerListener((event) => {
    switch (event.event) {
        case "commandResult":
            // 识别成功，执行对应操作
            const cmd = event.data.value
            console.log("识别到命令:", cmd)
            break

        case "commandUnsupported":
            // 识别到语音但不支持，可以提示用户
            console.log("不支持的操作:", event.data.value)
            break

        case "commandNoMatch":
            // 未识别到有效语音，可以提示用户重新说话
            console.log("未识别到有效语音")
            break

        case "commandTimeout":
            // 识别超时，可以提示用户重新说话
            console.log("识别超时")
            break
    }
})
```

## 更新日志

### v1.0.2

-   🔧 **识别结果优化**：统一识别结果处理逻辑，根据结果类型自动发送对应事件
-   🔧 **VAD 事件处理**：关闭 VAD 功能并屏蔽 VAD 事件，避免无结果事件干扰
-   ✨ **新增事件**：
    -   `commandNoMatch`：未识别到有效语音时触发
    -   `commandUnsupported`：识别到语音但不在命令词列表中时触发
-   🔧 **代码优化**：重构识别结果处理逻辑，避免多个事件冲突

### v1.0.1

-   ✨ **架构升级**：推荐使用 Service 模式管理插件，实现业务逻辑与 UI 完全解耦
-   ✨ **智能交互流**：支持“唤醒 -> 自动回复 -> 自动录音 -> 结果处理”全自动化闭环
-   ✨ **动态缓存系统**：TTS 支持 `fileName` 参数，实现语音文件按指令动态缓存
-   ✨ **识别优化**：修复乱码，支持自定义超时时间，区分渐进式和最终结果

-   ✨ **新增功能**：添加音频播放功能（`playAudio` / `stopPlayAudio`）
-   ✨ **进度监控**：TTS 和 XTTS 支持合成进度事件

### v1.0.0

-   初始版本，支持唤醒、合成、识别基础功能。

## 许可证

MIT License
