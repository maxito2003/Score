package com.example.clubhome.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String? = null,
    val mode: String,
    @SerialName("home_team") val homeTeam: String,
    @SerialName("away_team") val awayTeam: String,
    val status: String,
    @SerialName("lineup_data") val lineupData: String? = null,
    val runs: Int = 0,
    val hits: Int = 0,
    val errors: Int = 0,
    val outs: Int = 0,
    @SerialName("home_runs") val homeRuns: Int = 0
)