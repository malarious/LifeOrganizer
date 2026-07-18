package com.geepscontrol.core.domain

enum class ReviewType { DAILY, WEEKLY }

data class ReviewRecord(
    val id: Long = 0,
    val type: ReviewType,
    val periodStart: Long,
    val periodEnd: Long,
    val wentWell: String = "",
    val friction: String = "",
    val nextPriority: String = "",
    val notes: String = "",
    val completedAt: Long,
)
