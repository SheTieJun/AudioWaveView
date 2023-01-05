package com.shetj.waveview


class FrameArray {

    private var rawFrameArray: ArrayList<Float> = ArrayList()

    fun add(frames: Collection<Float>) {
        rawFrameArray.addAll(frames)
    }

    fun add(frames: Array<Float>) {
        rawFrameArray.addAll(frames)
    }

    fun add(index:Int,frames: Collection<Float>){
        rawFrameArray.addAll(index,frames)
    }

    fun add(frame: Float) {
        rawFrameArray.add(frame)
    }

    fun get(): ArrayList<Float> {
        return rawFrameArray
    }

    fun getSize(): Int {
        return rawFrameArray.size
    }

    fun delete(fromIndex: Int, toIndex: Int) {
        rawFrameArray.subList(fromIndex.coerceAtLeast(0), toIndex.coerceAtMost(getSize())).clear()
    }

    fun reset() {
        rawFrameArray.clear()
    }
}
