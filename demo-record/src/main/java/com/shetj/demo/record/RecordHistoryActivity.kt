package com.shetj.demo.record

import android.view.MenuItem
import com.chad.library.adapter.base.animation.ScaleInAnimation
import com.shetj.demo.record.databinding.ActivityRecordHistoryBinding
import com.shetj.demo.record.model.RecordInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import me.shetj.base.ktx.launch
import me.shetj.base.ktx.setAppearance
import me.shetj.base.mvvm.BaseBindingActivity

/**
 * 录音历史记录
 */
class RecordHistoryActivity :
    BaseBindingActivity<ActivityRecordHistoryBinding, HistoryViewModel>() {


    private val mAdapter: HistoryRecordAdapter by lazy { HistoryRecordAdapter() }

    override fun initView() {
        super.initView()
        setAppearance(true)
        setTitle("录音")
        with(mViewBinding) {
            mAdapter.adapterAnimation = ScaleInAnimation()
//            mAdapter.setOnItemLongClickListener { _, view, position ->
//                if (!mAdapter.isCheckStyle()) {
//                    mAdapter.setCanCheck(true)
//                }
//                mAdapter.invertCheck(position)
//                return@setOnItemLongClickListener true
//            }
            mAdapter.setOnItemClickListener { _, view, position ->
                if (mAdapter.isCheckStyle()) {
                    mAdapter.invertCheck(position)
                } else {
                    //TODO（点击播放）
                }
            }
            recyclerView.adapter = mAdapter

        }

    }

    override fun initData() {
        super.initData()
        launch {
            mViewModel.getAllRecordInfo().map {
                val recordInfoList = ArrayList<RecordInfo>()
                var showTime: String? = null
                it.forEach { record ->
                    if (showTime != record.createTimeDate) {
                        showTime = record.createTimeDate
                        recordInfoList.add(RecordInfo(RecordInfo.TYPE_TITLE, title = record.createTimeDate))
                    }
                    recordInfoList.add(RecordInfo(RecordInfo.TYPE_RECORD, record = record))
                }
                recordInfoList
            }.shareIn(this, SharingStarted.WhileSubscribed(), 1).first().forEach { record ->
                delay(5)
                mAdapter.addData(record)
            }
        }
    }

    override fun onBackPressed() {
        if (mAdapter.isCheckStyle()) {
            mAdapter.setCanCheck(false)
            return
        }
        super.onBackPressed()
    }
}