package com.shetj.demo.record.utils

import me.shetj.base.ktx.toJson
import me.shetj.base.tools.app.Utils
import me.shetj.base.tools.file.SPUtils
import me.shetj.base.tools.json.GsonKit


class RecordAppOption {
    var isUseFloat = false
    var maxTime = 30 * 60 * 1000
}


object RecordConfig {

    private const val KEY_RECORD_CONFIG = "key_record_config"

    private var option: RecordAppOption

    init {
        option = getRecordAppOption()
    }

    private fun getRecordAppOption(): RecordAppOption {
        val info: String? = SPUtils.get(Utils.app, KEY_RECORD_CONFIG, "") as? String
        return info?.let { GsonKit.jsonToBean(it, RecordAppOption::class.java) }
            ?: RecordAppOption()
    }

    /**
     * 更新选项
     */
    fun updateOption(action: RecordAppOption.()->Unit){
        option.apply(action)
        save()
    }

    private fun save(){
        option.toJson()?.let { SPUtils.put(Utils.app, KEY_RECORD_CONFIG, it) }
    }
}