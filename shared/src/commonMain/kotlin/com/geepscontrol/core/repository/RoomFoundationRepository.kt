package com.geepscontrol.core.repository

import com.geepscontrol.core.database.FoundationMetadata
import com.geepscontrol.core.database.FoundationMetadataDao

class RoomFoundationRepository(
    private val dao: FoundationMetadataDao,
) : FoundationRepository {
    override fun observeMetadata() = dao.observeAll()

    override suspend fun put(
        key: String,
        value: String,
    ) {
        dao.put(FoundationMetadata(key = key, value = value))
    }
}
