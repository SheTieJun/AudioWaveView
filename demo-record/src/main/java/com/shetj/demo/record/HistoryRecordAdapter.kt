package com.shetj.demo.record

import android.util.SparseArray
import android.widget.CheckBox
import androidx.core.util.containsKey
import androidx.core.util.forEach
import androidx.core.util.valueIterator
import androidx.lifecycle.MutableLiveData
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.shetj.demo.record.model.Record
import com.shetj.demo.record.model.RecordInfo
import com.shetj.demo.record.utils.Util
import me.shetj.base.ktx.highString
import me.shetj.base.ktx.isTrue


class HistoryRecordAdapter(data: MutableList<RecordInfo>? = null) :
    BaseMultiItemQuickAdapter<RecordInfo, BaseViewHolder>(data) {
    init {

        addItemType( RecordInfo.TYPE_RECORD,R.layout.item_history_record, )
        addItemType(RecordInfo.TYPE_TITLE,R.layout.item_history_record_title)
    }

    private var highString: String? = null
    var canCheckLiveDate =MutableLiveData(false)

    private var checkArray = SparseArray<RecordInfo>()

    val checkUpdateLiveDate = MutableLiveData(0)

    override fun convert(holder: BaseViewHolder, item: RecordInfo) {

        when(item.itemType){
            RecordInfo.TYPE_RECORD ->{
                item.record?.let {record ->
                    holder.setText(R.id.tv_record_name, highString(record.audioName!!, listOf(highString ?: "")))
                        .setText(R.id.tv_record_length, Util.formatSeconds3(record.audioLength))
                        .setText(R.id.tv_record_create_time, record.createTimeDate)
                        .setGone(R.id.checkbox,!canCheckLiveDate.isTrue())

                    val check = isCheck(holder.bindingAdapterPosition)
                    holder.getView<CheckBox>(R.id.checkbox).apply {
                        isChecked = check
                    }.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked){
                            checkArray[holder.bindingAdapterPosition] = item
                        }else{
                            checkArray.remove(holder.bindingAdapterPosition)
                        }
                    }
                }
            }
            RecordInfo.TYPE_TITLE ->{
                holder.setText(R.id.tv_year_moth, item.title)
            }
        }
    }

    override fun convert(holder: BaseViewHolder, item: RecordInfo, payloads: List<Any>) {
        super.convert(holder, item, payloads)
        if (payloads[0] == 1){
            if (item.itemType == RecordInfo.TYPE_RECORD) {
                holder.getView<CheckBox>(R.id.checkbox).apply {
                    val check = isCheck(holder.bindingAdapterPosition)
                    isChecked = check
                }
            }
        }
    }

    private fun isCheck(position: Int ) =  checkArray.containsKey(position)


    fun isCheckStyle() = canCheckLiveDate.isTrue()

    fun setHighString(highString: String?) {
        this.highString = highString
        notifyItemRangeChanged(0, getDefItemCount())
    }

    fun setCanCheck(canCheck:Boolean){
        this.canCheckLiveDate.postValue(canCheck)
        notifyItemRangeChanged(0, getDefItemCount())
        checkArray.clear()
        checkUpdateLiveDate.postValue(if (canCheck)checkArray.size() else 0)
    }

    fun invertCheck(position:Int,needNotify:Boolean = true){
        if (isCheck(position)){
            checkArray.remove(position)
        }else{
            checkArray[position] = getItem(position)
        }
        if (needNotify){
            notifyItemChanged(position,1)
        }
        checkUpdateLiveDate.postValue(if (canCheckLiveDate.isTrue())checkArray.size() else 0)
    }

    fun getSelectRecordList(): MutableList<Record> {
       return checkArray.valueIterator().asSequence().mapNotNull { it.record }.toMutableList()
    }

    fun clearSelect() {
        checkArray.forEach { _, value ->
            remove(value)
        }
        checkArray.clear()
        notifyItemRangeChanged(0, getDefItemCount())
        checkUpdateLiveDate.postValue(0)
    }
}