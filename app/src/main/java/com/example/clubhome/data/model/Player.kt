package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String? = null,
    @SerialName("team_id")
    val teamId: String? = null,
    val name: String,
    val number: Int? = null,
    val position: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)