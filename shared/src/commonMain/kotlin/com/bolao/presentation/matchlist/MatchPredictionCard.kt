package com.bolao.presentation.matchlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bolao.domain.model.GameStatus
import com.bolao.domain.model.Team
import com.bolao.domain.usecase.PredictionCalculator
import com.bolao.presentation.theme.BolaoGold
import com.bolao.presentation.theme.BolaoGoldDark
import com.bolao.presentation.theme.BolaoGoldLight
import com.bolao.presentation.theme.BolaoGreen
import com.bolao.presentation.theme.DarkCardVariant
import com.bolao.presentation.theme.HalfTimeAmber
import com.bolao.presentation.theme.LiveRed
import com.bolao.presentation.theme.LiveRedMuted
import androidx.compose.foundation.layout.fillMaxSize
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ═══════════════════════════════════════════════════════════════════════════════
// MatchPredictionCard — componente público
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Card de palpite para uma partida de futebol.
 *
 * ## Layout
 * ```
 * ┌─────────────────────────────────────────────────┐
 * │  🏆 Copa do Mundo · Rodada     [● AO VIVO 67'] │  ← Header
 * │  Sex, 20/06 · 16:00                             │
 * ├─────────────────────────────────────────────────┤
 * │                                                 │
 * │  [Logo]       [-]  2  [+]  ×  [-]  1  [+]  [Logo]│  ← Teams + Counters
 * │  Brasil                              Argentina  │
 * │                                                 │
 * ├─────────────────────────────────────────────────┤
 * │         [⭐  Confirmar Palpite  ]              │  ← Footer (se editável)
 * │         ✦ 5 pontos ganhos ✦                    │  ← Footer (se finalizado)
 * └─────────────────────────────────────────────────┘
 * ```
 *
 * ## Regras de UI
 * - `GameStatus.Scheduled` → counters interativos + botão Confirmar
 * - `GameStatus.Live` / `HalfTime` → counters estáticos (palpite bloqueado)
 * - `GameStatus.Finished` → counters estáticos + badge de pontos
 * - `GameStatus.Interrupted` → counters estáticos + badge de status
 *
 * @param item                  Estado combinado (partida + palpite + draft)
 * @param onHomeGoalIncrement   Callback para incrementar gols do mandante
 * @param onHomeGoalDecrement   Callback para decrementar gols do mandante
 * @param onAwayGoalIncrement   Callback para incrementar gols do visitante
 * @param onAwayGoalDecrement   Callback para decrementar gols do visitante
 * @param onSave                Callback para persistir o palpite atual
 */
@Composable
fun MatchPredictionCard(
    item: MatchPredictionItem,
    onHomeGoalIncrement: () -> Unit,
    onHomeGoalDecrement: () -> Unit,
    onAwayGoalIncrement: () -> Unit,
    onAwayGoalDecrement: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditable = item.match.isPredictionAllowed

    ElevatedCard(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
    ) {
        Column {

            // ── Header: competição, data e badge de status ─────────────────
            MatchCardHeader(
                competition = item.match.competition,
                round       = item.match.round,
                scheduledAt = item.match.scheduledAt,
                status      = item.match.status,
                modifier    = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                DarkCardVariant,
                                DarkCardVariant.copy(alpha = 0.6f),
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // ── Teams + Counters ──────────────────────────────────────────
            Row(
                modifier                = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 24.dp),
                verticalAlignment       = Alignment.CenterVertically,
                horizontalArrangement   = Arrangement.SpaceEvenly,
            ) {
                // Mandante (home)
                TeamColumn(
                    team        = item.match.homeTeam,
                    goalCount   = item.currentHomeGoals,
                    isEditable  = isEditable,
                    onIncrement = onHomeGoalIncrement,
                    onDecrement = onHomeGoalDecrement,
                    modifier    = Modifier.weight(1f),
                )

                // Separador "×"
                Text(
                    text  = "×",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                // Visitante (away)
                TeamColumn(
                    team        = item.match.awayTeam,
                    goalCount   = item.currentAwayGoals,
                    isEditable  = isEditable,
                    onIncrement = onAwayGoalIncrement,
                    onDecrement = onAwayGoalDecrement,
                    modifier    = Modifier.weight(1f),
                )
            }

            // ── Potencial de Pontos (Real Time) ──
            if (isEditable) {
                val potential = PredictionCalculator.calculatePotential(
                    predHomeGoals = item.currentHomeGoals,
                    predAwayGoals = item.currentAwayGoals,
                    stageMultiplier = item.match.stageMultiplier,
                    homeOdd = item.match.homeOdd,
                    drawOdd = item.match.drawOdd,
                    awayOdd = item.match.awayOdd
                )

                if (potential == null) {
                    Text(
                        text = "⏳ Multiplicadores de zebra liberados de 5 a 7 dias antes do jogo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (potential.isZebra) {
                            Text(
                                text = "🔥 Zebra! Potencial: ${potential.maxPotentialPoints} pontos",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                text = "⭐ Potencial máximo: ${potential.maxPotentialPoints} pontos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Placar Real — exibido centralizado quando o jogo não está aberto ──
            val homeScore = item.match.homeScore
            val awayScore = item.match.awayScore
            if (!isEditable && homeScore != null && awayScore != null) {
                RealScoreLabel(
                    homeScore = homeScore,
                    awayScore = awayScore,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 4.dp),
                )
            }

            // ── Footer: save / pontos / sem palpite ───────────────────────
            MatchCardFooter(
                item       = item,
                isEditable = isEditable,
                onSave     = onSave,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Subcomponentes privados
// ═══════════════════════════════════════════════════════════════════════════════

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun MatchCardHeader(
    competition: String,
    round: String,
    scheduledAt: Instant,
    status: GameStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text      = competition,
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.primary,
                fontWeight= FontWeight.Bold,
            )
            Text(
                text  = "$round · ${formatMatchDateTime(scheduledAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        StatusBadge(status = status)
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

/**
 * Badge visual do estado da partida.
 * - Scheduled → nenhum badge (a data já é visível no header)
 * - Live       → badge vermelho pulsante com o minuto
 * - HalfTime   → badge âmbar estático
 * - Finished   → badge cinza "Encerrado"
 * - Interrupted→ badge vermelho com motivo
 */
@Composable
private fun StatusBadge(status: GameStatus) {
    when (status) {
        is GameStatus.Scheduled -> Unit // Sem badge — a data já informa

        is GameStatus.Live ->
            LiveBadge(minute = status.minutePlayed)

        is GameStatus.HalfTime ->
            StatusPill(
                text  = "Intervalo",
                color = HalfTimeAmber,
            )

        is GameStatus.Finished ->
            StatusPill(
                text  = "Encerrado",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

        is GameStatus.Interrupted ->
            StatusPill(
                text  = status.reason,
                color = LiveRed,
            )
    }
}

/** Badge "AO VIVO" com ponto pulsante para chamar atenção. */
@Composable
private fun LiveBadge(minute: Int) {
    val transition = rememberInfiniteTransition(label = "LivePulse")
    val alpha by transition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.25f,
        animationSpec = InfiniteRepeatableSpec(
            animation  = tween(750, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "LiveAlpha",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = LiveRedMuted,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            // Ponto pulsante
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(LiveRed.copy(alpha = alpha))
            )
            Text(
                text       = "AO VIVO · $minute'",
                style      = MaterialTheme.typography.labelSmall,
                color      = LiveRed,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Pill genérica para status de partida. */
@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text      = text,
            style     = MaterialTheme.typography.labelSmall,
            color     = color,
            fontWeight= FontWeight.SemiBold,
            modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ── Team + GoalCounter ────────────────────────────────────────────────────────

/**
 * Coluna com logo do time, nome e contador de gols.
 * O layout é centrado horizontalmente para ambos os lados ficarem simétricos.
 */
@Composable
private fun TeamColumn(
    team: Team,
    goalCount: Int,
    isEditable: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        // Logo do time carregado via proxy ou placeholder se não disponível
        TeamLogoImage(team = team)

        // Nome do time — truncado para não quebrar o layout
        Text(
            text      = team.name,
            style     = MaterialTheme.typography.titleSmall,
            fontWeight= FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )

        // Contador de gols — interativo ou estático dependendo do status
        GoalCounter(
            count       = goalCount,
            isEditable  = isEditable,
            onIncrement = onIncrement,
            onDecrement = onDecrement,
        )
    }
}

/**
 * Placeholder circular para o escudo do time.
 * Usa gradiente com o shortName em negrito.
 * Substitua por `AsyncImage` (Coil 3) quando as URLs estiverem disponíveis.
 */
@Composable
private fun TeamLogoPlaceholder(
    team: Team,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier          = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                )
            ),
        contentAlignment  = Alignment.Center,
    ) {
        Text(
            text      = team.shortName.take(3),
            style     = MaterialTheme.typography.labelMedium,
            fontWeight= FontWeight.ExtraBold,
            color     = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Tenta carregar o escudo do time via proxy de imagem da API.
 * Se apiTeamId for nulo/vazio, ou o carregamento falhar, cai de volta para o placeholder.
 */
@Composable
private fun TeamLogoImage(
    team: Team,
    modifier: Modifier = Modifier,
) {
    val apiTeamId = team.apiTeamId
    if (!apiTeamId.isNullOrBlank()) {
        val logoUrl = "https://sports.bzzoiro.com/img/team/$apiTeamId/"
        val resource = asyncPainterResource(data = logoUrl)
        Box(
            modifier = modifier
                .size(56.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            KamelImage(
                resource = { resource },
                contentDescription = "Escudo do ${team.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                onLoading = { _ ->
                    ShimmerPlaceholder()
                },
                onFailure = { exception ->
                    TeamLogoPlaceholder(team = team)
                }
            )
        }
    } else {
        TeamLogoPlaceholder(team = team, modifier = modifier)
    }
}

/**
 * Efeito de shimmer simples (pulsação de alpha) para o carregamento da imagem.
 */
@Composable
private fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ShimmerAlpha",
    )
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
    )
}

/**
 * Contador de gols interativo (+/-) ou estático.
 *
 * ## Modo interativo (`isEditable = true`)
 * - FilledTonalIconButton "–" (desabilitado quando count = 0)
 * - Número central com AnimatedContent (slide up/down dependendo da direção)
 * - FilledIconButton "+" (sempre habilitado)
 *
 * ## Modo estático (`isEditable = false`)
 * - Só exibe o número — sem botões — para não confundir o usuário
 *
 * A transição entre os modos usa AnimatedContent com fadeIn/fadeOut.
 */
@Composable
private fun GoalCounter(
    count: Int,
    isEditable: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = isEditable,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "GoalCounterMode",
        modifier = modifier,
    ) { editable ->
        if (editable) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Botão decremento — desabilitado em 0
                FilledTonalIconButton(
                    onClick  = onDecrement,
                    enabled  = count > 0,
                    modifier = Modifier.size(36.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarkCardVariant,
                    ),
                ) {
                    Icon(
                        imageVector      = Icons.Rounded.Remove,
                        contentDescription = "Diminuir gols",
                        modifier         = Modifier.size(18.dp),
                    )
                }

                // Número animado — slide para cima ao incrementar, para baixo ao decrementar
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInVertically { it * direction } + fadeIn()) togetherWith
                            (slideOutVertically { -it * direction } + fadeOut())
                    },
                    label = "GoalCount",
                    modifier = Modifier.widthIn(min = 44.dp),
                ) { goalCount ->
                    Text(
                        text      = goalCount.toString(),
                        style     = MaterialTheme.typography.headlineMedium,
                        fontWeight= FontWeight.Black,
                        color     = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }

                // Botão incremento — sempre habilitado
                FilledIconButton(
                    onClick  = onIncrement,
                    modifier = Modifier.size(36.dp),
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = BolaoGreen.copy(alpha = 0.85f),
                        contentColor   = Color.Black,
                    ),
                ) {
                    Icon(
                        imageVector      = Icons.Rounded.Add,
                        contentDescription = "Aumentar gols",
                        modifier         = Modifier.size(18.dp),
                    )
                }
            }
        } else {
            // Modo estático: só o número, centralizado
            Box(
                modifier          = Modifier.widthIn(min = 44.dp),
                contentAlignment  = Alignment.Center,
            ) {
                Text(
                    text      = count.toString(),
                    style     = MaterialTheme.typography.headlineMedium,
                    fontWeight= FontWeight.Black,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── RealScoreLabel ────────────────────────────────────────────────────────────

/**
 * Label centralizado exibindo o placar real da partida.
 * Visível apenas quando o jogo não está mais aberto para palpites
 * (Live, HalfTime, Finished, Interrupted) e os scores estão disponíveis.
 *
 * Posicionado entre a área de times e o footer para não duplicar informação
 * dentro dos [TeamColumn]s individuais.
 */
@Composable
private fun RealScoreLabel(
    homeScore: Int,
    awayScore: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text      = "Placar real:",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text       = "$homeScore × $awayScore",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun MatchCardFooter(
    item: MatchPredictionItem,
    isEditable: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {

        // Mensagem de erro de save (com animação de aparecimento)
        AnimatedVisibility(visible = item.saveError != null) {
            Text(
                text     = item.saveError.orEmpty(),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        when {
            // 1. Jogo encerrado E palpite com pontuação → badge de pontos
            item.savedPrediction?.pointsEarned != null ->
                PointsBadge(points = item.savedPrediction.pointsEarned!!)

            // 2. Jogo aberto → botão de salvar
            isEditable -> SaveButton(
                hasChanges = item.hasUnsavedChanges,
                isSaving   = item.isSaving,
                isUpdate   = item.savedPrediction != null,
                onSave     = onSave,
            )

            // 3. Jogo em andamento sem palpite prévio → aviso
            item.savedPrediction == null ->
                Text(
                    text     = "Você não fez um palpite para esta partida",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign= TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

/** Badge dourado exibindo os pontos ganhos após o jogo encerrar. */
@Composable
private fun PointsBadge(points: Int) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BolaoGoldDark.copy(alpha = 0.8f))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector      = Icons.Rounded.Star,
            contentDescription = null,
            tint             = BolaoGold,
            modifier         = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text      = if (points == 1) "1 ponto ganho" else "$points pontos ganhos",
            style     = MaterialTheme.typography.titleSmall,
            fontWeight= FontWeight.Bold,
            color     = BolaoGoldLight,
        )
    }
}

/**
 * Botão de confirmação do palpite.
 *
 * @param hasChanges Habilita o botão apenas quando há edições não salvas
 * @param isSaving   Exibe spinner e desabilita durante o save
 * @param isUpdate   Altera o label para "Atualizar" se já havia palpite salvo
 */
@Composable
private fun SaveButton(
    hasChanges: Boolean,
    isSaving: Boolean,
    isUpdate: Boolean,
    onSave: () -> Unit,
) {
    Button(
        onClick  = onSave,
        enabled  = hasChanges && !isSaving,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = BolaoGreen,
            contentColor   = Color.Black,
        ),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier    = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color       = Color.Black,
            )
        } else {
            Icon(
                imageVector = if (isUpdate) Icons.Rounded.Check else Icons.Rounded.Star,
                contentDescription = null,
                modifier    = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text      = if (isUpdate) "Atualizar Palpite" else "Confirmar Palpite",
                style     = MaterialTheme.typography.labelLarge,
                fontWeight= FontWeight.Bold,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Utilitário de formatação de data/hora
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Formata um [Instant] para exibição no horário local do dispositivo.
 * Exemplo: "Sex, 20/06 · 16:00"
 */
private fun formatMatchDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val dayAbbr = when (local.dayOfWeek.value) {
        1 -> "Seg"; 2 -> "Ter"; 3 -> "Qua"
        4 -> "Qui"; 5 -> "Sex"; 6 -> "Sáb"
        else -> "Dom"
    }
    val day   = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    val hour  = local.hour.toString().padStart(2, '0')
    val min   = local.minute.toString().padStart(2, '0')

    return "$dayAbbr, $day/$month · $hour:$min"
}
