package com.cinefinder.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinefinder.app.api.Api
import com.cinefinder.app.api.Extras
import com.cinefinder.app.api.Keys
import com.cinefinder.app.api.Movie
import kotlinx.coroutines.launch
import java.util.Locale

private val Grey = Color(0xFFAAAAAA)
private val Dark = Color(0xFF141414)

fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
    }
}

fun subtitle(m: Movie): String {
    val y = if (m.year.isBlank()) "" else m.year + " · "
    return y + "★ " + String.format(Locale.US, "%.1f", m.rating)
}

@Composable
fun CineFinderScreen() {
    val context = LocalContext.current
    val keys = remember { Keys(context) }
    var tab by remember { mutableIntStateOf(if (keys.tmdb.isBlank()) 2 else 0) }
    var selected by remember { mutableStateOf<Movie?>(null) }
    var geminiKey by remember { mutableStateOf(keys.gemini) }
    var tmdbKey by remember { mutableStateOf(keys.tmdb) }

    BackHandler(enabled = selected != null) { selected = null }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Início") }
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.VideoFile, null) },
                        label = { Text("Identificar") }
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Definições") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> HomeTab(tmdbKey, { selected = it }, { tab = 2 })
                    1 -> IdentifyTab(geminiKey, tmdbKey) { selected = it }
                    else -> SettingsTab(keys) { g, t ->
                        geminiKey = g
                        tmdbKey = t
                    }
                }
            }
        }
        selected?.let { m ->
            DetailScreen(m, tmdbKey) { selected = null }
        }
    }
}

@Composable
fun HomeTab(tmdbKey: String, onOpen: (Movie) -> Unit, onSettings: () -> Unit) {
    var trending by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var series by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var top by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var results by remember { mutableStateOf<List<Movie>?>(null) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tmdbKey) {
        if (tmdbKey.isNotBlank()) {
            try {
                error = null
                trending = Api.list(tmdbKey, "trending/movie/week")
                series = Api.list(tmdbKey, "trending/tv/week").map { it.copy(isTv = true) }
                top = Api.list(tmdbKey, "movie/top_rated")
            } catch (e: Exception) {
                error = "Não consegui carregar: " + (e.message ?: "erro")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "CineFinder",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) results = null
            },
            placeholder = { Text("Pesquisar filmes e séries") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (tmdbKey.isNotBlank() && query.isNotBlank()) {
                    scope.launch {
                        try {
                            results = Api.search(tmdbKey, query)
                        } catch (e: Exception) {
                            error = "Erro na pesquisa: " + (e.message ?: "")
                        }
                    }
                }
            }),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        if (tmdbKey.isBlank()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Para ver filmes, escreve primeiro a chave do TMDB nas Definições.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onSettings) { Text("Abrir Definições") }
            }
        } else {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            val r = results
            if (r != null) {
                if (r.isEmpty()) Text("Sem resultados.", modifier = Modifier.padding(16.dp))
                r.forEach { ResultCard(it, onOpen) }
            } else {
                if (trending.isNotEmpty()) Hero(trending.first(), onOpen)
                MovieRow("Filmes em alta", trending.drop(1), onOpen)
                MovieRow("Séries em alta", series, onOpen)
                MovieRow("Mais bem avaliados", top, onOpen)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun Hero(movie: Movie, onOpen: (Movie) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(440.dp).clickable { onOpen(movie) }) {
        AsyncImage(
            model = movie.posterUrl ?: movie.backdropUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(colors = listOf(Color.Transparent, Dark), startY = 300f)
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DESTAQUE AGORA", fontSize = 12.sp, letterSpacing = 3.sp, color = Color(0xFFCCCCCC))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                movie.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onOpen(movie) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Ver detalhes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MovieRow(title: String, movies: List<Movie>, onOpen: (Movie) -> Unit) {
    if (movies.isEmpty()) return
    Text(
        title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(movies) { m -> Poster(m, onOpen) }
    }
}

@Composable
fun Poster(m: Movie, onOpen: (Movie) -> Unit) {
    Column(modifier = Modifier.width(120.dp).clickable { onOpen(m) }) {
        AsyncImage(
            model = m.posterUrl,
            contentDescription = m.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2A2A))
        )
        Text(
            m.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun ResultCard(movie: Movie, onOpen: (Movie) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(movie) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(64.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(movie.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle(movie), color = Grey, fontSize = 13.sp)
        }
    }
}

@Composable
fun DetailScreen(movie: Movie, tmdbKey: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var extras by remember { mutableStateOf<Extras?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(movie.id) {
        extras = try {
            Api.extras(tmdbKey, movie)
        } catch (e: Exception) {
            null
        }
        loaded = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                AsyncImage(
                    model = movie.backdropUrl ?: movie.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Dark))
                    )
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(movie.title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(subtitle(movie), color = Grey, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(16.dp))
                if (movie.overview.isNotBlank()) {
                    Text(movie.overview, color = Color(0xFFDDDDDD))
                }
                Spacer(modifier = Modifier.height(20.dp))
                val e = extras
                if (!loaded) {
                    CircularProgressIndicator()
                } else if (e != null) {
                    if (e.trailerUrl != null) {
                        Button(
                            onClick = { openLink(context, e.trailerUrl) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("▶  Ver trailer", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (e.whereLink != null) {
                        OutlinedButton(
                            onClick = { openLink(context, e.whereLink) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("Onde assistir")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (e.providers.isNotEmpty()) {
                        Text("Disponível em: " + e.providers.joinToString(", "), color = Grey)
                    } else {
                        Text("Não encontrei serviços para este título na tua região.", color = Grey)
                    }
                } else {
                    Text("Não consegui carregar o trailer e os serviços.", color = Grey)
                }
            }
        }
    }
}

@Composable
fun IdentifyTab(geminiKey: String, tmdbKey: String, onOpen: (Movie) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var done by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (geminiKey.isBlank() || tmdbKey.isBlank()) {
            error = "Vai a Definições e escreve as duas chaves."
            return@rememberLauncherForActivityResult
        }
        busy = true
        error = null
        found = emptyList()
        done = false
        scope.launch {
            try {
                val titles = Api.identify(context, uri, geminiKey) { p, s ->
                    progress = p
                    status = s
                }
                val list = mutableListOf<Movie>()
                for ((i, t) in titles.withIndex()) {
                    status = "A procurar \"" + t + "\"..."
                    progress = 0.8f + 0.2f * (i + 1) / maxOf(titles.size, 1)
                    val r = Api.search(tmdbKey, t).firstOrNull()
                    if (r != null && list.none { it.id == r.id }) list.add(r)
                }
                found = list
                done = true
            } catch (e: Exception) {
                error = "Erro: " + (e.message ?: "falhou")
            }
            busy = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Identificar vídeo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Escolhe um vídeo curto de um filme ou série. A IA vê alguns fotogramas e tenta descobrir qual é. Os fotogramas são enviados à Google.",
            color = Grey
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { picker.launch("video/*") },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.VideoFile, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Escolher vídeo do telemóvel")
        }
        if (busy) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(status)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (done && found.isEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("A IA não conseguiu identificar este vídeo. Tenta um excerto mais claro.")
        }
        if (found.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Resultados mais prováveis", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            found.forEach { ResultCard(it, onOpen) }
        }
    }
}

@Composable
fun SettingsTab(keys: Keys, onSaved: (String, String) -> Unit) {
    var g by remember { mutableStateOf(keys.gemini) }
    var t by remember { mutableStateOf(keys.tmdb) }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Definições",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("As chaves ficam guardadas só neste telemóvel.", color = Grey)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = g,
            onValueChange = {
                g = it
                saved = false
            },
            label = { Text("Chave da Gemini") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = t,
            onValueChange = {
                t = it
                saved = false
            },
            label = { Text("Chave do TMDB") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                keys.gemini = g
                keys.tmdb = t
                onSaved(g.trim(), t.trim())
                saved = true
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Guardar")
        }
        if (saved) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Guardado ✓")
        }
    }
}
