package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val name: String,
    val coach: String? = null,
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)