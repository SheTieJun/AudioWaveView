package com.shetj.demo.record.model

import com.chad.library.adapter.base.entity.MultiItemEntity


class RecordInfo(override val itemType: Int,val record: Record? = null,val title:String? = null) : MultiItemEntity {

    companion object{
        const val TYPE_RECORD = 1
        const val TYPE_TITLE = 0
    }
}