package com.florosoft.florobeat.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.florosoft.florobeat.ui.icons.FloroBeatIcons
import com.florosoft.florobeat.ui.theme.FloroDarkCard
import com.florosoft.florobeat.ui.theme.FloroDarkCardBorder
import com.florosoft.florobeat.ui.theme.FloroMint
import com.florosoft.florobeat.ui.theme.FloroMintLight
import com.florosoft.florobeat.ui.theme.FloroObsidian
import com.florosoft.florobeat.ui.theme.FloroTextSecondary

/**
 * Dedicated first-launch and unauthenticated Welcome / Sign-In screen.
 *
 * Implements the FloroBeat cinematic visual identity with glowing waveform branding,
 * product value propositions, and direct "Continue with Google" authentication.
 */
@Composable
fun WelcomeSignInScreen(
    onSignInWithGoogle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseGlow",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FloroObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Atmospheric radial aura
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FloroMint.copy(alpha = pulseGlow),
                            Color(0xFF024E40).copy(alpha = pulseGlow * 0.7f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(16.dp))

            // Center Branding & Features
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // FloroBeat Brand Mark Emblem
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF14242E), Color(0xFF0B141B))
                            )
                        )
                        .border(1.5.dp, FloroMint.copy(alpha = 0.45f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.florosoft.florobeat.R.drawable.florobeat_foreground),
                        contentDescription = "FloroBeat",
                        modifier = Modifier.size(60.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Welcome to",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        color = FloroTextSecondary,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                    ),
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Floro",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                    )
                    Text(
                        text = "Beat",
                        color = FloroMint,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Your music. Your world.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Normal,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(36.dp))

                // Feature highlight pills
                FeatureHighlightRow(
                    icon = FloroBeatIcons.Equalizer,
                    title = "Lossless Audio",
                    description = "High-fidelity streaming & offline downloads",
                )
                Spacer(Modifier.height(14.dp))
                FeatureHighlightRow(
                    icon = FloroBeatIcons.QueueMusic,
                    title = "Smart Automix",
                    description = "Seamless track transitions & smart mixes",
                )
                Spacer(Modifier.height(14.dp))
                FeatureHighlightRow(
                    icon = FloroBeatIcons.LyricsQuote,
                    title = "Synced Lyrics",
                    description = "Real-time, word-by-word karaoke playback",
                )
            }

            // Bottom Actions & Legal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                // "Continue with Google" Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(FloroDarkCard)
                        .border(1.5.dp, FloroMint.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                        .clickable(onClick = onSignInWithGoogle),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = FloroBeatIcons.Google,
                            contentDescription = "Google",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = "By continuing, you agree to the applicable terms and policies.",
                    color = FloroTextSecondary,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Developed & maintained by FloroSoft",
                    color = FloroMint.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FloroDarkCard.copy(alpha = 0.7f))
            .border(1.dp, FloroDarkCardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(FloroMint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FloroMint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = FloroTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
