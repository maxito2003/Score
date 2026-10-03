package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String? = null,
    val code: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val mode: String = "LIGA",
    @SerialName("home_team") val homeTeam: String? = "Equipo Local",
    @SerialName("away_team") val awayTeam: String? = "Equipo Visitante",
    val status: String? = "LIVE",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("lineup_data") val lineupData: String? = null,
    val runs: Int = 0,
    val hits: Int = 0,
    val errors: Int = 0,
    val outs: Int = 0,
    @SerialName("home_runs") val homeRuns: Int = 0,
    @SerialName("duration_seconds") val durationSeconds: Int = 0
)