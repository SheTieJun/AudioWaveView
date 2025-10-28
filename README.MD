# AudioWaveView

一个功能强大的Android音频波形视图组件，仿喜马拉雅录音剪切界面，支持音频波形显示、编辑、剪切、缩放等功能。

## 📋 目录

- [功能特性](#功能特性)
- [快速开始](#快速开始)
  - [安装](#安装)
  - [基本使用](#基本使用)
- [详细配置](#详细配置)
  - [XML属性配置](#xml属性配置)
  - [代码动态配置](#代码动态配置)
- [API文档](#api文档)
  - [核心方法](#核心方法)
  - [监听器接口](#监听器接口)
- [高级功能](#高级功能)
  - [编辑模式](#编辑模式)
  - [覆盖录制](#覆盖录制)
  - [文字背景](#文字背景)
- [实现原理](#实现原理)
- [示例项目](#示例项目)
- [常见问题](#常见问题)
- [更新日志](#更新日志)

## ✨ 功能特性

- [x] **音频波形显示** - 支持实时显示音频波形数据
- [x] **编辑模式** - 支持选择音频片段进行编辑操作
- [x] **音频剪切** - 支持剪切选中的音频片段
- [x] **整段替换** - 支持替换选中音频片段
- [x] **波形缩放** - 支持手势缩放波形显示
- [x] **覆盖录制** - 支持覆盖录制功能
- [x] **滚动支持** - 支持手势滚动浏览长音频
- [x] **播放动画** - 支持播放进度动画显示
- [x] **文字背景** - 支持为时间文字添加背景样式
- [x] **高度自定义** - 丰富的属性配置，满足各种UI需求


### 基本使用

#### 1. XML布局中使用

```xml
<com.shetj.waveview.AudioWaveView
    android:id="@+id/audioWaveView"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    app:wv_rect_level="10"
    app:wv_one_second_rect_size="25"
    app:wv_bottom_line_margin="20dp"
    app:wv_can_scroll="true"
    app:wv_center_line_width="2dp"
    app:wv_cut_icon="@mipmap/icon_test_touch"
    app:wv_cut_icon_size="25dp"
    app:wv_cut_line_width="1dp"
    app:wv_cut_time_text_size="12sp"
    app:wv_time_progress_text_color="@color/black"
    app:wv_time_progress_text_size="12sp"
    app:wv_show_text_background="true"
    app:wv_text_background_color="#80000000"
    app:wv_text_background_horizontal_padding="8dp"
    app:wv_text_background_vertical_padding="4dp"
    app:wv_text_background_corner_radius="4dp" />
```

#### 2. 代码中使用

```kotlin
val audioWaveView = findViewById<AudioWaveView>(R.id.audioWaveView)

// 设置监听器
audioWaveView.setListener(object : AudioWaveView.OnChangeListener {
    override fun onUpdateCurrentPosition(position: Long, duration: Long) {
        // 当前播放位置更新
    }
    
    override fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {
        // 剪切位置更新
    }
    
    override suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean {
        // 执行音频剪切操作
        return true
    }
})

// 添加音频数据
audioWaveView.addFrame(0.5f, 1000L) // 添加单个帧数据
```

## ⚙️ 详细配置

### XML属性配置

AudioWaveView 提供了丰富的XML属性配置，按功能分为以下几个部分：

#### 第一部分：顶部和底部线条设置

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wv_top_line_margin` | dimension | 30dp | 顶部线距离顶部的高度 |
| `wv_bottom_line_margin` | dimension | 30dp | 底部线距离底部的高度 |
| `wv_top_bottom_line_color` | color | #c8cad0 | 顶部和底部线的颜色 |
| `wv_top_bottom_line_width` | dimension | 3dp | 顶部和底部线的宽度 |

#### 第二部分：矩形波形设置

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wv_rect_scale` | float | 1.0f | 缩放级别，范围0.1~1.4 |
| `wv_rect_level` | integer | 10 | 矩形分级数量，决定波形高度层次 |
| `wv_one_second_rect_size` | float | 25 | 一秒钟包含的矩形数量 |
| `wv_rect_width` | dimension | 2dp | 矩形的宽度 |
| `wv_rect_corner_radius` | dimension | 2dp | 矩形的圆角半径 |
| `wv_rect_space` | dimension | 2dp | 矩形之间的间距 |
| `wv_rect_right_color` | color | #c8cad0 | 中线右边矩形的颜色 |
| `wv_rect_left_color` | color | #93b3ea | 中线左边矩形的颜色 |

#### 第三部分：中线设置

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wv_center_line_color` | color | #93b3ea | 中线颜色 |
| `wv_center_line_width` | dimension | 2dp | 中线宽度 |

#### 第四部分：剪切功能设置

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wv_cut_icon` | reference | - | 剪切标记图标资源 |
| `wv_cut_icon_size` | dimension | 30dp | 剪切图标大小 |
| `wv_cut_line_color` | color | RED | 剪切线颜色 |
| `wv_cut_line_width` | dimension | 3dp | 剪切线宽度 |
| `wv_cut_time_text_color` | color | RED | 剪切时间文字颜色 |
| `wv_cut_time_text_size` | dimension | 18sp | 剪切时间文字大小 |
| `wv_cut_select_color` | color | #33FFBB22 | 选中区域背景颜色 |

#### 第五部分：其他设置

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wv_time_progress_text_color` | color | #c8cad0 | 时间刻度文字颜色 |
| `wv_time_progress_text_size` | dimension | 18sp | 时间刻度文字大小 |
| `wv_can_scroll` | boolean | true | 是否可以滚动 |
| `wv_show_center_line` | boolean | true | 是否显示中线 |
| `wv_show_top_bottom_line` | boolean | true | 是否显示上下线 |

#### 第六部分：文字背景设置（新功能）

| 属性名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wv_show_text_background` | boolean | false | 是否显示文本背景 |
| `wv_text_background_color` | color | #80000000 | 文本背景颜色 |
| `wv_text_background_horizontal_padding` | dimension | 8dp | 文本背景水平内边距 |
| `wv_text_background_vertical_padding` | dimension | 4dp | 文本背景垂直内边距 |
| `wv_text_background_corner_radius` | dimension | 4dp | 文本背景圆角半径 |

### 代码动态配置

除了XML属性配置外，AudioWaveView还提供了丰富的代码配置方法：

#### 颜色设置

```kotlin
// 设置中线颜色
audioWaveView.setCenterLineColor(Color.BLUE)

// 设置左侧波形颜色
audioWaveView.setLeftPaintColor(Color.GREEN)

// 设置右侧波形颜色
audioWaveView.setRightPaintColor(Color.GRAY)

// 设置剪切标记颜色
audioWaveView.setCutMarkerPointPaintColor(Color.RED)

// 设置选中区域背景颜色
audioWaveView.setCutSelectPaintColor(Color.parseColor("#33FFBB22"))
```

#### 波形样式设置

```kotlin
// 设置波形宽度
audioWaveView.setWaveWidth(3f)

// 设置波形间距
audioWaveView.setWaveSpace(1f)

// 设置波形圆角
audioWaveView.setWaveCornerRadius(4f)

// 设置缩放比例
audioWaveView.setWaveScale(1.2f)

// 设置时间文字大小
audioWaveView.setTimeSize(14f)
```

#### 文字背景设置

```kotlin
// 设置是否显示文字背景
audioWaveView.setShowTextBackground(true)

// 设置文字背景颜色
audioWaveView.setTextBackgroundColor(Color.parseColor("#80000000"))

// 设置文字背景内边距
audioWaveView.setTextBackgroundPadding(10f, 6f) // 水平10dp，垂直6dp

// 设置文字背景圆角
audioWaveView.setTextBackgroundCornerRadius(6f)
```

## 📚 API文档

### 核心方法

#### 数据操作

```kotlin
// 添加单个音频帧
fun addFrame(frame: Float, duration: Long)

// 批量添加音频帧
fun addFrames(index: Int, newFrameArray: FrameArray, frameDuration: Long, editModel: Boolean = true)

// 替换音频片段
fun replaceFrames(startTime: Long, endTime: Long, newFrameArray: FrameArray, newFrameDuration: Long, editModel: Boolean = true)

// 清空所有数据
fun clearFrame()

// 获取所有音频帧数据
fun getFrames(): ArrayList<Float>

// 获取音频总时长
fun getDuration(): Long
```

#### 播放控制

```kotlin
// 设置当前播放位置
fun setCurrentPosition(currentTime: Long)

// 获取当前播放位置
fun getCurrentPosition(): Long

// 开始播放动画
fun startPlayAnim(speed: Float = 1.0f)

// 暂停播放动画
fun pausePlayAnim()

// 检查是否正在播放
fun isPlaying(): Boolean
```

#### 编辑功能

```kotlin
// 开启编辑模式
fun startEditModel()

// 开启编辑模式（指定时间范围）
fun startEditModel(startTime: Long? = null, endTime: Long? = null)

// 关闭编辑模式
fun closeEditModel()

// 检查是否处于编辑模式
fun isEditModel(): Boolean

// 剪切选中片段
suspend fun cutSelect(): Boolean

// 开始覆盖录制
suspend fun startOverwrite(): Boolean

// 获取剪切开始时间
fun getCutStartTime(): Long

// 获取剪切结束时间
fun getCutEndTime(): Long
```

#### 滚动控制

```kotlin
// 设置是否可以滚动
fun setEnableScroll(enable: Boolean, toEnd: Boolean = false)

// 滚动到末尾
fun scrollToEnd(needAnim: Boolean = true)

// 滚动到开头
fun scrollToStart(needAnim: Boolean = true)

// 检查是否已滚动到末尾
fun isScrollEnd(): Boolean
```

### 监听器接口

```kotlin
interface OnChangeListener {
    /**
     * 当前播放位置更新
     * @param position 当前位置（毫秒）
     * @param duration 总时长（毫秒）
     */
    fun onUpdateCurrentPosition(position: Long, duration: Long)

    /**
     * 剪切位置更新
     * @param startPosition 开始位置（毫秒）
     * @param endPosition 结束位置（毫秒）
     */
    fun onUpdateCutPosition(startPosition: Long, endPosition: Long)

    /**
     * 执行音频剪切
     * @param startTime 开始时间（毫秒）
     * @param endTime 结束时间（毫秒）
     * @return 是否剪切成功
     */
    suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean

    /**
     * 剪切完成回调
     */
    fun onCutFinish()

    /**
     * 缩放级别更新
     * @param scale 当前缩放比例
     */
    fun onUpdateScale(scale: Float)

    /**
     * 编辑模式状态变化
     * @param isEditModel 是否处于编辑模式
     */
    fun onEditModelChange(isEditModel: Boolean)
}
```

## 🔥 高级功能

### 编辑模式

编辑模式允许用户选择音频片段进行剪切、替换等操作：

```kotlin
// 开启编辑模式
audioWaveView.startEditModel()

// 开启编辑模式并指定选择范围
audioWaveView.startEditModel(startTime = 1000L, endTime = 5000L)

// 监听编辑模式状态变化
audioWaveView.setListener(object : AudioWaveView.OnChangeListener {
    override fun onEditModelChange(isEditModel: Boolean) {
        if (isEditModel) {
            // 进入编辑模式
            showEditControls()
        } else {
            // 退出编辑模式
            hideEditControls()
        }
    }
    
    override fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {
        // 更新选择范围显示
        updateSelectionInfo(startPosition, endPosition)
    }
})
```

### 覆盖录制

覆盖录制功能允许从当前位置开始覆盖后续的音频内容：

```kotlin
// 开始覆盖录制
lifecycleScope.launch {
    val success = audioWaveView.startOverwrite()
    if (success) {
        // 覆盖录制开始成功
        startRecording()
    }
}
```

### 文字背景

新增的文字背景功能可以为时间显示文字添加背景，提高可读性：

#### XML配置方式

```xml
<com.shetj.waveview.AudioWaveView
    ...
    app:wv_show_text_background="true"
    app:wv_text_background_color="#80000000"
    app:wv_text_background_horizontal_padding="8dp"
    app:wv_text_background_vertical_padding="4dp"
    app:wv_text_background_corner_radius="4dp" />
```

#### 代码配置方式

```kotlin
// 启用文字背景
audioWaveView.setShowTextBackground(true)

// 设置背景颜色（半透明黑色）
audioWaveView.setTextBackgroundColor(Color.parseColor("#80000000"))

// 设置内边距
audioWaveView.setTextBackgroundPadding(8f, 4f)

// 设置圆角
audioWaveView.setTextBackgroundCornerRadius(4f)
```

## 🔧 实现原理

### 1. 滚动实现

本质是通过偏移量的变化来实现滚动效果：
- 使用 `Scroller` + `DecelerateInterpolator` 实现手势阻尼效果
- 通过 `mOffsetX` 变量控制整体内容的偏移

### 2. 缩放声波

- 使用 `ScaleGestureDetector` 检测缩放手势
- 通过 `mScaleFactor` 变量控制缩放比例
- 重新计算并绘制UI元素

### 3. 覆盖模式实现

- 剪切中线以后的数据（触发剪切回调）
- 在末端开始添加新的音频数据
- 实现无缝的覆盖录制体验

### 4. 替换整段实现

- 剪切选中部分的数据
- 在指定位置插入新的音频数据
- 文件操作需要使用者自行实现

### 5. 颜色分割效果

使用 `PorterDuffXfermode` 的 `SRC_ATOP` 模式：
- 给左边一半画一个大矩形
- 只绘制重叠部分，实现精确的颜色分割效果

### 6. 文字背景绘制

- 计算文字的实际尺寸
- 根据内边距计算背景矩形范围
- 使用 `drawRoundRect` 绘制圆角背景

## 📱 示例项目

项目包含完整的示例应用，展示了所有功能的使用方法：

- **[demo-record](demo-record)** - 完整的录音、剪切、播放示例
  - ✅ 真实录音功能
  - ✅ 音频剪切功能
  - ✅ 音频播放功能
  - ✅ 覆盖录制功能

### 运行示例

1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 运行 `demo-record` 模块
4. 体验完整的音频编辑功能

## ❓ 常见问题

### Q: 如何设置音频数据？

A: AudioWaveView 只处理UI交互逻辑，不直接支持PCM数据。你需要：
1. 将音频文件转换为PCM格式
2. 从PCM数据中提取分贝值
3. 根据 `wv_rect_level` 和 `wv_one_second_rect_size` 设置合适的数据
4. 使用 `addFrame()` 或 `addFrames()` 添加数据

### Q: 如何配置波形显示参数？

A: 关键参数说明：
- `wv_rect_level="10"` - 波形分为10个高度级别
- `wv_one_second_rect_size="25"` - 一秒显示25个矩形
- 示例：一个矩形表示40ms（1000ms ÷ 25 = 40ms）

### Q: 为什么手势操作没有响应？

A: 检查以下设置：
- 确保 `wv_can_scroll="true"`
- 确保没有在播放状态（播放时会禁用手势）
- 检查是否正确设置了监听器

### Q: 如何自定义剪切图标？

A: 使用 `wv_cut_icon` 属性：
```xml
app:wv_cut_icon="@mipmap/your_custom_icon"
app:wv_cut_icon_size="30dp"
```

### Q: 文字背景功能不显示？

A: 确保正确配置：
```xml
app:wv_show_text_background="true"
app:wv_text_background_color="#80000000"
```


## 📝 更新日志

### 最新版本

- ✅ **新增文字背景功能** - 支持为时间文字添加背景样式
- ✅ **优化渲染性能** - 减少不必要的遍历，直接计算索引范围
- ✅ **完善API文档** - 提供更详细的使用说明和示例

### 已完成功能

- ✅ 音频波形显示
- ✅ 编辑模式支持
- ✅ 音频剪切功能
- ✅ 整段替换功能
- ✅ 波形缩放功能
- ✅ 覆盖录制功能
- ✅ 滚动浏览支持
- ✅ 播放动画效果
- ✅ 文字背景样式
- ✅ 性能优化

---

## 📄 许可证

本项目采用 MIT 许可证，详情请查看 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目！

## 📞 联系方式

如果你有任何问题或建议，请通过以下方式联系：

- GitHub Issues: [提交问题](https://github.com/SheTieJun/AudioWaveView/issues)

---

**⭐ 如果这个项目对你有帮助，请给个Star支持一下！**
