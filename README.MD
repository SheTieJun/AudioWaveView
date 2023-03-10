## AudioWaveView
[![](https://jitpack.io/v/SheTieJun/AudioWaveView.svg)](https://jitpack.io/#SheTieJun/AudioWaveView)
仿喜马拉雅录音剪切的view

- [X] 声音波纹
- [X] 编辑模式
- [X] 整段替换声波
- [X] 缩放声波
- [X] 覆盖录制逻辑实现

> 只做交互逻辑，不做其他逻辑，所以不直接支持PCM,但是使用者可以audio文件通过转PCM获取db,
> 
> 在根据自己的实际情况设置wv_rect_level和wv_one_second_rect_size 然后添加frame
> 
> 也可以自行继承实现

| 测试相关参数                | DEMO                    |
|-----------------------|-------------------------|
| ![](/image/demo.webp) | ![](/image/demo剪切.webp) |

## 集成
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

```groovy
  implementation 'com.github.SheTieJun:AudioWaveView:Tag'
```

```xml
<com.shetj.waveview.AudioWaveView
    android:id="@+id/audioWaveView"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent"
    app:wv_rect_level="10"
    app:wv_bottom_line_margin="20dp"
    app:wv_can_scroll="true"
    app:wv_center_line_width="2dp"
    app:wv_cut_icon="@mipmap/icon_test_touch"
    app:wv_cut_icon_size="25dp"
    app:wv_cut_line_width="1dp"
    app:wv_cut_time_text_size="12sp"
    app:wv_time_progress_text_color="@color/black"
    app:wv_time_progress_text_size="12sp" />
```

```xml

<declare-styleable name="AudioWaveView">
    <!--        顶部的线到底部的距离-->
    <attr name="wv_top_line_margin" format="dimension" />
    <!--        底部的线到底部的距离-->
    <attr name="wv_bottom_line_margin" format="dimension" />
    <!--        一秒存在的矩形数量 默认25个 -->
    <attr name="wv_one_second_rect_size" format="float" />
    <!--        顶部线和底部线的颜色-->
    <attr name="wv_top_bottom_line_color" format="color" />
    <!--        顶部线和底部线的颜色-->
    <attr name="wv_top_bottom_line_width" format="dimension" />
    <!--        矩形相关设置-->
    <!--        缩放级别内部限制,默认1f-->
    <attr name="wv_rect_scale" format="float" />
    <!--    矩形分几个级别，默认10种高度的矩形-->
    <attr name="wv_rect_level" format="integer" />
    <!--    矩形的宽度-->
    <attr name="wv_rect_width" format="dimension" />
    <!--    矩形的圆角-->
    <attr name="wv_rect_corner_radius" format="dimension" />
    <!--    矩形的间距-->
    <attr name="wv_rect_space" format="dimension" />
    <!--    矩形中线右边的颜色-->
    <attr name="wv_rect_right_color" format="color" />
    <!--   矩形中线左边的颜色 -->
    <attr name="wv_rect_left_color" format="color" />

    <!--        中线的颜色，宽度-->
    <attr name="wv_center_line_color" format="color" />
    <attr name="wv_center_line_width" format="dimension" />

    <!--        剪切部分的设置-->
    <!--        icon 资源-->
    <attr name="wv_cut_icon" format="reference" />
    <!--        icon的大小-->
    <attr name="wv_cut_icon_size" format="dimension" />
    <!--        剪切线的颜色-->
    <attr name="wv_cut_line_color" format="color" />
    <!--        剪切线的宽度-->
    <attr name="wv_cut_line_width" format="dimension" />
    <!--        剪切线上时间字体颜色-->
    <attr name="wv_cut_time_text_color" format="dimension" />
    <!--        剪切线上时间字体大小-->
    <attr name="wv_cut_time_text_size" format="dimension" />
    <!--        2条剪切线中间区域选中的颜色-->
    <attr name="wv_cut_select_color" format="color" />

    <!--        时间刻度的字体颜色-->
    <attr name="wv_time_progress_text_color" format="color" />
    <!--    时间刻度的字体大小    -->
    <attr name="wv_time_progress_text_size" format="dimension" />
    <!--        是否可以滚动-->
    <attr name="wv_can_scroll" format="boolean" />

</declare-styleable>
```

## API
- startEditModel
- closeEditModel
- cutSelect
- startOverwrite
- addFrame
- addFrames
- clearFrame
- deleteFrames
- replaceFrames
- startPlayAnim
- pausePlayAnim

## 实现原理

### 1. 滚动实现
> 本质是通过一个偏移量的变化，达到滚得效果
> 
> 使用Scroller+DecelerateInterpolator 到达一个简单的阻尼效果

### 2. 缩放声波
> ScaleGestureDetector + 计算展示UI

### 3. 覆盖模式实现
> 剪切中线以后的数据（触发剪切）,然后开始末端添加数据

### 4. 替换整段实现
> 剪切选中部分数据，然后插入数据
> 
> 文件部分操作，需要自行完成

### 5. 如何刚好颜色一半一半
> 一开始我的做法，是计算矩形的位置，给2边的矩形用不同的画笔，但是会出现，中间的矩形没有全部移过去的时候，不会变色，效果不佳。
> 
> 后面的使用方法是：给左边一半画一个大矩形，使用画笔的PorterDuffXfermode的SRC_ATOP方式，只绘制重叠部分达到完美的一般

## DEMO已实现功能
- [X] [添加DEMO](demo-record): 真实录音，剪切，播放
  - [X] 录音
  - [X] 剪切
  - [X] 播放
- [X] [添加DEMO](demo-record): 覆盖录制  
- [X] 降低渲染计算时间,不用全部遍历所有数据，直接计算获取开始和结束的索引
## TODO(暂时不做了)
- [ ] 添加交互：覆盖录制选中区域
- [ ] 提高剪切后的显示的精准度
  - [ ] 如果要提高剪切后的显示精准度，那就需要每次剪切时间每次只是一个数据代表秒（如demo的一个矩形表示40ms）的倍数，否则会存在多矩形或者少矩形的情况
    - 暂时没有好的方法进行解决，目前只要在刚好整除的时候自定纠正过来
- [ ] 给移动剪切线的文字加上背景
