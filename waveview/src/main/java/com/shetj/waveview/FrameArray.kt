package com.shetj.waveview

/**
 * Created by chunsheng on 2019-08-21.
 * 构建一个容纳声波数据的 List
 */
class FrameArray() {

    private var rawFrameArray: ArrayList<Float> = ArrayList()

    fun add(frames: FloatArray) {
        frames.map {
            rawFrameArray.add(it)
        }
    }

    fun add(frame: Float) {
        rawFrameArray.add(frame)
    }

    fun get(): FloatArray {
        return rawFrameArray.toFloatArray()
    }

    fun getSize(): Int {
        return rawFrameArray.size
    }

    /**
     * 删除指定的序列[fromIndex]包含，[toIndex]不包含
     */
    fun delete(fromIndex: Int, toIndex: Int) {
        rawFrameArray.subList(fromIndex.coerceAtLeast(0), toIndex.coerceAtMost(getSize())).clear()
    }

    fun reset() {
        rawFrameArray.clear()
    }
}
