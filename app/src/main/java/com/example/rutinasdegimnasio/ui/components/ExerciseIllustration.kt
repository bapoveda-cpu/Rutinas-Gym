package com.example.rutinasdegimnasio.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ExerciseIllustration(
    modifier: Modifier = Modifier,
    title: String,
    startPhaseImageRes: Int? = null,
    finishPhaseImageRes: Int? = null
) {
    var showStartPhase by remember { mutableStateOf(true) }

    // Simula animación de ejercicio cambiando entre fase 1 y 2 cada segundo
    LaunchedEffect(Unit) {
        if (startPhaseImageRes != null && finishPhaseImageRes != null) {
            while (true) {
                delay(1000L)
                showStartPhase = !showStartPhase
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        val currentImage = if (showStartPhase) startPhaseImageRes else finishPhaseImageRes ?: startPhaseImageRes

        if (currentImage != null && currentImage != 0) {
            Image(
                painter = painterResource(id = currentImage),
                contentDescription = title,
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            // Placeholder si no hay imagen
            Text(
                text = "ILUSTRACIÓN TÁCTICA\nPENDIENTE",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray
            )
        }
    }
}
