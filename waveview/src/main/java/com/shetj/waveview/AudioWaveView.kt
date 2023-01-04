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
import android.util.Log
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

    companion object{
        const val TAG = "AudioWaveView"
    }

    protected val cirWidth = dip2px(5f) //圆大小
    protected val touchRec = dip2px(12f) //触摸范围反馈
    protected var iconSize = dip2px(30f).toFloat() //触发自动滚动的
    protected var antoMoveLimit = dip2px(30f) //触发自动滚动的
    protected var topLineMargin = dip2px(30f).toFloat() //顶部的线距离 顶部的高度
    protected var bottomLineMargin = dip2px(30f).toFloat() //底部的线距离 底部的高度

    protected var mChangeListener: OnChangeListener? = null
    protected var mDuration: Long = 0 //总时间
    protected var mCurrentPosition: Long = 0 //中线代表的进度
    protected var mStartTime: Long = 0 //剪切开始时间
    protected var mEndTime: Long = 0 //剪切结束时间
    protected var mAnima: ValueAnimator? = null
    protected var mPlayAnima: ValueAnimator? = null

    protected var mLevel = 10  //声波分level个级别
    protected var mOneSecondSize = 25f  //一秒声音的样本个数

    //每一秒25个
    protected val mSecondWidth: Float
        get() {
            return mOneSecondSize * (mRectWidth + mRectSpace)
        }

    //边界 屏幕宽度+音频长度
    protected var mHalfEmptyLength = 540
    protected val mContentLength: Float  //总长度
        get() {
            return mFrameArray.getSize() * (mRectWidth + mRectSpace)
        }

    protected var mRectStart = 0f //重复计算矩形的left,矩形宽度= rectEnd-rectStart
    protected var mRectEnd = 0f//重复计算矩形的right,矩形宽度= rectEnd-rectStart

    protected var mMinScale = 0.2f //最小放大缩小
    protected var mMaxScale = 1.2f //最大放大缩小

    protected val mRectVoiceLine = RectF()//右边波纹矩形的数据，10个矩形复用一个rectF
    protected var mRectWidth: Float = dip2px(2f).toFloat() //矩形的宽度
    protected var mRectSpace: Float = dip2px(2f).toFloat() //间隔宽度
    protected val mGravityScroller = Scroller(getContext(), DecelerateInterpolator()) //模拟滚动的Scroller

    protected var mFrameArray = FrameArray() //播放数据

    //刻度部分
    protected val mRectTimeLine = RectF()//右边波纹矩形的数据，10个矩形复用一个rectF
    protected var mRectTimeStart = 0f //重复计算矩形的left
    //region 画笔部分
    /**
     * Rect right paint 右边矩形的画笔
     */
    protected val mRectRightPaint = Paint().apply {
        color = Color.parseColor("#c8cad0")
    }

    /**
     * Rect left paint，左边矩形的画笔
     */
    protected val mRectLeftPaint = Paint().apply {
        color = Color.parseColor("#93b3ea")
    }

    protected val mIconPaint = Paint()

    /**
     * Center line paint 中间的画笔
     */
    protected val mCenterLinePaint = Paint().apply {
        color = Color.parseColor("#93b3ea")
        strokeWidth = dip2px(2f).toFloat()
    }

    private var bitmap: Bitmap? = null

    /**
     * Cut marker point：剪切指示器的画笔
     */
    protected val mCutMarkerPoint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    protected val mTextTimePaint = Paint().apply {
        color = Color.parseColor("#c8cad0")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    protected val mTextCutTimePaint = Paint().apply {
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
    private var mLastX: Float = 0.toFloat() // 用帮助手势滑动的帮助变量
    protected var mOffsetCutStartX = 0f //左边剪切线的偏移
    protected var mOffsetCutEndX = 0f //右边剪切线的偏移
    protected var mOffsetX: Float = 0f
    protected open fun getStartX(): Float {
        return mOffsetX * mScaleFactor + mHalfEmptyLength
    }

    protected open fun getLeftStartX(): Float {
        return getStartX() + mOffsetCutStartX * mScaleFactor
    }

    protected open fun getRightStartX(): Float {
        return getStartX() + mOffsetCutEndX * mScaleFactor
    }
    //endregion

    protected var mIsEditModel = false //是否是剪切模式
    protected var mIsTouchLeft = false //是否触摸左边
    protected var mIsTouchRight = false //是否触摸右边
    protected var mCanScroll = true //是否可以滑动，建议在进行动态添加操作的时候禁止滑动
    protected var mIsReplaceModel = false //是否是替换模式

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
            mScaleFactor = mMinScale.coerceAtLeast(mScaleFactor.coerceAtMost(mMaxScale))
            checkOffsetX()
            invalidate()
            return true
        }
    }

    protected val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    protected val mGestureListener = object : SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if (!mCanScroll) return false
            mOffsetX -= distanceX
            checkOffsetX()
            invalidate()
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
            if (!mCanScroll) return false
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
            if (!mCanScroll) return false
            mAnima?.cancel()
            return true
        }
    }
    protected val mGestureDetector = GestureDetector(getContext(), mGestureListener)

    open fun checkOffsetX() {
        if (mOffsetX > 0) {
            mOffsetX = 0f
        }
        if (mOffsetX < 0 && abs(mOffsetX) > (mContentLength)) {
            mOffsetX = (-mContentLength)
        }
        //通过偏移量得到时间
        if (mContentLength == 0F) return
        this.mCurrentPosition = (mDuration * abs(mOffsetX) / mContentLength).toLong()
        mChangeListener?.onUpdateCurrentPosition(mCurrentPosition)
    }

    init {
        obtainAttributes(context, attrs)
    }

    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AudioWaveView)
        topLineMargin = ta.getDimension(R.styleable.AudioWaveView_wv_top_line_margin, dip2px(30f).toFloat())
        bottomLineMargin = ta.getDimension(R.styleable.AudioWaveView_wv_bottom_line_margin, dip2px(30f).toFloat())
        mOneSecondSize = ta.getFloat(R.styleable.AudioWaveView_wv_one_second_rect_size, 25f)
        mLinePaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_top_bottom_line_color, Color.parseColor("#c8cad0"))

        mLinePaint.strokeWidth = ta.getDimension(R.styleable.AudioWaveView_wv_top_bottom_line_width, 3f)

        mLevel = ta.getInt(R.styleable.AudioWaveView_wv_rect_level, 10)
        mRectSpace = ta.getDimension(R.styleable.AudioWaveView_wv_rect_space, dip2px(2f).toFloat())
        mRectWidth = ta.getDimension(R.styleable.AudioWaveView_wv_rect_width, dip2px(2f).toFloat())
        mRectRightPaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_rect_right_color, Color.parseColor("#c8cad0"))
        mRectLeftPaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_rect_left_color, Color.parseColor("#93b3ea"))
        mCenterLinePaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_center_line_color, Color.parseColor("#93b3ea"))
        mCenterLinePaint.strokeWidth =
            ta.getDimension(R.styleable.AudioWaveView_wv_center_line_width, dip2px(2f).toFloat())
        iconSize = ta.getDimension(R.styleable.AudioWaveView_wv_cut_icon_size, dip2px(30f).toFloat())
        bitmap =
            ta.getDrawable(R.styleable.AudioWaveView_wv_cut_icon)?.toBitmap(iconSize.toInt(), iconSize.toInt())
        mCutMarkerPoint.color = ta.getColor(R.styleable.AudioWaveView_wv_cut_line_color, Color.RED)
        mCutMarkerPoint.strokeWidth = ta.getDimension(R.styleable.AudioWaveView_wv_cut_line_width, 3f)
        mTextCutTimePaint.color = ta.getColor(R.styleable.AudioWaveView_wv_cut_time_text_color, Color.RED)
        mTextCutTimePaint.textSize = ta.getDimension(R.styleable.AudioWaveView_wv_cut_time_text_size, 18f)
        mSelectedBGPaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_cut_select_color, Color.parseColor("#33FFBB22"))
        mTextTimePaint.color =
            ta.getColor(R.styleable.AudioWaveView_wv_time_progress_text_color, Color.parseColor("#c8cad0"))
        mTextTimePaint.textSize = ta.getDimension(R.styleable.AudioWaveView_wv_time_progress_text_size, 18f)
        mCanScroll = ta.getBoolean(R.styleable.AudioWaveView_wv_can_scroll, true)
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
        mHalfEmptyLength = (right - left) / 2
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
        this.mCanScroll = canScroll
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
        mOffsetX += -(mRectWidth + mRectSpace)
        this.mDuration = duration
        checkOffsetX()
        postInvalidate()
    }

    /**
     * Scroll to end
     * 快速到底不
     */
    open fun scrollToEnd() {
        mAnima?.cancel()
        mAnima = ObjectAnimator.ofFloat(mOffsetX, -mContentLength.toFloat()).also { an ->
            an.duration = 50
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                mOffsetX = changeSize
                checkOffsetX()
                postInvalidateOnAnimation()
            }
            an.start()
        }
    }

    /**
     * Clear frame
     * 情况
     */
    open fun clearFrame() {
        mDuration = 0
        mIsEditModel = false
        mFrameArray.reset()
        mStartTime = 0
        mEndTime = 0
        mOffsetX = 0f
        mOffsetCutStartX = 0f
        mOffsetCutEndX = 0f
        postInvalidate()
    }


    /**
     * Start edit model
     * 开启编辑模式
     */
    open fun startEditModel() {
        mOffsetCutEndX = (-mOffsetX).coerceAtLeast(min(100f, mContentLength))
        mOffsetCutStartX = mOffsetCutEndX - min(100f, mContentLength)
        mIsEditModel = true
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
        mOffsetCutEndX = endTime?.coerceAtLeast(0)?.let {
            (it.times(mContentLength) / mDuration).coerceAtLeast(min(100f, mContentLength))
        } ?: kotlin.run { (-mOffsetX).coerceAtLeast(min(100f, mContentLength)) }

        mOffsetCutStartX = startTime?.coerceAtLeast(0)?.let {
            (it.times(mContentLength) / mDuration)
        } ?: kotlin.run { mOffsetCutEndX - min(100f, mContentLength) }
        mIsEditModel = true
        updateSelectTimePosition()
        invalidate()
    }

    /**
     * 获取剪切模式是的开始时间
     * @return
     */
    open fun getCutStartTime(): Long {
        if (mIsEditModel) {
            return mStartTime
        }
        return 0
    }

    /**
     * 获取剪切模式是的结束时间
     * @return
     */
    open fun getCutEndTime(): Long {
        if (mIsEditModel) {
            return mEndTime
        }
        return 0
    }

    /**
     * Close edit model
     * 关闭编辑模式
     */
    open fun closeEditModel() {
        mIsEditModel = false
        invalidate()
    }

    /**
     * Get frames
     * 获取所有的frames
     * @return
     */
    open fun getFrames(): ArrayList<Float> {
        return mFrameArray.get()
    }


    open fun getDuration(): Long {
        return mDuration
    }


    /**
     * Get index by time
     *
     * @param time 时间
     * @return 当前时间的在FrameArray的位置
     */
    open fun getIndexByTime(time: Long): Int {
        return (time * mOneSecondSize / 1000 + 0.5).toInt()
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
        mDuration += frameDuration
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
        if (mDuration < endTime) {
            throw IllegalArgumentException(" need endTime($endTime) < duration($mDuration)  ")
        }
        mDuration -= (endTime - startTime)
        val size = mDuration * mOneSecondSize / 1000
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
        if (!mIsEditModel) return
        val isCutOk = mChangeListener?.onCutAudio(mStartTime, mEndTime) ?: true
        if (isCutOk) {
            mDuration -= (mEndTime - mStartTime)
            val size = mDuration * mOneSecondSize / 1000
            val delSize = mFrameArray.getSize() - size
            deleteFrames(mStartTime, delSize.toInt())
            checkOffsetX()
            mStartTime = mEndTime
            closeEditModel()
        }
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
            if (mIsEditModel) { //如果是标记模式，需要判断是不是进行左右切割线操作
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mLastX = event.x
                        val x = event.x.toInt()
                        mIsTouchLeft = abs(x - getLeftStartX()) < touchRec
                        mIsTouchRight = abs(x - getRightStartX()) < touchRec
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val moveX = (event.x - mLastX) / (mScaleFactor * 2)
                        mLastX = event.x
                        if (moveX > 0) {
                            //右滑动
                            if (mIsTouchLeft && mIsTouchRight) {
                                mOffsetCutEndX = (mOffsetCutEndX + moveX).coerceAtLeast(mOffsetCutStartX)
                                    .coerceAtMost(mContentLength.toFloat())
                                mIsTouchLeft = false
                            } else if (mIsTouchLeft) {
                                mOffsetCutStartX =
                                    (mOffsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(mOffsetCutEndX)
                            } else if (mIsTouchRight) {
                                mOffsetCutEndX = (mOffsetCutEndX + moveX).coerceAtLeast(mOffsetCutStartX)
                                    .coerceAtMost(mContentLength.toFloat())
                                if (mLastX > width - antoMoveLimit) {
                                    mOffsetX = (mOffsetX - 5).coerceAtMost(0f)
                                }
                            }
                            updateSelectTimePosition()
                            invalidate()
                        } else {
                            //左滑动
                            if (mIsTouchLeft && mIsTouchRight) {
                                mOffsetCutStartX =
                                    (mOffsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(mOffsetCutEndX)
                                mIsTouchRight = false
                            } else if (mIsTouchLeft) {
                                mOffsetCutStartX =
                                    (mOffsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(mOffsetCutEndX)
                                if (mLastX < antoMoveLimit) {
                                    mOffsetX = (mOffsetX + 5).coerceAtLeast(-mContentLength.toFloat())
                                }
                            } else if (mIsTouchRight) {
                                mOffsetCutEndX = (mOffsetCutEndX + moveX).coerceAtLeast(mOffsetCutStartX)
                                    .coerceAtMost(mContentLength.toFloat())
                            }
                            updateSelectTimePosition()
                            invalidate()
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        mIsTouchLeft = false
                        mIsTouchRight = false
                        invalidate()
                    }
                }
            }
            if (mIsTouchLeft || mIsTouchRight) return true
            //如果不是左右切割线，那就是给手势，让他去做滚动操作，达到一个scrollview的效果
            mGestureDetector.onTouchEvent(event)
        }
    }

    protected open fun updateSelectTimePosition() {
        this.mStartTime = (mDuration * abs(mOffsetCutStartX) / mContentLength).toLong()
        this.mEndTime = (mDuration * abs(mOffsetCutEndX) / mContentLength).toLong()
        mChangeListener?.onUpdateCutPosition(mStartTime, mEndTime)
    }

    protected val mIconRect = Rect(0, 0, 0, 0) //用来

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        val halfWidth = (width / 2).toFloat()
        val halfHeight = (height / 2).toFloat()
        canvas?.apply {


            //先判断一共需要画的刻度是多少
            val time = (mContentLength / mSecondWidth).toInt()
            // 可能没有内容，我们也要画为了美观
            val i = (width / (mSecondWidth * mScaleFactor)).toInt()

            val mFontMetrics = mTextTimePaint.fontMetrics
            val mTop = mFontMetrics.top
            val mBottom = mFontMetrics.bottom
            val baseLineY = topLineMargin / 2 - mTop / 2 - mBottom / 2 //基线中间点的y轴计算公式
            // 先不管至少执行3次，后面可以总结优化下
            repeat(i + 3 + time) {

                mRectTimeStart = getStartX() + it * mSecondWidth * mScaleFactor
                mRectTimeLine.left = mRectTimeStart
                mRectTimeLine.top = topLineMargin - 10f
                mRectTimeLine.right = mRectTimeStart + 2f
                mRectTimeLine.bottom = topLineMargin

                if (mRectTimeStart >= 0 && mRectTimeStart <= width) {
                    //只有屏幕之内的刻度才画出来
                    drawRoundRect(mRectTimeLine, 1f, 1f, mLinePaint)
                    if (mScaleFactor > 0.5) {
                        drawText(it.covertToTime(), mRectTimeStart, baseLineY, mTextTimePaint)
                    } else {
                        if (it % 2 == 0) {
                            drawText(it.covertToTime(), mRectTimeStart, baseLineY, mTextTimePaint)
                        }
                    }
                }
            }
            // 画声音播放矩形
            mFrameArray.get().forEachIndexed { index, value ->

                mRectStart =
                    getStartX() + index * mRectWidth * mScaleFactor + mRectSpace * mScaleFactor * index
                mRectEnd =
                    getStartX() + (index + 1) * mRectWidth * mScaleFactor + mRectSpace * mScaleFactor * index
                if (mRectEnd <= width + 20 || mRectStart >= -10) {
                    val halfRectHeight = min(value / mLevel.toFloat(), 1f) / 2 * halfHeight //矩形的半高
                    mRectVoiceLine.left = mRectStart
                    mRectVoiceLine.top = halfHeight - halfRectHeight
                    mRectVoiceLine.right = mRectEnd
                    mRectVoiceLine.bottom = halfHeight + halfRectHeight
                    if (mRectEnd < halfWidth) {
                        drawRoundRect(mRectVoiceLine, 6f, 6f, mRectLeftPaint)
                    } else {
                        drawRoundRect(mRectVoiceLine, 6f, 6f, mRectRightPaint)
                    }
                }
            }

            // 上线
            drawLine(
                min(mOffsetX * mScaleFactor, 0f),
                topLineMargin,
                min(getStartX() + mContentLength * mScaleFactor + mHalfEmptyLength, width.toFloat()),
                topLineMargin,
                mLinePaint
            )
            // 下线
            drawLine(
                min(mOffsetX * mScaleFactor, 0f),
                height - bottomLineMargin,
                min(getStartX() + mContentLength * mScaleFactor + mHalfEmptyLength, width.toFloat()),
                height - bottomLineMargin,
                mLinePaint
            )

            // 画中线
            drawLine(halfWidth, 2f, halfWidth, height - bottomLineMargin, mCenterLinePaint)
            drawCircle(halfWidth, (cirWidth / 2).toFloat() + 8f, cirWidth.toFloat(), mCenterLinePaint)


            if (mIsEditModel) {

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
                    mCutMarkerPoint
                )
                if (mIsTouchLeft) {
                    //左边的文字进度
                    drawText(getStartTs(), getLeftStartX(), baseLineY, mTextCutTimePaint)
                }


                // 画结束右线
                drawLine(
                    getRightStartX(),
                    topLineMargin,
                    getRightStartX(),
                    height - bottomLineMargin,
                    mCutMarkerPoint
                )
                if (mIsTouchRight) {
                    //右边文字的进度
                    drawText(getEndTs(), getRightStartX(), baseLineY, mTextCutTimePaint)
                }



                mIconRect.set(
                    (getLeftStartX() - iconSize / 2).toInt(),
                    (height - bottomLineMargin - iconSize / 2).toInt(),
                    (getLeftStartX() + iconSize / 2).toInt(),
                    (height - bottomLineMargin + iconSize / 2).toInt()
                )
                bitmap?.let { drawBitmap(it, null, mIconRect, mIconPaint) }
                mIconRect.set(
                    (getRightStartX() - iconSize / 2).toInt(),
                    (height - bottomLineMargin - iconSize / 2).toInt(),
                    (getRightStartX() + iconSize / 2).toInt(),
                    (height - bottomLineMargin + iconSize / 2).toInt()
                )
                bitmap?.let { drawBitmap(it, null, mIconRect, mIconPaint) }
            }


        }
    }

    protected open fun getStartTs() =
        (mDuration * abs(mOffsetCutStartX) / mContentLength).toLong().covertToTimets()

    protected open fun getEndTs() = (mDuration * abs(mOffsetCutEndX) / mContentLength).toLong().covertToTimets()


    protected open fun startAnimaFling() {
        mAnima?.cancel()
        val finalX = mGravityScroller.finalX
        val duration = mGravityScroller.duration
        val offsetX1 = mOffsetX
        mAnima = ObjectAnimator.ofFloat(0.toFloat(), finalX.toFloat()).also { an ->
            an.duration = duration.toLong()
            an.interpolator = DecelerateInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                mOffsetX = offsetX1 + changeSize
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
    fun startPlayAnim(speed: Float = 1.0f) {
        mPlayAnima?.cancel()
        mPlayAnima = ObjectAnimator.ofFloat(mOffsetX, -mContentLength).also { an ->
            an.duration = ((mDuration - mCurrentPosition) / speed).toLong()
            an.interpolator = LinearInterpolator()
            an.addUpdateListener { animation ->
                val changeSize = animation.animatedValue as Float
                mOffsetX = changeSize
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
        mPlayAnima?.cancel()
    }

    fun isPlaying(): Boolean {
        return mPlayAnima?.isRunning ?: false
    }

    fun isEditModel() = mIsEditModel

    fun setListener(callBack: OnChangeListener) {
        this.mChangeListener = callBack
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