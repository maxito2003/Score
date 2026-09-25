package com.example.clubhome.data.repository

import com.example.clubhome.data.model.Group
import com.example.clubhome.data.model.Match
import com.example.clubhome.data.model.Player
import com.example.clubhome.data.model.Team
import com.example.clubhome.data.remote.SupabaseClientManager
import io.github.jan.supabase.postgrest.postgrest

class BaseballRepository {

    private val client = SupabaseClientManager.client

    // --- GRUPOS / LIGAS ---
    suspend fun getGroups(): List<Group> {
        return client.postgrest["groups"].select().decodeList<Group>()
    }

    suspend fun createGroup(group: Group): Group {
        return client.postgrest["groups"].insert(group) {
            select()
        }.decodeSingle<Group>()
    }

    // --- EQUIPOS ---
    suspend fun getTeams(groupId: String? = null): List<Team> {
        return if (groupId != null) {
            client.postgrest["teams"].select {
                filter {
                    eq("group_id", groupId)
                }
            }.decodeList<Team>()
        } else {
            client.postgrest["teams"].select().decodeList<Team>()
        }
    }

    suspend fun createTeam(team: Team): Team {
        return client.postgrest["teams"].insert(team) {
            select()
        }.decodeSingle<Team>()
    }

    // --- JUGADORES ---
    suspend fun getPlayersByTeam(teamId: String): List<Player> {
        return client.postgrest["players"].select {
            filter {
                eq("team_id", teamId)
            }
        }.decodeList<Player>()
    }

    suspend fun createPlayer(player: Player): Player {
        return client.postgrest["players"].insert(player) {
            select()
        }.decodeSingle<Player>()
    }

    // --- PARTIDOS ---
    suspend fun getMatches(groupId: String? = null): List<Match> {
        return if (groupId != null) {
            client.postgrest["matches"].select {
                filter {
                    eq("group_id", groupId)
                }
            }.decodeList<Match>()
        } else {
            client.postgrest["matches"].select().decodeList<Match>()
        }
    }

    suspend fun createMatch(match: Match): Match {
        return client.postgrest["matches"].insert(match) {
            select()
        }.decodeSingle<Match>()
    }

    suspend fun updateMatch(match: Match) {
        match.id?.let { matchId ->
            client.postgrest["matches"].update(match) {
                filter {
                    eq("id", matchId)
                }
            }
        }
    }
}