package com.gateopenerz.paperserver

enum class ServerType {
    PAPER,
    VELOCITY,
    FOLIA,
    PURPUR,
    ADVANCED_SLIME_PAPER;

    companion object {
        fun fromString(value: String): ServerType {
            return when (value.lowercase().replace("-", "_").replace(" ", "_")) {
                "paper" -> PAPER
                "velocity" -> VELOCITY
                "folia" -> FOLIA
                "purpur" -> PURPUR
                "advanced_slime_paper", "asp" -> ADVANCED_SLIME_PAPER
                else -> PAPER
            }
        }
    }
}