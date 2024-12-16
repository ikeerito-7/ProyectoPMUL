package com.iker.juegomemoria

import com.iker.juegomemoria.model.Card
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MemoryGame()
        }
    }
}

@Composable
fun MemoryGame() {
    var gameStarted by remember { mutableStateOf(false) }
    var cards by remember { mutableStateOf(generateCards()) }
    var selectedCards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var timeRemaining by remember { mutableStateOf(2 * 60) }
    var isGameCompleted by remember(cards) { mutableStateOf(cards.all { it.isMatched }) }
    var gameActive by remember { mutableStateOf(true) }
    var gameHistory by remember { mutableStateOf<List<GameResult>>(emptyList()) }
    var resultMessage by remember { mutableStateOf("") } // Nuevo estado para el mensaje

    val scope = rememberCoroutineScope()

    if (!gameStarted) {
        // Pantalla inicial
        IntroScreen(onStart = { gameStarted = true })
    } else {
        // Pantalla del juego
        LaunchedEffect(gameActive) {
            if (gameActive) {
                while (timeRemaining > 0 && !isGameCompleted) {
                    delay(1000)
                    timeRemaining -= 1
                }
                if (!isGameCompleted) gameActive = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5DC)) // Fondo beige
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!gameActive || isGameCompleted) {
                // Agregar resultado de la partida al historial
                val currentResult = GameResult(
                    score = score,
                    isVictory = isGameCompleted,
                    timeRemaining = timeRemaining
                )
                if (!gameHistory.contains(currentResult)) {
                    gameHistory = listOf(currentResult) + gameHistory.take(4)
                }

                EndGameScreen(
                    isGameCompleted = isGameCompleted,
                    score = score,
                    onRestart = {
                        // Reiniciar el juego y volver a la pantalla de inicio
                        gameStarted = false
                        cards = generateCards()
                        score = 0
                        timeRemaining = 2 * 60
                        selectedCards = emptyList()
                        isGameCompleted = false
                        gameActive = true
                        resultMessage = "" // Reiniciar mensaje
                    },
                    gameHistory = gameHistory
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopBar(timeRemaining, score)

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(cards.size) { index ->
                            val card = cards[index]
                            CardView(card = card, onClick = {
                                if (selectedCards.size < 2 && !card.isFaceUp && !card.isMatched) {
                                    scope.launch {
                                        handleCardClick(
                                            card = card,
                                            cards = cards,
                                            selectedCards = selectedCards,
                                            onFlip = { updatedCards -> cards = updatedCards },
                                            onMatch = {
                                                score += 10 // Incrementar 10 puntos por coincidencia
                                                resultMessage = "¡Correcto!" // Mostrar mensaje de acierto
                                                selectedCards = emptyList()
                                                isGameCompleted = cards.all { it.isMatched }
                                                if (isGameCompleted) gameActive = false
                                            },
                                            onMismatch = {
                                                resultMessage = "¡Incorrecto!" // Mostrar mensaje de fallo
                                                delay(1000)
                                                cards = cards.map {
                                                    if (it.id == selectedCards[0].id || it.id == selectedCards[1].id) it.copy(isFaceUp = false)
                                                    else it
                                                }
                                                selectedCards = emptyList()
                                            },
                                            updateSelection = { newSelection -> selectedCards = newSelection }
                                        )
                                    }
                                }
                            })
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mostrar el mensaje de resultado
                    Text(
                        text = resultMessage,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (resultMessage == "¡Correcto!") Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = {
                            // Reiniciar el juego y volver a la pantalla de inicio
                            gameStarted = false
                            cards = generateCards()
                            score = 0
                            timeRemaining = 2 * 60
                            selectedCards = emptyList()
                            isGameCompleted = false
                            gameActive = true
                            resultMessage = "" // Reiniciar mensaje
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D9DC5)) // Azul claro
                    ) {
                        Text(text = "Reiniciar", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}


@Composable
fun TopBar(timeRemaining: Int, score: Int) {
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tiempo: ${"%02d".format(minutes)}:${"%02d".format(seconds)}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D9DC5)
        )
        Text(
            text = "Puntuación: $score",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6D9DC5)
        )
    }
}


@Composable
fun EndGameScreen(
    isGameCompleted: Boolean,
    score: Int,
    onRestart: () -> Unit,
    gameHistory: List<GameResult>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (isGameCompleted) {
            Text(
                text = "¡Enhorabuena!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "¡Ha completado el juego!",
                fontSize = 20.sp,
                color = Color.Gray
            )
        } else {
            Text(
                text = "¡Tiempo finalizado!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336)
            )
        }
        Text(
            text = "Puntuación final: $score",
            fontSize = 20.sp,
            color = Color(0xFF3949AB)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .padding(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(text = "Jugar de nuevo", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Historial de resultados (Últimos 5)",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3949AB)
        )
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(gameHistory.size) { index ->
                val result = gameHistory[index]
                Column {
                    Text(
                        text = if (result.isVictory) {
                            "Victoria: ${result.score} puntos (Tiempo restante: ${result.timeRemaining} seg)"
                        } else if (result.timeRemaining == 0) {
                            "Derrota: ${result.score} puntos"
                        } else {
                            "En progreso: ${result.score} puntos (Tiempo restante: ${result.timeRemaining} seg)"
                        },
                        fontSize = 16.sp,
                        color = if (result.isVictory) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Divider(color = Color.Gray, thickness = 1.dp)
                }
            }
        }
    }
}

fun generateCards(): List<Card> {
    val contents = (1..10).map { it.toString() }
    return (contents + contents).shuffled().mapIndexed { index, content ->
        Card(id = index, content = content, isFaceUp = false, isMatched = false, isMismatched = false)
    }
}


suspend fun handleCardClick(
    card: Card,
    cards: List<Card>,
    selectedCards: List<Card>,
    onFlip: (List<Card>) -> Unit,
    onMatch: () -> Unit,
    onMismatch: suspend () -> Unit,
    updateSelection: (List<Card>) -> Unit
) {
    // Voltear la carta seleccionada
    onFlip(cards.map { if (it.id == card.id) it.copy(isFaceUp = true) else it })

    val updatedSelection = selectedCards + card
    updateSelection(updatedSelection)

    if (updatedSelection.size == 2) {
        if (updatedSelection[0].content == updatedSelection[1].content) {
            // Si hay coincidencia, marcar las cartas como emparejadas
            onFlip(cards.map {
                if (it.id == updatedSelection[0].id || it.id == updatedSelection[1].id) it.copy(isMatched = true)
                else it
            })
            onMatch()
        } else {
            // Voltear ambas cartas para que se muestren antes de aplicar el retraso
            onFlip(cards.map {
                if (it.id == updatedSelection[0].id || it.id == updatedSelection[1].id) it.copy(isFaceUp = true, isMismatched = true)
                else it
            })

            delay(1000) // Esperar 1 segundo para que el usuario vea las cartas

            // Volver a voltear las cartas y quitar el estado de no coincidencia
            onFlip(cards.map {
                if (it.id == updatedSelection[0].id || it.id == updatedSelection[1].id) it.copy(isFaceUp = false, isMismatched = false)
                else it
            })

            onMismatch()
        }
    }
}


data class GameResult(
    val score: Int,
    val isVictory: Boolean,
    val timeRemaining: Int
)

@Composable
fun CardView(card: Card, onClick: () -> Unit) {
    val backgroundColor = when {
        card.isMatched -> Color(0xFF4CAF50)
        card.isMismatched -> Color(0xFFF44336)
        card.isFaceUp -> Color(0xFFE0F7FA)
        else -> Color(0xFFE0E0E0)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .shadow(4.dp)
            .background(backgroundColor)
            .clickable(enabled = !card.isMatched && !card.isFaceUp) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (card.isFaceUp || card.isMatched) {
            Text(text = card.content, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        } else {
            Text(text = "❓", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}




@Composable
fun IntroScreen(onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5DC)) // Fondo beige
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido al Juego de Memoria",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Encuentra todas las parejas de números antes de que el tiempo se acabe.",
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onStart) {
                Text(text = "Iniciar Juego")
            }
        }
    }
}
