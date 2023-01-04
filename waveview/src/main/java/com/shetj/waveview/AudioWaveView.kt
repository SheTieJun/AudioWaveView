package com.shetj.waveview

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.min


/**
 * Audio wave view
 * 音频播放的view
 * 1. 支持放大缩小
 */
open class AudioWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    protected val cirWidth = dip2px(5f) //圆大小
    protected val touchRec = dip2px(12f) //触摸范围反馈
    protected var iconSize = dip2px(30f).toFloat() //触发自动滚动的
    protected var antoMoveLimit = dip2px(30f) //触发自动滚动的
    protected var topLineMargin = dip2px(30f).toFloat() //顶部的线距离 顶部的高度
    protected var bottomLineMargin = dip2px(30f).toFloat() //底部的线距离 底部的高度

    protected var changeListener: OnChangeListener? = null
    protected var duration: Long = 0 //总时间
    protected var currentPosition: Long = 0 //中线代表的进度
    protected var mStartTime: Long = 0 //剪切开始时间
    protected var mEndTime: Long = 0 //剪切结束时间
    protected var anima: ValueAnimator? = null
    protected var playAnima: ValueAnimator? = null

    protected var level = 10  //声波分level个级别
    protected var oneSecondSize = 25f  //一秒声音的样本个数

    //每一秒25个
    protected val secondWidth: Float
        get() {
            return oneSecondSize * (rectWidth + rectSpace)
        }

    //边界 屏幕宽度+音频长度
    protected var halfEmptyLength = 540
    protected val contentLength: Float  //总长度
        get() {
            return mFrameArray.getSize() * (rectWidth + rectSpace)
        }

    protected var rectStart = 0f //重复计算矩形的left,矩形宽度= rectEnd-rectStart
    protected var rectEnd = 0f//重复计算矩形的right,矩形宽度= rectEnd-rectStart

    protected var minScale = 0.2f //最小放大缩小
    protected var maxScale = 1.2f //最大放大缩小

    protected val rectVoiceLine = RectF()//右边波纹矩形的数据，10个矩形复用一个rectF
    protected var rectWidth: Float = dip2px(2f).toFloat() //矩形的宽度
    protected var rectSpace: Float = dip2px(2f).toFloat() //间隔宽度
    protected val mGravityScroller = Scroller(getContext(), DecelerateInterpolator()) //模拟滚动的Scroller

    protected var mFrameArray = FrameArray() //播放数据

    //刻度部分
    protected val rectTimeLine = RectF()//右边波纹矩形的数据，10个矩形复用一个rectF
    protected var rectTimeStart = 0f //重复计算矩形的left
    //region 画笔部分
    /**
     * Rect right paint 右边矩形的画笔
     */
    protected val rectRightPaint = Paint().apply {
        color = Color.parseColor("#c8cad0")
    }

    /**
     * Rect left paint，左边矩形的画笔
     */
    protected val rectLeftPaint = Paint().apply {
        color = Color.parseColor("#93b3ea")
    }

    protected val iconPaint = Paint()

    /**
     * Center line paint 中间的画笔
     */
    protected val centerLinePaint = Paint().apply {
        color = Color.parseColor("#93b3ea")
        strokeWidth = dip2px(2f).toFloat()
    }

    private var bitmap: Bitmap? = null

    /**
     * Cut marker point：剪切指示器的画笔
     */
    protected val cutMarkerPoint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    protected val textTimePaint = Paint().apply {
        color = Color.parseColor("#c8cad0")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    protected val textCutTimePaint = Paint().apply {
        color = Color.RED
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    /**
     * 选中区域的背景颜色
     */
    protected val mSelectedBGPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#33FFBB22")
    }

    /**
     * 上线/下线的画笔
     */
    protected val mLinePaint = Paint().apply {
        strokeWidth = 3f
        color = Color.parseColor("#c8cad0")
    }
    //endregion

    //region 用来计算是否绘制的相关偏移
    private var lastX: Float = 0.toFloat() // 用帮助手势滑动的帮助变量
    protected var offsetCutStartX = 0f //左边剪切线的偏移
    protected var offsetCutEndX = 0f //右边剪切线的偏移
    protected var offsetX: Float = 0f
    protected open fun getStartX(): Float {
        return offsetX * mScaleFactor + halfEmptyLength
    }

    protected open fun getLeftStartX(): Float {
        return getStartX() + offsetCutStartX * mScaleFactor
    }

    protected open fun getRightStartX(): Float {
        return getStartX() + offsetCutEndX * mScaleFactor
    }
    //endregion

    protected var isEditModel = false //是否是剪切模式
    protected var isTouchLeft = false //是否触摸左边
    protected var isTouchRight = false //是否触摸右边
    protected var canScroll = true //是否可以滑动，建议在进行动态添加操作的时候禁止滑动

    protected fun dip2px(dpVal: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpVal, context.resources.displayMetrics
        ).toInt()
    }

    protected var mScaleFactor = 1f
    protected val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = minScale.coerceAtLeast(mScaleFactor.coerceAtMost(maxScale))
            checkOffsetX()
            invalidate()
            return true
        }
    }

    protected val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    protected val ongestureListener = object : SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (!canScroll) return false
            offsetX -= distanceX
            checkOffsetX()
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (!canScroll) return false
            mGravityScroller.fling(
                0,
                0,
                velocityX.toInt(),
                velocityY.toInt(),
                Int.MIN_VALUE,
                Int.MAX_VALUE,
                Int.MIN_VALUE,
                Int.MAX_VALUE
            )
            startAnimaFling()
            return true
        }


        override fun onDown(e: MotionEvent?): Boolean {
            if (!canScroll) return false
            anima?.cancel()
            return true
        }
    }
    protected val gestureDetector = GestureDetector(getContext(), ongestureListener)

    open fun checkOffsetX() {
        if (offsetX > 0) {
            offsetX = 0f
        }
        if (offsetX < 0 && abs(offsetX) > (contentLength)) {
            offsetX = (-contentLength)
        }
        //通过偏移量得到时间
        if (contentLength == 0F) return
        this.currentPosition = (duration * abs(offsetX) / contentLength).toLong()
        changeListener?.onUpdateCurrentPosition(currentPosition)
    }

    init {
        obtainAttributes(context, attrs)
    }

    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AudioWaveView)
        topLineMargin = ta.getDimension(R.styleable.AudioWaveView_wv_top_line_margin, dip2px(30f).toFloat())
        bottomLineMargin = ta.getDimension(R.styleable.AudioWaveView_wv_bottom_line_margin, dip2px(30f).toFloat())
        oneSecondSize = ta.getFloat(R.styleable.AudioWaveView_wv_one_second_rect_size, 25f)
        mLinePaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_top_bottom_line_color, Color.parseColor("#c8cad0"))

        mLinePaint.strokeWidth = ta.getDimension(R.styleable.AudioWaveView_wv_top_bottom_line_width, 3f)

        level = ta.getInt(R.styleable.AudioWaveView_wv_rect_level, 10)
        rectSpace = ta.getDimension(R.styleable.AudioWaveView_wv_rect_space, dip2px(2f).toFloat())
        rectWidth = ta.getDimension(R.styleable.AudioWaveView_wv_rect_width, dip2px(2f).toFloat())
        rectRightPaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_rect_right_color, Color.parseColor("#c8cad0"))
        rectLeftPaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_rect_left_color, Color.parseColor("#93b3ea"))
        centerLinePaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_center_line_color, Color.parseColor("#93b3ea"))
        centerLinePaint.strokeWidth =
            ta.getDimension(R.styleable.AudioWaveView_wv_center_line_width, dip2px(2f).toFloat())
        iconSize = ta.getDimension(R.styleable.AudioWaveView_wv_cut_icon_size, dip2px(30f).toFloat())
        bitmap =
            ta.getDrawable(R.styleable.AudioWaveView_wv_cut_icon)?.toBitmap(iconSize.toInt(), iconSize.toInt())
        cutMarkerPoint.color = ta.getColor(R.styleable.AudioWaveView_wv_cut_line_color, Color.RED)
        cutMarkerPoint.strokeWidth = ta.getDimension(R.styleable.AudioWaveView_wv_cut_line_width, 3f)
        textCutTimePaint.color = ta.getColor(R.styleable.AudioWaveView_wv_cut_time_text_color, Color.RED)
        textCutTimePaint.textSize = ta.getDimension(R.styleable.AudioWaveView_wv_cut_time_text_size, 18f)
        mSelectedBGPaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_cut_select_color, Color.parseColor("#33FFBB22"))
        textTimePaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_time_progress_text_color, Color.parseColor("#c8cad0"))
        textTimePaint.textSize = ta.getDimension(R.styleable.AudioWaveView_wv_time_progress_text_size, 18f)
        canScroll = ta.getBoolean(R.styleable.AudioWaveView_wv_can_scroll, true)
        ta.recycle()
    }

    //测量
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureWidth(widthMeasureSpec)
        val height = measureHeight(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        halfEmptyLength = (right - left) / 2
    }


    private fun measureHeight(measureSpec: Int, defaultSize: Int = suggestedMinimumHeight): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = defaultSize + paddingTop + paddingBottom
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize)
            }
        }
        result = result.coerceAtLeast(suggestedMinimumHeight)
        return result
    }

    private fun measureWidth(measureSpec: Int, defaultSize: Int = suggestedMinimumWidth): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = defaultSize + paddingLeft + paddingRight
            if (specMode == MeasureSpec.AT_MOST) {
                result = result.coerceAtMost(specSize)
            }
        }
        result = result.coerceAtLeast(suggestedMinimumWidth)
        return result
    }


    /**
     * Set can scroll
     *
     * @param canScroll 是否可以通过手势进行滚动
     * @param toEnd 是否直接滚动底部
     */
    open fun setCanScroll(canScroll: Boolean, toEnd: Boolean = false) {
        this.canScroll = canScroll
        if (toEnd) {
            scrollToEnd()
        }
    }

    /**
     * Add frame
     * 添加声音
     * @param frame
     * @param duration
     */
    open fun addFrame(frame: Float, duration: Long) {
        mFrameArray.add(frame)
        offsetX += -(rectWidth + rectSpace)
        this.duration = duration
        checkOffsetX()
        postInvalidate()
    }

    /**
     * Scroll to end
     * 快速到底不
     */
    open fun scrollToEnd() {
        anima?.cancel()
        anima = ObjectAnimator.ofFloat(offsetX, -contentLength.toFloat()).also { an ->
            an.duration = 50
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }

    open fun clearFrame() {
        mFrameArray.reset()
        offsetX = 0f
        postInvalidate()
    }


    /**
     * Start edit model
     * 开启编辑模式
     */
    open fun startEditModel() {
        offsetCutEndX = (-offsetX).coerceAtLeast(min(100f, contentLength))
        offsetCutStartX = offsetCutEndX - min(100f, contentLength)
        isEditModel = true
        updateSelectTimePosition()
        invalidate()
    }

    /**
     * Start edit model
     *
     * @param startTime
     * @param endTime
     */
    open fun startEditModel(startTime: Long? = null, endTime: Long? = null) {
        if ((startTime ?: 0) > (endTime ?: 0)) {
            throw IllegalArgumentException("startTime($startTime) > endTime($endTime) ")
        }
        offsetCutEndX = endTime?.coerceAtLeast(0)?.let {
            (it.times(contentLength) / duration).coerceAtLeast(min(100f, contentLength))
        } ?: kotlin.run { (-offsetX).coerceAtLeast(min(100f, contentLength)) }

        offsetCutStartX = startTime?.coerceAtLeast(0)?.let {
            (it.times(contentLength) / duration)
        } ?: kotlin.run { offsetCutEndX - min(100f, contentLength) }
        isEditModel = true
        updateSelectTimePosition()
        invalidate()
    }

    open fun getFrames(): ArrayList<Float> {
        return mFrameArray.get()
    }


    /**
     * Get index by time
     *
     * @param time 时间
     * @return 当前时间的在FrameArray的位置
     */
    open fun getIndexByTime(time:Long):Int{
        return  (time * oneSecondSize / 1000 + 0.5).toInt()
    }
    /**
     * Add frames
     *
     * @param index 在什么位置添加
     * @param newFrameArray 新的声音的数据
     * @param frameDuration 新的声音的时间
     * @param editModel 是否进入编辑模式，选择
     */
    open fun addFrames(index: Int, newFrameArray: FrameArray, frameDuration: Long, editModel: Boolean = true) {
        mFrameArray.add(index, newFrameArray.get())
        duration += frameDuration
    }

    /**
     * Replace frames
     *
     * @param startTime  被替换部分开始的时间
     * @param endTime  被替换部分结束的时间
     * @param newFrameArray 新的部分
     * @param newFrameDuration  新的部分的宗师级
     * @param editModel 替换后是否展示时间
     */
    open fun replaceFrames(
        startTime: Long,
        endTime: Long,
        newFrameArray: FrameArray,
        newFrameDuration: Long,
        editModel: Boolean = true
    ) {
        if (duration < endTime) {
            throw IllegalArgumentException(" need endTime($endTime) < duration($duration)  ")
        }
        duration -= (endTime - startTime)
        val size = duration * oneSecondSize / 1000
        val delSize = mFrameArray.getSize() - size
        mStartTime = startTime
        val startIndex = getIndexByTime(startTime)
        mFrameArray.delete(startIndex, startIndex + delSize.toInt())
        addFrames(startIndex, newFrameArray, newFrameDuration)
        if (editModel) {
            startEditModel(mStartTime, mStartTime + newFrameDuration)
        }
    }


    /**
     * Cut select
     * 剪切掉选中部分
     */
    open fun cutSelect() {
        if (!isEditModel) return
        val isCutOk = changeListener?.onCutAudio(mStartTime, mEndTime) ?: true
        if (isCutOk) {
            duration -= (mEndTime - mStartTime)
            val size = duration * oneSecondSize / 1000
            val delSize = mFrameArray.getSize() - size
            deleteFrames(mStartTime, delSize.toInt())
            checkOffsetX()
            mStartTime = mEndTime
            closeEdit()
        }
    }

    /**
     * 获取剪切模式是的开始时间
     * @return
     */
    open fun getCutStartTime(): Long {
        if (isEditModel) {
            return mStartTime
        }
        return 0
    }

    /**
     * 获取剪切模式是的结束时间
     * @return
     */
    open fun getCutEndTime(): Long {
        if (isEditModel) {
            return mEndTime
        }
        return 0
    }

    open fun closeEdit() {
        isEditModel = false
        invalidate()
    }

    protected open fun deleteFrames(startTime: Long, delSize: Int) {
        val startIndex = getIndexByTime(time = startTime)
        mFrameArray.delete(startIndex, startIndex + delSize)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isPlaying()) return true //如果在播放，不进行手势
        return if (event.pointerCount >= 2) {
            //放大缩小操作
            mScaleDetector.onTouchEvent(event)
            true
        } else {
            if (isEditModel) { //如果是标记模式，需要判断是不是进行左右切割线操作
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.x
                        val x = event.x.toInt()
                        isTouchLeft = abs(x - getLeftStartX()) < touchRec
                        isTouchRight = abs(x - getRightStartX()) < touchRec
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val moveX = (event.x - lastX) / (mScaleFactor * 2)
                        lastX = event.x
                        if (moveX > 0) {
                            //右滑动
                            if (isTouchLeft && isTouchRight) {
                                offsetCutEndX = (offsetCutEndX + moveX).coerceAtLeast(offsetCutStartX)
                                    .coerceAtMost(contentLength.toFloat())
                                isTouchLeft = false
                            } else if (isTouchLeft) {
                                offsetCutStartX = (offsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(offsetCutEndX)
                            } else if (isTouchRight) {
                                offsetCutEndX = (offsetCutEndX + moveX).coerceAtLeast(offsetCutStartX)
                                    .coerceAtMost(contentLength.toFloat())
                                if (lastX > width - antoMoveLimit) {
                                    offsetX = (offsetX - 5).coerceAtMost(0f)
                                }
                            }
                            updateSelectTimePosition()
                            invalidate()
                        } else {
                            //左滑动
                            if (isTouchLeft && isTouchRight) {
                                offsetCutStartX = (offsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(offsetCutEndX)
                                isTouchRight = false
                            } else if (isTouchLeft) {
                                offsetCutStartX = (offsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(offsetCutEndX)
                                if (lastX < antoMoveLimit) {
                                    offsetX = (offsetX + 5).coerceAtLeast(-contentLength.toFloat())
                                }
                            } else if (isTouchRight) {
                                offsetCutEndX = (offsetCutEndX + moveX).coerceAtLeast(offsetCutStartX)
                                    .coerceAtMost(contentLength.toFloat())
                            }
                            updateSelectTimePosition()
                            invalidate()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        isTouchLeft = false
                        isTouchRight = false
                        invalidate()
                    }
                }
            }
            if (isTouchLeft || isTouchRight) return true
            //如果不是左右切割线，那就是给手势，让他去做滚动操作，达到一个scrollview的效果
            gestureDetector.onTouchEvent(event)
        }
    }

    protected open fun updateSelectTimePosition() {
        this.mStartTime = (duration * abs(offsetCutStartX) / contentLength).toLong()
        this.mEndTime = (duration * abs(offsetCutEndX) / contentLength).toLong()
        changeListener?.onUpdateCutPosition(mStartTime, mEndTime)
    }

    private val fontMetrics = textTimePaint.fontMetrics
    private val top = fontMetrics.top
    private val bottom = fontMetrics.bottom
    private val baseLineY = topLineMargin / 2 - top / 2 - bottom / 2 //基线中间点的y轴计算公式


    protected val iconRect = Rect(0, 0, 0, 0)

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        val halfWidth = (width / 2).toFloat()
        val halfHeight = (height / 2).toFloat()
        canvas?.apply {


            //先判断一共需要画的刻度是多少
            val time = (contentLength / secondWidth).toInt()
            // 可能没有内容，我们也要画为了美观
            val i = (width / (secondWidth * mScaleFactor)).toInt()
            // 先不管至少执行3次，后面可以总结优化下
            repeat(i + 3 + time) {

                rectTimeStart = getStartX() + it * secondWidth * mScaleFactor
                rectTimeLine.left = rectTimeStart
                rectTimeLine.top = topLineMargin - 10f
                rectTimeLine.right = rectTimeStart + 2f
                rectTimeLine.bottom = topLineMargin

                if (rectTimeStart >= 0 && rectTimeStart <= width) {
                    //只有屏幕之内的刻度才画出来
                    drawRoundRect(rectTimeLine, 1f, 1f, mLinePaint)
                    if (mScaleFactor > 0.5) {
                        drawText(it.covertToTime(), rectTimeStart, baseLineY, textTimePaint)
                    } else {
                        if (it % 2 == 0) {
                            drawText(it.covertToTime(), rectTimeStart, baseLineY, textTimePaint)
                        }
                    }
                }
            }
            // 画声音播放矩形
            mFrameArray.get().forEachIndexed { index, value ->

                rectStart =
                    getStartX() + index * rectWidth * mScaleFactor + rectSpace * mScaleFactor * index
                rectEnd =
                    getStartX() + (index + 1) * rectWidth * mScaleFactor + rectSpace * mScaleFactor * index
                if (rectEnd <= width + 20 || rectStart >= -10) {
                    val halfRectHeight = min(value / level.toFloat(), 1f) / 2 * halfHeight //矩形的半高
                    rectVoiceLine.left = rectStart
                    rectVoiceLine.top = halfHeight - halfRectHeight
                    rectVoiceLine.right = rectEnd
                    rectVoiceLine.bottom = halfHeight + halfRectHeight
                    if (rectEnd < halfWidth) {
                        drawRoundRect(rectVoiceLine, 6f, 6f, rectLeftPaint)
                    } else {
                        drawRoundRect(rectVoiceLine, 6f, 6f, rectRightPaint)
                    }
                }
            }

            // 上线
            drawLine(
                min(offsetX * mScaleFactor, 0f),
                topLineMargin,
                min(getStartX() + contentLength * mScaleFactor + halfEmptyLength, width.toFloat()),
                topLineMargin,
                mLinePaint
            )
            // 下线
            drawLine(
                min(offsetX * mScaleFactor, 0f),
                height - bottomLineMargin,
                min(getStartX() + contentLength * mScaleFactor + halfEmptyLength, width.toFloat()),
                height - bottomLineMargin,
                mLinePaint
            )

            // 画中线
            drawLine(halfWidth, 2f, halfWidth, height - bottomLineMargin, centerLinePaint)
            drawCircle(halfWidth, (cirWidth / 2).toFloat() + 8f, cirWidth.toFloat(), centerLinePaint)


            if (isEditModel) {

                // 画矩形
                drawRect(
                    getLeftStartX(),
                    topLineMargin,
                    getRightStartX(),
                    height - bottomLineMargin,
                    mSelectedBGPaint
                )

                // 画开始左线
                drawLine(
                    getLeftStartX(),
                    topLineMargin,
                    getLeftStartX(),
                    height - bottomLineMargin,
                    cutMarkerPoint
                )
                if (isTouchLeft) {
                    //左边的文字进度
                    drawText(getStartTs(), getLeftStartX(), baseLineY, textCutTimePaint)
                }


                // 画结束右线
                drawLine(
                    getRightStartX(),
                    topLineMargin,
                    getRightStartX(),
                    height - bottomLineMargin,
                    cutMarkerPoint
                )
                if (isTouchRight) {
                    //右边文字的进度
                    drawText(getEndTs(), getRightStartX(), baseLineY, textCutTimePaint)
                }



                iconRect.set(
                    (getLeftStartX() - iconSize / 2).toInt(),
                    (height - bottomLineMargin - iconSize / 2).toInt(),
                    (getLeftStartX() + iconSize / 2).toInt(),
                    (height - bottomLineMargin + iconSize / 2).toInt()
                )
                bitmap?.let { drawBitmap(it, null, iconRect, iconPaint) }
                iconRect.set(
                    (getRightStartX() - iconSize / 2).toInt(),
                    (height - bottomLineMargin - iconSize / 2).toInt(),
                    (getRightStartX() + iconSize / 2).toInt(),
                    (height - bottomLineMargin + iconSize / 2).toInt()
                )
                bitmap?.let { drawBitmap(it, null, iconRect, iconPaint) }
            }


        }
    }

    protected open fun getStartTs() = (duration * abs(offsetCutStartX) / contentLength).toLong().covertToTimets()

    protected open fun getEndTs() = (duration * abs(offsetCutEndX) / contentLength).toLong().covertToTimets()


    protected open fun startAnimaFling() {
        anima?.cancel()
        val finalX = mGravityScroller.finalX
        val duration = mGravityScroller.duration
        val offsetX1 = offsetX
        anima = ObjectAnimator.ofFloat(0.toFloat(), finalX.toFloat()).also { an ->
            an.duration = duration.toLong()
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = offsetX1 + changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }


    /**
     * Start play anim
     * 开始播放动画
     * @param speed 几倍速进行播放动画，默认1f
     */
    fun startPlayAnim(speed:Float = 1.0f) {
        playAnima?.cancel()
        playAnima = ObjectAnimator.ofFloat(offsetX, -contentLength).also { an ->
            an.duration = ((duration - currentPosition)/speed).toLong()
            an.interpolator = LinearInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                offsetX = changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }

    /**
     * Pause play anim
     * 暂停播放的动画
     */
    fun pausePlayAnim() {
        playAnima?.cancel()
    }

    fun isPlaying(): Boolean {
        return playAnima?.isRunning ?: false
    }

    fun setListener(callBack: OnChangeListener) {
        this.changeListener = callBack
    }

    interface OnChangeListener {

        /**
         * On update current position
         * 更新当前的位置,中线表示的时间
         * @param position
         */
        fun onUpdateCurrentPosition(position: Long)

        /**
         * On update cut position
         * 更新当前选中剪切的位置
         * @param startPosition
         * @param endPosition
         */
        fun onUpdateCutPosition(startPosition: Long, endPosition: Long)

        /**
         * On cut audio
         * 触发剪切
         * @param startTime
         * @param endTime
         * @return 是否剪切成功
         */
        fun onCutAudio(startTime: Long, endTime: Long): Boolean

    }
}