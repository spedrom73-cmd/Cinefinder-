package com.cinefinder.app.v3

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.cinefinder.app.api.*

@Composable
private fun AccountRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Cf.Card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Cf.Gold)
        Spacer(Modifier.width(14.dp))
        Text(title, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(value, color = Cf.Grey, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Cf.Grey)
    }
}

@Composable
fun AccountScreen(
    wantCount: Int,
    historyCount: Int,
    favCount: Int,
    requestCount: Int,
    onNav: (String) -> Unit
) {
    var dialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { CfTopBar("Conta", null) }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Cf.Card)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Cf.Night),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Cf.Silver, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Modo local", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Os teus dados ficam só neste telemóvel.",
                        color = Cf.Grey,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Text(
                "Criar conta e sincronizar entre aparelhos ainda não está disponível.",
                color = Cf.Grey,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        item { AccountRow(Icons.Default.Bookmark, "Minha Lista", wantCount.toString()) { onNav("want") } }
        item { AccountRow(Icons.Default.Refresh, "Histórico de identificações", historyCount.toString()) { onNav("history") } }
        item { AccountRow(Icons.Default.Favorite, "Favoritos", favCount.toString()) { onNav("favs") } }
        item { AccountRow(Icons.Default.Check, "Já vistos", "") { onNav("seen") } }
        item { AccountRow(Icons.Default.Edit, "Pedidos de conteúdo", requestCount.toString()) { onNav("requests") } }
        item { AccountRow(Icons.Default.Settings, "Configurações", "") { onNav("settings") } }
        item { AccountRow(Icons.Default.Info, "Privacidade", "") { dialog = "privacy" } }
        item { AccountRow(Icons.Default.Info, "Ajuda e suporte", "") { dialog = "help" } }
        item { Spacer(Modifier.height(24.dp)) }
    }

    val dlg = dialog
    if (dlg != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Fechar") } },
            title = { Text(if (dlg == "privacy") "Privacidade" else "Ajuda e suporte") },
            text = {
                Text(
                    if (dlg == "privacy") {
                        "Os favoritos, listas e histórico ficam guardados apenas neste telemóvel. " +
                            "Quando identificas um vídeo, uma imagem ou uma descrição, esse conteúdo é enviado à Google (Gemini) para análise, e os títulos são procurados no TMDB. " +
                            "O CineFinder não recolhe contactos nem localização. Podes apagar tudo em Configurações > Sistema."
                    } else {
                        "Se a identificação falhar: confirma as chaves em Configurações > Sistema, usa vídeos curtos e claros, e tenta outra vez mais tarde se a IA estiver ocupada. " +
                            "Se os filmes não aparecerem, confirma a chave do TMDB e a ligação à Internet."
                    }
                )
            }
        )
    }
}

@Composable
fun MovieListScreen(
    title: String,
    list: List<Movie>,
    onBack: () -> Unit,
    onOpen: (Movie) -> Unit,
    onClear: () -> Unit
) {
    CfScreen {
        CfTopBar(title, onBack) {
            if (list.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Limpar") }
            }
        }
        if (list.isEmpty()) {
            CfEmpty("Esta lista está vazia. Abre um filme e usa os botões para o guardar aqui.")
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(list) { m -> MovieListItem(m, onOpen) }
        }
    }
}

@Composable
fun RequestsScreen(
    requests: List<String>,
    onAdd: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    CfScreen {
        CfTopBar("Pedidos de conteúdo", onBack) {
            if (requests.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("Limpar") }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Não encontraste um título? Escreve o nome e guarda o pedido. " +
                    "Os pedidos ficam guardados neste telemóvel; o envio para um servidor ainda não está disponível.",
                color = Cf.Grey,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Nome do filme ou série, e o que sabes dele") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            GoldButton(
                "Guardar pedido",
                {
                    if (text.isNotBlank()) {
                        onAdd(text.trim())
                        text = ""
                    }
                },
                Modifier.fillMaxWidth()
            )
        }
        SectionHeader("Pedidos anteriores")
        if (requests.isEmpty()) {
            CfEmpty("Ainda não fizeste pedidos.")
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(requests) { r ->
                val parts = r.split("|", limit = 2)
                val date = if (parts.size == 2) parts[0] else ""
                val body = if (parts.size == 2) parts[1] else r
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Cf.Card)
                        .padding(12.dp)
                ) {
                    Text(body)
                    Text("Guardado no telemóvel · " + date, color = Cf.Grey, fontSize = 11.sp)
                }
            }
        }
    }
}
