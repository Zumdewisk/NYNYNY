package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class WeightLog(
    @DocumentId val id: String = "",
    val logId: String = "",
    val userId: String = "",
    val date: String = "", // "YYYY-MM-DD"
    val weightKg: Double = 0.0,
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "logId" to logId,
            "userId" to userId,
            "date" to date,
            "weightKg" to weightKg,
            "note" to note,
            "createdAt" to createdAt
        ).filterValues { it != null }
    }
}
