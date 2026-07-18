package com.geepscontrol.core.repository

import com.geepscontrol.core.database.FoundationMetadata
import kotlinx.coroutines.flow.Flow

interface FoundationRepository {
    fun observeMetadata(): Flow<List<FoundationMetadata>>

    suspend fun put(
        key: String,
        value: String,
    )
}
