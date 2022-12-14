package com.nguyennhatminh614.motobikedriverlicenseapp.screen.study

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nguyennhatminh614.motobikedriverlicenseapp.R
import com.nguyennhatminh614.motobikedriverlicenseapp.data.model.QuestionOptions
import com.nguyennhatminh614.motobikedriverlicenseapp.data.model.Questions
import com.nguyennhatminh614.motobikedriverlicenseapp.data.model.StudyCategory
import com.nguyennhatminh614.motobikedriverlicenseapp.data.model.WrongAnswerObject
import com.nguyennhatminh614.motobikedriverlicenseapp.data.repository.StudyRepository
import com.nguyennhatminh614.motobikedriverlicenseapp.data.repository.WrongAnswerRepository
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.base.BaseViewModel
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.constant.AppConstant
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.generateEmptyQuestionStateList
import com.nguyennhatminh614.motobikedriverlicenseapp.utils.interfaces.IResponseListener
import kotlinx.coroutines.launch

class StudyViewModel(
    private val studyRepository: StudyRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
) : BaseViewModel() {

    private val listDefaultStudyCategory = listOf(
        StudyCategory(
            R.drawable.ic_caution,
            MUST_NOT_WRONG_ANSWER_QUESTION,
        ),
        StudyCategory(
            R.drawable.ic_exam,
            CONCEPT_AND_RULES_QUESTION,
        ),
        StudyCategory(
            R.drawable.ic_signal,
            TRAFFIC_SIGNAL_QUESTION,
        ),
        StudyCategory(
            R.drawable.ic_sat_figure,
            SAT_FIGURE_QUESTION,
        ),
    )

    private val _listStudyCategory = MutableLiveData(listDefaultStudyCategory)

    val listStudyCategory: LiveData<List<StudyCategory>>
        get() = _listStudyCategory

    private val _currentStudyCategoryIndex = MutableLiveData<Int>(AppConstant.NONE_POSITION)

    val currentStudyCategory: LiveData<Int>
        get() = _currentStudyCategoryIndex

    init {
        onUpdateListStudyCategory()
    }

    fun setCurrentStudyCategoryPosition(index: Int) {
        _currentStudyCategoryIndex.postValue(index)
    }

    fun saveProgressToDatabase() {
        viewModelScope.launch {
            listStudyCategory.value?.let {
                studyRepository.saveProgress(it)
            }
        }
    }

    fun saveWrongAnswerObjectToDatabase(wrongAnswerObject: WrongAnswerObject) {
        viewModelScope.launch {
            if (wrongAnswerRepository.checkWrongAnswerQuestionExists(wrongAnswerObject.questionsID)) {
                wrongAnswerRepository.updateWrongAnswerQuestion(wrongAnswerObject)
            } else {
                wrongAnswerRepository.insertNewWrongAnswerQuestion(wrongAnswerObject)
            }
        }
    }

    private fun onUpdateListStudyCategory() {
        launchTask {
            val data = studyRepository.getAllInfoStudyCategory()

            if (data.isEmpty()) {
                studyRepository.saveProgress(listDefaultStudyCategory)
            } else {
                _listStudyCategory.postValue(data)
            }

            studyRepository.getListQuestion(
                object : IResponseListener<MutableList<Questions>> {
                    override fun onSuccess(data: MutableList<Questions>) {
                        viewModelScope.launch {
                            val newList = mutableListOf<StudyCategory>()
                            val oldList = _listStudyCategory.value ?: listDefaultStudyCategory

                            newList.apply {
                                add(data.processPullListData(
                                    AppConstant.MUST_NOT_WRONG_ANSWER,
                                    oldList[MUST_NOT_WRONG_ANSWER_POSITION]
                                ))
                                add(data.processPullListData(
                                    AppConstant.CONCEPTS_AND_RULES,
                                    oldList[CONCEPT_AND_RULES_POSITION]
                                ))
                                add(data.processPullListData(
                                    AppConstant.TRAFFIC_SIGNAL,
                                    oldList[TRAFFIC_SIGNAL_POSITION]
                                ))
                                add(data.processPullListData(
                                    AppConstant.TRAFFIC_SITUATION_BY_PICTURE,
                                    oldList[SAT_FIGURE_POSITION]
                                ))
                            }

                            studyRepository.saveProgress(newList)

                            _listStudyCategory.postValue(newList)
                            hideLoading()
                        }
                    }

                    override fun onError(exception: Exception?) {
                        this@StudyViewModel.exception.postValue(exception)
                        hideLoading()
                    }
                }
            )
        }
    }

    private fun MutableList<Questions>.processPullListData(
        categoryFilter: String,
        lastData: StudyCategory,
    ): StudyCategory {
        val listQuestion =
            this.filter { return@filter it.questionType == categoryFilter }.toMutableList()
        return StudyCategory(
            lastData.iconResourceID,
            lastData.title,
            listQuestion,
            if (lastData.listQuestionsState.isEmpty() ||
                lastData.listQuestionsState.size != listQuestion.size)
                generateEmptyQuestionStateList(listQuestion.size)
            else lastData.listQuestionsState,
            listQuestion.size,
            lastData.numbersOfSelectedQuestions,
        )
    }

    fun updateDataQuestionPos(questionsPosition: Int, item: QuestionOptions) {
        val currentStudyCategoryPos = currentStudyCategory.value
        val currentListStudy = _listStudyCategory.value

        if (currentStudyCategoryPos != null && currentListStudy != null) {
            val list = mutableListOf<StudyCategory>()
            list.addAll(currentListStudy)
            list[currentStudyCategoryPos].listQuestionsState[questionsPosition] = item
            list[currentStudyCategoryPos].numbersOfSelectedQuestions =
                list[currentStudyCategoryPos]
                    .listQuestionsState
                    .filter { return@filter it.position != AppConstant.NONE_POSITION }
                    .count()
            _listStudyCategory.postValue(list)
        }
    }

    fun getQuestionOptionSelectedByQuestionPosition(questionsPosition: Int): QuestionOptions? {
        val currentStudyCategoryPos = currentStudyCategory.value

        if (currentStudyCategoryPos != null
            && currentStudyCategoryPos != AppConstant.NONE_POSITION
            && questionsPosition != AppConstant.NONE_POSITION
        ) {
            _listStudyCategory.value?.let { notNullList ->
                if (questionsPosition < notNullList[currentStudyCategoryPos].listQuestionsState.size) {
                    return notNullList[currentStudyCategoryPos].listQuestionsState[questionsPosition]
                }
            }
        }

        return null
    }

    fun resetAllStudyCategoryState() {
        launchTask {
            val newList = listStudyCategory.value

            newList?.forEach {
                it.numbersOfSelectedQuestions = DEFAULT_NOT_SELECTED_STATE
                val listSize = it.listQuestionsState.size
                it.listQuestionsState.clear()
                it.listQuestionsState.addAll(generateEmptyQuestionStateList(listSize))
            }

            _listStudyCategory.value = newList

            newList?.let {
                studyRepository.saveProgress(it)
                hideLoading()
            }
        }
    }

    companion object {
        const val MUST_NOT_WRONG_ANSWER_QUESTION = "C??u h???i ??i???m li???t"
        const val CONCEPT_AND_RULES_QUESTION = "Kh??i ni???m v?? lu???t"
        const val TRAFFIC_SIGNAL_QUESTION = "C??u h???i bi???n b??o"
        const val SAT_FIGURE_QUESTION = "C??u h???i sa h??nh"
        const val MUST_NOT_WRONG_ANSWER_POSITION = 0
        const val CONCEPT_AND_RULES_POSITION = 1
        const val TRAFFIC_SIGNAL_POSITION = 2
        const val SAT_FIGURE_POSITION = 3
        const val DEFAULT_NOT_SELECTED_STATE = 0
    }
}
