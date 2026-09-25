package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String? = null,
    val name: String,
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
