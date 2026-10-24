package com.cinefinder.app.model

data class ReferenceMovie(
    val id: String,
    val title: String,
    val hash: Long,
    val imagePath: String
)

data class MatchResult(
    val movie: ReferenceMovie,
    val similarityPercentage: Double
)