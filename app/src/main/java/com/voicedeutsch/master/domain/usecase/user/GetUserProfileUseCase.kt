package com.voicedeutsch.master.domain.usecase.user

import com.voicedeutsch.master.domain.model.user.UserProfile
import com.voicedeutsch.master.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

/**
 * Retrieves the user profile.
 * Provides both one-shot and observable (Flow) access.
 */
class GetUserProfileUseCase(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(userId: String): UserProfile? {
        return userRepository.getUserProfile(userId)
    }

    fun observeProfile(userId: String): Flow<UserProfile?> {
        return userRepository.getUserProfileFlow(userId)
    }

    suspend fun getActiveUserId(): String? {
        return userRepository.getActiveUserId()
    }

    suspend fun userExists(): Boolean {
        return userRepository.userExists()
    }
}