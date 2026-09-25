package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String? = null,
    @SerialName("group_id")
    val groupId: String? = null,
    @SerialName("home_team_id")
    val homeTeamId: String,
    @SerialName("away_team_id")
    val awayTeamId: String,
    @SerialName("home_score")
    val homeScore: Int = 0,
    @SerialName("away_score")
    val awayScore: Int = 0,
    @SerialName("current_inning")
    val currentInning: Int = 1,
    @SerialName("is_top_inning")
    val isTopInning: Boolean = true,
    val status: String = "SCHEDULED", // SCHEDULED, IN_PROGRESS, FINISHED
    @SerialName("created_at")
    val createdAt: String? = null
)
