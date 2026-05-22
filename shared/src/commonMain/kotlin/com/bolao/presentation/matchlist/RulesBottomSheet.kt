package com.bolao.presentation.matchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bolao.presentation.theme.BolaoGold
import com.bolao.presentation.theme.BolaoGreen
import com.bolao.presentation.theme.DarkCardVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = BolaoGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Regras de Pontuação",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Formula card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(DarkCardVariant, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pontos = (Base × Multiplicador da Fase) + Zebra",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = BolaoGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Base points
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "1. Pontuação Base",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Você só pontua se acertar a tendência (Vencedor ou Empate).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                RuleRow(title = "Placar Exato", value = "5 pts", desc = "1 pt da tendência + 2 pts pelo saldo do mandante + 2 pts pelo saldo do visitante.")
                RuleRow(title = "Tendência + 1 Saldo", value = "3 pts", desc = "Acertou o vencedor/empate + gols de um dos times (Ex: Palpite 3-1, Placar 2-1).")
                RuleRow(title = "Apenas Tendência", value = "1 pt", desc = "Acertou apenas quem venceu ou que seria empate (Ex: Palpite 2-0, Placar 3-1).")
                RuleRow(title = "Erro", value = "0 pts", desc = "Errou o vencedor ou empate.")
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))

            // Zebra points
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "2. Bônus de Zebra em Faixas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Se você acertar a tendência (Vitória ou Empate), ganha um bônus se o resultado for improvável (Zebra), de acordo com a odd correspondente:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ZebraRow(oddRange = "Odd 3.00 a 4.99", bonus = "+2 pts")
                ZebraRow(oddRange = "Odd 5.00 a 8.99", bonus = "+4 pts")
                ZebraRow(oddRange = "Odd >= 9.00", bonus = "+7 pts")
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant))

            // Info warning
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "As odds são atualizadas diariamente e congeladas 48h antes do jogo. Palpites feitos após esse prazo usarão os multiplicadores congelados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun RuleRow(
    title: String,
    value: String,
    desc: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ZebraRow(
    oddRange: String,
    bonus: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Rounded.TrendingUp,
                contentDescription = null,
                tint = BolaoGold,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = oddRange,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Surface(
            color = BolaoGold.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = bonus,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = BolaoGold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
