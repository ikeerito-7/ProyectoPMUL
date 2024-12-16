package com.iker.juegomemoria.model

data class Card(
    val id: Int,
    val content: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false,
    var isMismatched: Boolean = false
)