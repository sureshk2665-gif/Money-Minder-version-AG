package com.example.moneyminder.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moneyminder.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.3f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            glowAlpha.animateTo(0.6f, tween(400))
            glowScale.animateTo(1.8f, tween(800))
        }
        launch {
            alpha.animateTo(1f, tween(350))
        }
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(500)
        launch {
            alpha.animateTo(0f, tween(250))
            glowAlpha.animateTo(0f, tween(250))
        }
        scale.animateTo(
            targetValue = 1.15f,
            animationSpec = tween(300)
        )
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(glowScale.value)
                .graphicsLayer { this.alpha = glowAlpha.value }
                .blur(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A3A))
        )

        Image(
            painter = painterResource(id = R.drawable.logo_money_minder),
            contentDescription = "Money Minder Logo",
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
            contentScale = ContentScale.Fit
        )
    }
}
