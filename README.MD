## AudioWaveView

仿喜马拉雅录音剪切的view

- [X] 声音波纹
- [X] 编辑模式
- [X] 整段替换声波
- [X] 缩放声波
- [X] 覆盖模式实现

| 正常模式                  | 编辑模式                  |     |
|-----------------------|-----------------------|-----|
| ![](/image/正常模式.webp) | ![](/image/编辑模式.webp) |     |

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
    <!--    矩形分几个级别，默认10种高度的矩形-->
    <attr name="wv_rect_level" format="integer" />
    <!--    矩形的宽度-->
    <attr name="wv_rect_width" format="dimension" />
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

## 实现原理

### 1. 滚动实现
> 本质是通过一个偏移量的变化，达到滚得效果
> 使用Scroller+DecelerateInterpolator 到达一个简单的阻尼效果

### 2. 缩放声波
> ScaleGestureDetector + 计算展示UI

### 3. 覆盖模式实现
> 剪切中线以后的数据（触发剪切）,然后开始添加数据


## TODO
- [ ] 是否支持PCM
- [ ] 尝试支持添加交互：覆盖录制选中区域