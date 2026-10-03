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
    suspend fun getTeams(groupId: String? = null, userId: String? = null): List<Team> {
        return client.postgrest["teams"].select {
            filter {
                if (!groupId.isNullOrBlank()) {
                    eq("group_id", groupId)
                }
                if (!userId.isNullOrBlank()) {
                    eq("user_id", userId)
                }
            }
        }.decodeList<Team>()
    }

    suspend fun createTeam(team: Team): Team {
        return client.postgrest["teams"].insert(team) {
            select()
        }.decodeSingle<Team>()
    }

    // Agregado: Permite actualizar el nombre, coach o logo de un equipo existente
    suspend fun updateTeam(team: Team) {
        team.id?.let { teamId ->
            client.postgrest["teams"].update(team) {
                filter {
                    eq("id", teamId)
                }
            }
        }
    }

    // Agregado: Permite eliminar un equipo por su ID
    suspend fun deleteTeam(teamId: String, userId: String? = null) {
        client.postgrest["teams"].delete {
            filter {
                eq("id", teamId)
                if (!userId.isNullOrBlank()) {
                    eq("user_id", userId)
                }
            }
        }
    }

    // --- JUGADORES ---
    suspend fun getPlayersByTeam(teamId: String, userId: String? = null): List<Player> {
        return client.postgrest["players"].select {
            filter {
                eq("team_id", teamId)
                if (!userId.isNullOrBlank()) {
                    eq("user_id", userId)
                }
            }
        }.decodeList<Player>()
    }

    suspend fun createPlayer(player: Player): Player {
        return client.postgrest["players"].insert(player) {
            select()
        }.decodeSingle<Player>()
    }

    suspend fun updatePlayer(player: Player) {
        player.id?.let { playerId ->
            client.postgrest["players"].update(player) {
                filter {
                    eq("id", playerId)
                }
            }
        }
    }

    suspend fun deletePlayer(playerId: String, userId: String? = null) {
        client.postgrest["players"].delete {
            filter {
                eq("id", playerId)
                if (!userId.isNullOrBlank()) {
                    eq("user_id", userId)
                }
            }
        }
    }

    // --- PARTIDOS ---
    suspend fun getMatches(mode: String? = null, userId: String? = null): List<Match> {
        return client.postgrest["matches"].select {
            filter {
                // Corregido: La columna en la BD se llama 'mode'
                if (!mode.isNullOrBlank()) {
                    eq("mode", mode)
                }
                if (!userId.isNullOrBlank()) {
                    eq("user_id", userId)
                }
            }
        }.decodeList<Match>()
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