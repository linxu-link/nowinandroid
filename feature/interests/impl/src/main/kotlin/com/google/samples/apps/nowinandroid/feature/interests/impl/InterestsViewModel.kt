/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.feature.interests.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.domain.GetFollowableTopicsUseCase
import com.google.samples.apps.nowinandroid.core.domain.TopicSortField
import com.google.samples.apps.nowinandroid.core.model.data.FollowableTopic
import com.google.samples.apps.nowinandroid.feature.interests.api.navigation.InterestsNavKey
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * "兴趣"页面的 ViewModel
 *
 * 负责管理：
 * - 可关注的话题列表
 * - 当前选中的话题
 * - 用户对话题的关注状态
 */
@HiltViewModel(assistedFactory = InterestsViewModel.Factory::class)
class InterestsViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    val userDataRepository: UserDataRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
    // TODO: 见下方注释
    @Assisted val key: InterestsNavKey,
) : ViewModel() {

    // TODO: 这应该不再必要，当前选中的话题应该可以通过导航状态获取
    // 用于从保存状态中保存和检索当前选中话题 ID 的键
    private val selectedTopicIdKey = "selectedTopicIdKey"

    /**
     * 当前选中的话题 ID
     */
    private val selectedTopicId = savedStateHandle.getStateFlow(
        key = selectedTopicIdKey,
        initialValue = key.initialTopicId,
    )

    /**
     * UI 状态
     */
    val uiState: StateFlow<InterestsUiState> = combine(
        selectedTopicId,
        getFollowableTopics(sortBy = TopicSortField.NAME),
        InterestsUiState::Interests,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InterestsUiState.Loading,
    )

    /**
     * 关注/取消关注话题
     *
     * @param followedTopicId 话题 ID
     * @param followed 是否关注
     */
    fun followTopic(followedTopicId: String, followed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(followedTopicId, followed)
        }
    }

    /**
     * 处理话题点击事件
     *
     * @param topicId 点击的话题 ID，如果为 null 则取消选中
     */
    fun onTopicClick(topicId: String?) {
        // TODO: 这应该直接修改导航状态，而不仅仅是更新 savedStateHandle
        savedStateHandle[selectedTopicIdKey] = topicId
    }

    /**
     * ViewModel 工厂接口
     */
    @AssistedFactory
    interface Factory {
        fun create(key: InterestsNavKey): InterestsViewModel
    }
}

/**
 * "兴趣"页面的 UI 状态密封接口
 */
sealed interface InterestsUiState {
    /**
     * 加载中状态
     */
    data object Loading : InterestsUiState

    /**
     * 兴趣列表状态
     *
     * @param selectedTopicId 当前选中的话题 ID
     * @param topics 可关注的话题列表
     */
    data class Interests(
        val selectedTopicId: String?,
        val topics: List<FollowableTopic>,
    ) : InterestsUiState

    /**
     * 空状态（没有话题）
     */
    data object Empty : InterestsUiState
}
