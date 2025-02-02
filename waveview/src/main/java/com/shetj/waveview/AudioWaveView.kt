package com.shetj.waveview

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode.SRC_ATOP
import android.graphics.PorterDuffXfermode
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

open class AudioWaveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "AudioWaveView"
    }

    protected val cirWidth = dip2px(5f) //圆大小
    protected val touchRec = dip2px(12f) //触摸范围反馈
    protected var iconSize = dip2px(30f).toFloat() //触发自动滚动的
    protected var antoMoveLimit = dip2px(30f) //触发自动滚动的
    protected var topLineMargin = dip2px(30f).toFloat() //顶部的线距离 顶部的高度
    protected var bottomLineMargin = dip2px(30f).toFloat() //底部的线距离 底部的高度
    protected var mRectCornerRadius = dip2px(2f).toFloat()

    protected var mChangeListener: OnChangeListener? = null
    protected var mDuration: Long = 0 //总时间
    protected var mCurrentPosition: Long = 0 //中线代表的进度
    protected var mCutStartTime: Long = 0 //剪切开始时间
    protected var mCutEndTime: Long = 0 //剪切结束时间
    protected var mAnima: ValueAnimator? = null
    protected var mPlayAnima: ValueAnimator? = null

    protected var mLevel = 10  //声波分level个级别
    protected var mOneSecondSize = 25f  //一秒声音的样本个数
    protected var mOneRectTime = 40f

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

    protected var mMinScale = 0.1f //最小放大缩小
    protected var mMaxScale = 1.4f //最大放大缩小

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
        xfermode = PorterDuffXfermode(SRC_ATOP)
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
    open var isShowCenterLine: Boolean = true
        set(value) {
            field = value
            invalidate()
        }
    open var isShowTopBottomLine: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    protected open fun getStartX(): Float {
        return mOffsetX * mScaleFactor + mHalfEmptyLength
    }

    protected open fun getLeftLineStartX(): Float {
        return getStartX() + mOffsetCutStartX * mScaleFactor
    }

    protected open fun getRightLineStartX(): Float {
        return getStartX() + mOffsetCutEndX * mScaleFactor
    }
    //endregion

    protected var mIsEditModel = false //是否是剪切模式
    protected var mIsTouchLeft = false //是否触摸左边
    protected var mIsTouchRight = false //是否触摸右边
    protected var mCanScroll = true //是否可以滑动，建议在进行动态添加操作的时候禁止滑动

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
            mChangeListener?.onUpdateScale(mScaleFactor)
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
        this.mCurrentPosition = (mDuration * abs(mOffsetX) / mContentLength).toLong() / 10 * 10
        mChangeListener?.onUpdateCurrentPosition(mCurrentPosition, mDuration)
    }

    init {
        obtainAttributes(context, attrs)
    }

    private fun obtainAttributes(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.AudioWaveView)
        topLineMargin = ta.getDimension(R.styleable.AudioWaveView_wv_top_line_margin, dip2px(30f).toFloat())
        bottomLineMargin = ta.getDimension(R.styleable.AudioWaveView_wv_bottom_line_margin, dip2px(30f).toFloat())
        mOneSecondSize = ta.getFloat(R.styleable.AudioWaveView_wv_one_second_rect_size, 25f)

        mOneRectTime = 1000 / mOneSecondSize

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
        mScaleFactor = ta.getFloat(R.styleable.AudioWaveView_wv_rect_scale, 1.0f).coerceAtLeast(mMinScale)
            .coerceAtMost(mMaxScale)
        mRectCornerRadius = ta.getDimension(R.styleable.AudioWaveView_wv_rect_corner_radius, 6f)
        isShowCenterLine = ta.getBoolean(R.styleable.AudioWaveView_wv_show_center_line, true)
        isShowTopBottomLine = ta.getBoolean(R.styleable.AudioWaveView_wv_show_top_bottom_line, true)
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

    fun setCenterLineColor(color: Int) {
        mCenterLinePaint.color = color
        postInvalidate()
    }

    fun setLeftPaintColor(color: Int) {
        mRectLeftPaint.color = color
        postInvalidate()
    }

    fun setRightPaintColor(color: Int) {
        mRectRightPaint.color = color
        postInvalidate()
    }

    fun setCutSelectPaintColor(color: Int) {
        val alpha = Color.alpha(color)
        if (alpha > 0.3 * 255) {
            Log.e(
                TAG,
                "setCutSelectPaintColor fail `alpha == $alpha` is not allow, preferably less than ${
                    Integer.toHexString((0.3 * 255).toInt())
                }"
            )
            return
        }
        mSelectedBGPaint.color = color
        postInvalidate()
    }

    fun setCutMarkerPointPaintColor(color: Int) {
        mCutMarkerPoint.color = color
        postInvalidate()
    }

    fun setWaveWidth(width: Float) {
        mRectWidth = width
        checkOffsetX()
        postInvalidate()
    }

    fun setWaveSpace(spaceWidth: Float) {
        mRectSpace = spaceWidth
        checkOffsetX()
        invalidate()
    }

    fun setWaveCornerRadius(radius: Float) {
        mRectCornerRadius = radius
        postInvalidate()
    }

    fun setWaveScale(scale: Float) {
        mScaleFactor = scale.coerceAtLeast(mMinScale).coerceAtMost(mMaxScale)
        postInvalidate()
    }

    fun setTimeSize(sizeDp: Float) {
        mTextTimePaint.textSize = dip2px(sizeDp).toFloat()
        postInvalidate()
    }

    /**
     * @param enable 是否可以通过手势进行滚动
     * @param toEnd 是否直接滚动底部
     */
    open fun setEnableScroll(enable: Boolean, toEnd: Boolean = false) {
        this.mCanScroll = enable
        if (toEnd) {
            scrollToEnd()
        }
    }

    /**
     * 是否已经滚到了底部
     * @return
     */
    open fun isScrollEnd(): Boolean {
        return mOffsetX == -mContentLength
    }

    /**
     * Add frame
     * 添加声音
     * @param frame
     * @param duration 总时长
     */
    open fun addFrame(frame: Float, duration: Long) {
        mFrameArray.add(frame)
        mOffsetX += -(mRectWidth + mRectSpace)
        this.mDuration = duration
        checkOffsetX()
        invalidate()
    }

    /**
     * Scroll to end
     * 快速到底部
     */
    open fun scrollToEnd(needAnim: Boolean = true) {
        mAnima?.cancel()
        if (needAnim) {
            mAnima = ObjectAnimator.ofFloat(mOffsetX, -mContentLength).also { an ->
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
        } else {
            mOffsetX = -mContentLength
            invalidate()
        }
    }

    /**
     * Scroll to end
     * 开头
     */
    open fun scrollToStart(needAnim: Boolean = true) {
        mAnima?.cancel()
        if (needAnim) {
            mAnima = ObjectAnimator.ofFloat(mOffsetX, 0f).also { an ->
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
        } else {
            mOffsetX = 0f
            invalidate()
        }
    }


    /**
     * Clear frame
     * 情况
     */
    open fun clearFrame() {
        mDuration = 0
        mIsEditModel = false
        mChangeListener?.onEditModelChange(false)
        mFrameArray.reset()
        mCutStartTime = 0
        mCutEndTime = 0
        mOffsetX = 0f
        mOffsetCutStartX = 0f
        mOffsetCutEndX = 0f
        invalidate()
    }


    /**
     * Start edit model
     * 开启编辑模式
     */
    open fun startEditModel() {
        mOffsetCutEndX = (-mOffsetX).coerceAtLeast(min(100f, mContentLength))
        mOffsetCutStartX = mOffsetCutEndX - min(100f, mContentLength)
        mIsEditModel = true
        mChangeListener?.onEditModelChange(true)
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
        mChangeListener?.onEditModelChange(true)
        updateSelectTimePosition()
        invalidate()
    }

    /**
     * 获取剪切模式是的开始时间
     * @return
     */
    open fun getCutStartTime(): Long {
        if (isEditModel()) {
            return mCutStartTime
        }
        return 0
    }

    /**
     * 获取剪切模式是的结束时间
     * @return
     */
    open fun getCutEndTime(): Long {
        if (isEditModel()) {
            return mCutEndTime
        }
        return 0
    }

    /**
     * Close edit model
     * 关闭编辑模式
     */
    open fun closeEditModel() {
        mIsEditModel = false
        mChangeListener?.onEditModelChange(false)
        invalidate()
    }


    /**
     * 开始覆盖模式
     * 删除中线以后的数据，触发剪切
     */
    open suspend fun startOverwrite(): Boolean {
        if (isEditModel()) {
            closeEditModel()
        }
        val isCutOk = mChangeListener?.onCutAudio(mCurrentPosition, mDuration) ?: false
        if (isCutOk) {
            deleteFrames(mCurrentPosition, mDuration)
            mCutStartTime = mCutEndTime
            invalidate()
            scrollToEnd(false)
            mChangeListener?.onCutFinish()
        }
        return isCutOk
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
     * Set current position
     * 设置当前中线位置代表的时间
     * @param currentTime
     */
    open fun setCurrentPosition(currentTime: Long) {
        mOffsetX = currentTime.coerceAtLeast(0).let {
            (it.times(mContentLength) / mDuration).coerceAtLeast(min(100f, mContentLength))
        }
        checkOffsetX()
        invalidate()
    }

    open fun getCurrentPosition(): Long {
        return mCurrentPosition
    }

    /**
     * Get index by time
     *
     * @param time 时间
     * @return 当前时间的在FrameArray的位置
     */
    open fun getIndexByTime(time: Long): Int {
        return (time / 1000f * mOneSecondSize).toInt()
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
     * 在替换之前需要进行对文件的剪切，拼接
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
        mCutStartTime = startTime
        val startIndex = getIndexByTime(startTime)
        mFrameArray.delete(startIndex, startIndex + delSize.toInt())
        addFrames(startIndex, newFrameArray, newFrameDuration)
        if (editModel) {
            startEditModel(mCutStartTime, mCutStartTime + newFrameDuration)
        }
        checkOffsetX()
    }


    /**
     * Cut select
     * 剪切掉选中部分
     */
    open suspend fun cutSelect(): Boolean {
        if (!mIsEditModel) return false
        val isCutOk = mChangeListener?.onCutAudio(mCutStartTime, mCutEndTime) ?: true
        if (isCutOk) {
            deleteFrames(mCutStartTime, mCutEndTime)
            mCutStartTime = mCutEndTime
            checkOffsetX()
            closeEditModel()
            mChangeListener?.onCutFinish()
        }
        return isCutOk
    }


    /**
     * Delete frames
     * 删除声音数据
     * @param startTime 开始时间
     * @param endTime 结束时间
     */
    protected open fun deleteFrames(startTime: Long, endTime: Long) {
        val cutTime = endTime - startTime
        mDuration -= cutTime
        val size = ((mDuration / 1000f) * mOneSecondSize).toInt()
        val delSize = mFrameArray.getSize() - size
        deleteFrames(startTime, delSize)
    }

    private fun deleteFrames(startTime: Long, delSize: Int) {
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
                        mIsTouchLeft = abs(x - getLeftLineStartX()) < touchRec
                        mIsTouchRight = abs(x - getRightLineStartX()) < touchRec
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val moveX = (event.x - mLastX) / (mScaleFactor * 2)
                        mLastX = event.x
                        if (moveX > 0) {
                            //右滑动
                            if (mIsTouchLeft && mIsTouchRight) {
                                mOffsetCutEndX = (mOffsetCutEndX + moveX).coerceAtLeast(mOffsetCutStartX)
                                    .coerceAtMost(mContentLength)
                                mIsTouchLeft = false
                            } else if (mIsTouchLeft) {
                                mOffsetCutStartX =
                                    (mOffsetCutStartX + moveX).coerceAtLeast(0f).coerceAtMost(mOffsetCutEndX)
                            } else if (mIsTouchRight) {
                                mOffsetCutEndX = (mOffsetCutEndX + moveX).coerceAtLeast(mOffsetCutStartX)
                                    .coerceAtMost(mContentLength)
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
                                    mOffsetX = (mOffsetX + 5).coerceAtLeast(-mContentLength)
                                }
                            } else if (mIsTouchRight) {
                                mOffsetCutEndX = (mOffsetCutEndX + moveX).coerceAtLeast(mOffsetCutStartX)
                                    .coerceAtMost(mContentLength)
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
        this.mCutStartTime = (mDuration * abs(mOffsetCutStartX) / mContentLength).toLong() / 10 * 10
        this.mCutEndTime = (mDuration * abs(mOffsetCutEndX) / mContentLength).toLong() / 10 * 10
        mChangeListener?.onUpdateCutPosition(mCutStartTime, mCutEndTime)
    }

    protected val mIconRect = Rect(0, 0, 0, 0) //用来

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        val halfWidth = (width / 2).toFloat()
        val halfHeight = (height / 2).toFloat()
        canvas?.apply {
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            if (mFrameArray.get().isNotEmpty()) {

                // 获取屏幕需要展示的下标
                val endIndex =
                    ((width + 20 - getStartX()) / (mRectWidth * mScaleFactor + mRectSpace * mScaleFactor) + 1).toInt()
                        .coerceAtMost(mFrameArray.getSize() - 1)
                val firstIndex =
                    ((-10 - getStartX()) / (mRectWidth * mScaleFactor + mRectSpace * mScaleFactor) - 1).toInt()
                        .coerceAtLeast(0)
                // 遍历下班展示矩形
                for (index in firstIndex..endIndex) {
                    mRectStart =
                        getStartX() + index * mRectWidth * mScaleFactor + mRectSpace * mScaleFactor * index
                    mRectEnd =
                        getStartX() + (index + 1) * mRectWidth * mScaleFactor + mRectSpace * mScaleFactor * index
                    if (mRectEnd <= width + 20 || mRectStart >= -10) {
                        val halfRectHeight =
                            min(mFrameArray.get()[index] / mLevel.toFloat(), 1f) / 2 * halfHeight //矩形的半高
                        // 获取到矩形一半的高度
                        if (halfRectHeight <= mRectWidth * mScaleFactor / 2) {
                            //如果小于mRectWidth，直接画圆
                            drawCircle((mRectEnd - mRectStart) / 2 + mRectStart, halfHeight, mRectWidth * mScaleFactor / 2, mRectRightPaint)
                        } else {
                            //否则画矩形
                            mRectVoiceLine.left = mRectStart
                            mRectVoiceLine.top = halfHeight - halfRectHeight
                            mRectVoiceLine.right = mRectEnd
                            mRectVoiceLine.bottom = halfHeight + halfRectHeight
                            drawRoundRect(mRectVoiceLine, mRectCornerRadius, mRectCornerRadius, mRectRightPaint)
                        }
                    }
                }
            }
            //左边着色
            canvas.drawRect(
                0f,
                0f,
                halfWidth,
                height.toFloat(),
                mRectLeftPaint
            )
            val mFontMetrics = mTextTimePaint.fontMetrics
            val mTop = mFontMetrics.top
            val mBottom = mFontMetrics.bottom
            val baseLineY = topLineMargin / 2 - mTop / 2 - mBottom / 2 //基线中间点的y轴计算公式
            if (isShowTopBottomLine) {
                //先判断一共需要画的刻度是多少
                val time = (mContentLength / mSecondWidth).toInt()
                // 可能没有内容，我们也要画为了美观
                val realSecondWidth = (mSecondWidth) * mScaleFactor
                val i = (width / realSecondWidth).toInt()
                repeat(i + time) {
                    mRectTimeStart = getStartX() + it * realSecondWidth
                    if (mRectTimeStart >= 0 && mRectTimeStart <= width) {
                        mRectTimeLine.left = mRectTimeStart - 1
                        mRectTimeLine.top = topLineMargin - 10f
                        mRectTimeLine.right = mRectTimeStart + 1f
                        mRectTimeLine.bottom = topLineMargin
                        //只有屏幕之内的刻度才画出来
                        drawRoundRect(mRectTimeLine, 1f, 1f, mLinePaint)
                        if (realSecondWidth > 90) {
                            drawText(it.covertToTime(), mRectTimeStart, baseLineY, mTextTimePaint)
                        } else if (realSecondWidth > 60) {
                            if (it % 2 == 0) {
                                drawText(it.covertToTime(), mRectTimeStart, baseLineY, mTextTimePaint)
                            }
                        } else if (realSecondWidth > 30) {
                            if (it % 3 == 0) {
                                drawText(it.covertToTime(), mRectTimeStart, baseLineY, mTextTimePaint)
                            }
                        } else {
                            if (it % 5 == 0) {
                                drawText(it.covertToTime(), mRectTimeStart, baseLineY, mTextTimePaint)
                            }
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
            }
            if (isShowCenterLine) {
                // 画中线
                drawLine(halfWidth, 2f, halfWidth, height - bottomLineMargin, mCenterLinePaint)
                drawCircle(halfWidth, (cirWidth / 2).toFloat() + 8f, cirWidth.toFloat(), mCenterLinePaint)
            }
            canvas.restore()
            if (mIsEditModel) {
                // 画矩形
                drawRect(
                    getLeftLineStartX(),
                    topLineMargin,
                    getRightLineStartX(),
                    height - bottomLineMargin,
                    mSelectedBGPaint
                )
                // 画开始左线
                drawLine(
                    getLeftLineStartX(),
                    topLineMargin,
                    getLeftLineStartX(),
                    height - bottomLineMargin,
                    mCutMarkerPoint
                )
                if (mIsTouchLeft) {
                    //左边的文字进度
                    drawText(getStartTs(), getLeftLineStartX(), baseLineY, mTextCutTimePaint)
                }
                // 画结束右线
                drawLine(
                    getRightLineStartX(),
                    topLineMargin,
                    getRightLineStartX(),
                    height - bottomLineMargin,
                    mCutMarkerPoint
                )
                if (mIsTouchRight) {
                    //右边文字的进度
                    drawText(getEndTs(), getRightLineStartX(), baseLineY, mTextCutTimePaint)
                }
                mIconRect.set(
                    (getLeftLineStartX() - iconSize / 2).toInt(),
                    (height - bottomLineMargin - iconSize / 2).toInt(),
                    (getLeftLineStartX() + iconSize / 2).toInt(),
                    (height - bottomLineMargin + iconSize / 2).toInt()
                )
                bitmap?.let { drawBitmap(it, null, mIconRect, mIconPaint) }
                mIconRect.set(
                    (getRightLineStartX() - iconSize / 2).toInt(),
                    (height - bottomLineMargin - iconSize / 2).toInt(),
                    (getRightLineStartX() + iconSize / 2).toInt(),
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
            an.resetDurationScale()
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
            an.resetDurationScale()
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


    fun setListener(callBack: OnChangeListener?) {
        this.mChangeListener = callBack
        mChangeListener?.let {
            it.onUpdateCurrentPosition(mCurrentPosition, mDuration)
            it.onUpdateScale(mScaleFactor)
            if (isEditModel()) {
                it.onUpdateCutPosition(mCutStartTime, mCutEndTime)
            }
        }
    }

    interface OnChangeListener {

        /**
         * On update current position
         * 更新当前的位置,中线表示的时间
         * @param position 中线的时间
         * @param duration 总时长
         */
        fun onUpdateCurrentPosition(position: Long, duration: Long) {

        }

        /**
         * On update cut position
         * 更新当前选中剪切的位置
         * @param startPosition
         * @param endPosition
         */
        fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {

        }

        /**
         * On cut audio
         * 触发剪切
         * @param startTime
         * @param endTime
         * @return 是否剪切成功 ,只有成功了才会进行后续操作，如
         */
        suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean {
            return true
        }

        /**
         * On cut finish
         * 剪切完成
         */
        fun onCutFinish() {

        }

        /**
         * On update scale
         * 缩放级别更新
         * @param scale
         */
        fun onUpdateScale(scale: Float) {

        }

        fun onEditModelChange(isEditModel: Boolean) {

        }
    }
}