package com.cinefinder.app.v3

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.cinefinder.app.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class IdState {
    var busy by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var status by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var found by mutableStateOf<List<Movie>>(emptyList())
    var done by mutableStateOf(false)
    var job: Job? = null

    fun reset() {
        busy = false
        error = null
        found = emptyList()
        done = false
        progress = 0f
        status = ""
    }

    fun cancel() {
        job?.cancel()
        busy = false
        status = ""
    }

    fun run(
        scope: CoroutineScope,
        tmdbKey: String,
        onFound: (List<Movie>) -> Unit,
        block: suspend ((Float, String) -> Unit) -> List<String>
    ) {
        reset()
        busy = true
        job = scope.launch {
            try {
                val titles = block { p, s ->
                    progress = p
                    status = s
                }
                val list = ArrayList<Movie>()
                for ((i, t) in titles.withIndex()) {
                    status = "A procurar \"" + t + "\"..."
                    progress = 0.8f + 0.2f * (i + 1) / maxOf(titles.size, 1)
                    val r = Api.search(tmdbKey, t).firstOrNull()
                    if (r != null && list.none { it.id == r.id && it.isTv == r.isTv }) list.add(r)
                }
                found = list
                done = true
                if (list.isNotEmpty()) onFound(list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Erro: " + (e.message ?: "falhou")
            }
            busy = false
        }
    }
}

@Composable
private fun OptionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Cf.Card)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Cf.Blue),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = Cf.Grey, fontSize = 12.sp)
        }
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Cf.Grey)
    }
}

@Composable
fun IdentifyScreen(
    tmdbKey: String,
    geminiKey: String,
    incoming: Uri?,
    onConsumed: () -> Unit,
    favs: List<Movie>,
    onFav: (Movie) -> Unit,
    onFound: (List<Movie>) -> Unit,
    onOpen: (Movie) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { IdState() }
    var mode by remember { mutableStateOf("hub") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var text by remember { mutableStateOf("") }

    fun ready(): Boolean {
        if (geminiKey.isBlank() || tmdbKey.isBlank()) {
            state.error = "Escreve primeiro as duas chaves em Conta > Configurações > Sistema."
            return false
        }
        return true
    }

    fun startVideo(uri: Uri) {
        if (ready()) {
            state.run(scope, tmdbKey, onFound) { p -> Api.identify(context, uri, geminiKey, p) }
        }
    }

    fun startImage(hint: String) {
        val u = imageUri ?: return
        if (ready()) {
            state.run(scope, tmdbKey, onFound) { p -> Api3.identifyImage(context, u, geminiKey, hint, p) }
        }
    }

    fun startText() {
        if (text.isBlank()) return
        if (ready()) {
            state.run(scope, tmdbKey, onFound) { _ -> Api2.describe(geminiKey, text) }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) startVideo(uri)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) imageUri = uri
    }

    LaunchedEffect(incoming) {
        if (incoming != null) {
            mode = "video"
            startVideo(incoming)
            onConsumed()
        }
    }

    BackHandler(enabled = mode != "hub") {
        state.cancel()
        state.reset()
        mode = "hub"
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (mode == "hub") {
            CfTopBar("Identificar Conteúdo", null)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Cf.Card)
                    .padding(16.dp)
            ) {
                Text("Escolhe como queres identificar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Envia um vídeo, uma imagem, descreve uma cena ou combina pistas para encontrarmos o que procuras.",
                    color = Cf.Grey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            OptionCard(Icons.Default.VideoFile, "Vídeo", "Analisa um vídeo da tua galeria") { mode = "video" }
            OptionCard(Icons.Default.Image, "Imagem", "Envia um cartaz, fotograma ou captura de ecrã") { mode = "image" }
            OptionCard(Icons.Default.Edit, "Lembrança", "Descreve o que te lembras da cena") { mode = "memory" }
            OptionCard(Icons.Default.Search, "Pesquisa combinada", "Imagem + descrição") { mode = "combo" }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33FFD21F))
                    .padding(14.dp)
            ) {
                Text("Dica", color = Cf.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Quanto mais detalhes deres, mais fácil será encontrar o título certo.",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            val err = state.error
            if (err != null) {
                Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
        } else {
            val title = when (mode) {
                "video" -> "Identificar por Vídeo"
                "image" -> "Identificar por Imagem"
                "memory" -> "Identificar por Lembrança"
                else -> "Pesquisa combinada"
            }
            CfTopBar(title, {
                state.cancel()
                state.reset()
                mode = "hub"
            })
            if (mode == "video") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BlueButton("Escolher vídeo da galeria", { videoPicker.launch("video/*") }, Modifier.fillMaxWidth(), !state.busy)
                    Spacer(Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Cf.Card)
                            .padding(16.dp)
                    ) {
                        Text("Dicas para melhores resultados:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        listOf(
                            "Usa vídeos curtos (até 60 segundos)",
                            "Evita vídeos com muita música ou texto por cima",
                            "Escolhe cenas com caras ou cenários bem visíveis",
                            "Podes partilhar um vídeo da Galeria diretamente para o CineFinder"
                        ).forEach {
                            Text("✓ " + it, color = Cf.Grey, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            if (mode == "image" || mode == "combo") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val u = imageUri
                    if (u != null) {
                        AsyncImage(
                            model = u,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Cf.Card)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    BlueButton(
                        if (u == null) "Escolher imagem" else "Trocar imagem",
                        { imagePicker.launch("image/*") },
                        Modifier.fillMaxWidth(),
                        !state.busy
                    )
                    if (mode == "combo") {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Ex.: esta cena acontece num hospital durante a noite") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GoldButton(
                        "Analisar",
                        { startImage(if (mode == "combo") text else "") },
                        Modifier.fillMaxWidth()
                    )
                }
            }
            if (mode == "memory") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Ex.: um rapaz descobre poderes especiais numa escola e enfrenta um grupo secreto") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    GoldButton("Procurar pela descrição", { startText() }, Modifier.fillMaxWidth())
                    Text(
                        "Dica: indica personagens, local, época, género ou algo marcante.",
                        color = Cf.Grey,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            IdPanel(state, favs, onFav, onOpen)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun IdPanel(s: IdState, favs: List<Movie>, onFav: (Movie) -> Unit, onOpen: (Movie) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (s.busy) {
            Spacer(Modifier.height(10.dp))
            Text(s.status)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { s.progress }, color = Cf.Gold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { s.cancel() }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
        }
        val err = s.error
        if (err != null) {
            Spacer(Modifier.height(10.dp))
            Text(err, color = MaterialTheme.colorScheme.error)
        }
        if (s.done && s.found.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text("Não encontrei correspondências. Tenta um excerto mais claro ou acrescenta mais pistas.", color = Cf.Grey)
        }
    }
    if (s.found.isNotEmpty()) {
        val first = s.found.first()
        SectionHeader("Resultados")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Cf.Card)
                .clickable { onOpen(first) }
                .padding(12.dp)
        ) {
            AsyncImage(
                model = first.posterUrl,
                contentDescription = first.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(100.dp).height(150.dp).clip(RoundedCornerShape(12.dp)).background(Cf.Black)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("MAIS PROVÁVEL", color = Cf.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(first.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(cfMeta(first) + " · " + cfStars(first), color = Cf.Grey, fontSize = 12.sp)
                if (first.overview.isNotBlank()) {
                    Text(
                        first.overview,
                        fontSize = 12.sp,
                        color = Cf.Silver,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldButton("Ver detalhes", { onOpen(first) }, Modifier.weight(1f))
                    val isFav = favs.any { it.id == first.id && it.isTv == first.isTv }
                    IconButton(onClick = { onFav(first) }) {
                        Icon(
                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFav) Cf.Gold else Cf.Silver
                        )
                    }
                }
            }
        }
        if (s.found.size > 1) {
            SectionHeader("Outros resultados")
            s.found.drop(1).forEachIndexed { i, m -> MovieListItem(m, onOpen, "#" + (i + 2)) }
        }
        Text(
            "Sugestões geradas por IA. Confirma pelo cartaz e pela sinopse.",
            color = Cf.Grey,
            fontSize = 11.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}
