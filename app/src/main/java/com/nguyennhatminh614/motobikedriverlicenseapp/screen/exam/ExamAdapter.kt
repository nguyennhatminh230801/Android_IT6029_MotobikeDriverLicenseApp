package com.nguyennhatminh614.motobikedriverlicenseapp.screen.exam

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.nguyennhatminh614.motobikedriverlicenseapp.R
import com.nguyennhatminh614.motobikedriverlicenseapp.data.model.Exam
import com.nguyennhatminh614.motobikedriverlicenseapp.data.model.ExamState
import com.nguyennhatminh614.motobikedriverlicenseapp.databinding.ItemExamListLayoutBinding
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.OnClickItem
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.base.BaseRecyclerViewAdapter
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.base.BaseViewHolder
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.constant.AppConstant
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.extensions.toDateTimeDetail
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.extensions.toDateTimeMMSS

class ExamAdapter :
    BaseRecyclerViewAdapter<Exam, ItemExamListLayoutBinding, ExamAdapter.ViewHolder>(Exam.getDiffCallBack()) {

    private var doExamButtonCallBack: OnClickItem<Int>? = null

    override fun registerOnClickItemEvent(onClickItem: OnClickItem<Exam>) {
        this.clickItemInterface = onClickItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExamListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    fun registerOnClickExamButtonEvent(onClickItem: OnClickItem<Int>) {
        this.doExamButtonCallBack = onClickItem
    }

    private fun getSelectedColor(binding: ItemExamListLayoutBinding, color: Int) =
        binding.root.context.resources.getColor(color)

    companion object {
        private const val WATCH_RESULT = "Xem lại"
        private const val CONTINUE_DOING_TEST = "Tiếp tục"
    }

    inner class ViewHolder(
        override val binding: ItemExamListLayoutBinding,
    ) : BaseViewHolder<Exam, ItemExamListLayoutBinding>(binding) {

        override fun onBindData(data: Exam) {
            binding.apply {
                textExamTitle.text = "Đề số ${data.id}"
                if (data.examState != ExamState.UNDEFINED.value) {
                    buttonDoExam.text = WATCH_RESULT
                    textDescription.text =
                        "Đúng ${data.numbersOfCorrectAnswer} / ${data.listQuestions.size} câu"
                }

                if(data.examState == ExamState.UNDEFINED.value &&
                    data.currentTimeStamp != AppConstant.EXAM_TEST_FULL_TIME
                    && data.currentTimeStamp != AppConstant.DEFAULT_NOT_HAVE_TIME_STAMP) {
                    buttonDoExam.text = "Tiếp tục"
                    textDescription.text =
                        "Còn ${data.currentTimeStamp.toDateTimeDetail()}"

                    if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                        root.setCardBackgroundColor(getSelectedColor(binding, R.color.yellow_pastel_low_alpha))
                    } else {
                        root.setCardBackgroundColor(getSelectedColor(binding, R.color.yellow_pastel))
                    }
                }

                val idColor = when (data.examState) {
                    ExamState.PASSED.value -> R.color.green_pastel
                    ExamState.FAILED.value -> R.color.red_pastel
                    else -> null
                }

                idColor?.let {
                    root.setCardBackgroundColor(getSelectedColor(binding, idColor))
                }

                buttonDoExam.setOnClickListener {
                    doExamButtonCallBack?.let { function ->
                        function(adapterPosition)
                    }
                }
            }
        }
    }
}
