package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
