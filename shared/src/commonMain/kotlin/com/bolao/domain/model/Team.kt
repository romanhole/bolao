package com.bolao.domain.model

import kotlinx.serialization.Serializable

/**
 * Entidade de domínio que representa um time de futebol.
 *
 * [logoUrl] aponta para um asset armazenado no nosso backend (Firebase Storage
 * ou Supabase Storage), nunca diretamente a uma API externa de futebol.
 */
@Serializable
data class Team(
    val id: String,
    val name: String,
    val shortName: String,
    val logoUrl: String,
    val apiTeamId: String? = null,
)
