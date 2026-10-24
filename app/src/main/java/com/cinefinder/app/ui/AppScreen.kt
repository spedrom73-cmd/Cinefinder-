Package com.cinefinder.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinefinder.app.api.Api
import com.cinefinder.app.api.Api2
import com.cinefinder.app.api.Extras
import com.cinefinder.app.api.Favorites
import com.cinefinder.app.api.Keys
import com.cinefinder.app.api.Library
import com.cinefinder.app.api.Movie
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AGrey = Color(0xFFAAAAAA)
private val ADark = Color(0xFF141414)

object Incoming {
    var uri: Uri? by mutableStateOf(null)
}

private fun shareMovie(context: Context, m: Movie) {
    val send = Intent(Intent.ACTION_SEND)
    send.type = "text/plain"
    send.putExtra(Intent.EXTRA_TEXT, "Descobri \"" + m.title + "\" com o CineFinder! " + subtitle(m))
    try {
        context.startActivity(Intent.createChooser(send, "Partilhar"))
    } catch (e: Exception) {
    }
}

private suspend fun resolveTitles(
    tmdbKey: String,
    titles: List<String>,
    onStep: (Float, String) -> Unit
): List<Movie> {
    val list = mutableListOf<Movie>()
    for ((i, t) in titles.withIndex()) {
        onStep(0.8f + 0.2f * (i + 1) / maxOf(titles.size, 1), "A procurar \"" + t + "\"...")
        val r = Api.search(tmdbKey, t).firstOrNull()
        if (r != null && list.none { it.id == r.id }) list.add(r)
    }
    return list
}

@Composable
fun CineFinderApp() {
    val context = LocalContext.current
    val keys = remember { Keys(context) }
    val favs = remember { Favorites(context) }
    val lib = remember { Library(context) }
    val settings = remember { AppSettings(context) }
    var splash by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Movie?>(null) }
    var profile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var geminiKey by remember { mutableStateOf(keys.gemini) }
    var tmdbKey by remember { mutableStateOf(keys.tmdb) }
    var favList by remember { mutableStateOf(favs.all()) }
    var wantList by remember { mutableStateOf(lib.get("want")) }
    var seenList by remember { mutableStateOf(lib.get("seen")) }
    var history by remember { mutableStateOf(lib.get("history")) }
    var lang by remember { mutableStateOf(settings.language) }
    var textScale by remember { mutableFloatStateOf(settings.textScale) }
    val incoming = Incoming.uri

    Api.language = lang

    LaunchedEffect(Unit) {
        delay(1800)
        splash = false
        if (keys.tmdb.isBlank() && Incoming.uri == null) showSettings = true
    }

    LaunchedEffect(incoming, splash) {
        if (incoming != null && !splash) {
            tab = 2
            selected = null
            profile = false
            showSettings = false
        }
    }

    BackHandler(enabled = selected != null || profile || showSettings) {
        if (selected != null) {
            selected = null
        } else if (showSettings) {
            showSettings = false
        } else {
            profile = false
        }
    }

    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * textScale)) {
        if (splash) {
            Splash()
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                key(lang) {
                    Scaffold(
                        topBar = { TopBar({ profile = true }, { tab = 1 }, { showSettings = true }) },
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
                                    icon = { Icon(Icons.Default.Explore, null) },
                                    label = { Text("Explorar") }
                                )
                                NavigationBarItem(
                                    selected = tab == 2,
                                    onClick = { tab = 2 },
                                    icon = { Icon(Icons.Default.VideoFile, null) },
                                    label = { Text("Identificar") }
                                )
                                NavigationBarItem(
                                    selected = tab == 3,
                                    onClick = { tab = 3 },
                                    icon = { Icon(Icons.Default.Bookmark, null) },
                                    label = { Text("Biblioteca") }
                                )
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            when (tab) {
                                0 -> HomePlus(tmdbKey, { selected = it }, { showSettings = true })
                                1 -> ExploreTab(tmdbKey) { selected = it }
                                2 -> IdentifyScreen(
                                    geminiKey = geminiKey,
                                    tmdbKey = tmdbKey,
                                    incoming = incoming,
                                    onConsumed = { Incoming.uri = null },
                                    onFound = { history = lib.push("history", it, 40) },
                                    onOpen = { selected = it }
                                )
                                else -> LibraryTab(
                                    favs = favList,
                                    want = wantList,
                                    seen = seenList,
                                    history = history,
                                    onClear = { name ->
                                        when (name) {
                                            "fav" -> {
                                                favList.forEach { favs.toggle(it) }
                                                favList = favs.all()
                                            }
                                            "want" -> {
                                                lib.clear("want")
                                                wantList = emptyList()
                                            }
                                            "seen" -> {
                                                lib.clear("seen")
                                                seenList = emptyList()
                                            }
                                            else -> {
                                                lib.clear("history")
                                                history = emptyList()
                                            }
                                        }
                                    },
                                    onOpen = { selected = it }
                                )
                            }
                        }
                    }
                }
                if (profile) {
                    ProfileScreen(onSettings = { showSettings = true }, onBack = { profile = false })
                }
                if (showSettings) {
                    SettingsScreen(
                        keys = keys,
                        settings = settings,
                        favCount = favList.size,
                        onKeysSaved = { g, t ->
                            geminiKey = g
                            tmdbKey = t
                        },
                        onLanguage = { lang = it },
                        onTextScale = { textScale = it },
                        onClearFavs = {
                            favList.forEach { favs.toggle(it) }
                            favList = favs.all()
                        },
                        onBack = { showSettings = false }
                    )
                }
                selected?.let { m ->
                    MovieDetail(
                        movie = m,
                        tmdbKey = tmdbKey,
                        isFav = favList.any { it.id == m.id && it.isTv == m.isTv },
                        isWant = wantList.any { it.id == m.id && it.isTv == m.isTv },
                        isSeen = seenList.any { it.id == m.id && it.isTv == m.isTv },
                        onFav = { favList = favs.toggle(m) },
                        onWant = { wantList = lib.toggle("want", m) },
                        onSeen = { seenList = lib.toggle("seen", m) },
                        onOpen = { selected = it },
                        onBack = { selected = null }
                    )
                }
            }
        }
    }
}

@Composable
fun HomePlus(tmdbKey: String, onOpen: (Movie) -> Unit, onSettings: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        if (tmdbKey.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    if (!busy) {
                        busy = true
                        scope.launch {
                            try {
                                val m = Api2.random(tmdbKey)
                                if (m != null) onOpen(m)
                            } catch (e: Exception) {
                            }
                            busy = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (busy) "A escolher..." else "🎲  Surpreende-me com um filme")
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            HomeTab(tmdbKey, onOpen, onSettings)
        }
    }
}

@Composable
fun IdentifyScreen(
    geminiKey: String,
    tmdbKey: String,
    incoming: Uri?,
    onConsumed: () -> Unit,
    onFound: (List<Movie>) -> Unit,
    onOpen: (Movie) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var done by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    fun begin(): Boolean {
        if (geminiKey.isBlank() || tmdbKey.isBlank()) {
            error = "Vai às Definições (⚙) e escreve as duas chaves."
            return false
        }
        busy = true
        error = null
        found = emptyList()
        done = false
        progress = 0f
        return true
    }

    fun startVideo(uri: Uri) {
        if (!begin()) return
        scope.launch {
            try {
                val titles = Api.identify(context, uri, geminiKey) { p, s ->
                    progress = p
                    status = s
                }
                val list = resolveTitles(tmdbKey, titles) { p, s ->
                    progress = p
                    status = s
                }
                found = list
                done = true
                if (list.isNotEmpty()) onFound(list)
            } catch (e: Exception) {
                error = "Erro: " + (e.message ?: "falhou")
            }
            busy = false
        }
    }

    fun startDescribe() {
        if (description.isBlank()) return
        if (!begin()) return
        scope.launch {
            try {
                status = "A perguntar à IA..."
                progress = 0.5f
                val titles = Api2.describe(geminiKey, description)
                val list = resolveTitles(tmdbKey, titles) { p, s ->
                    progress = p
                    status = s
                }
                found = list
                done = true
                if (list.isNotEmpty()) onFound(list)
            } catch (e: Exception) {
                error = "Erro: " + (e.message ?: "falhou")
            }
            busy = false
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) startVideo(uri)
    }

    LaunchedEffect(incoming) {
        if (incoming != null) {
            startVideo(incoming)
            onConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            "Descobrir filme",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Escolhe um vídeo curto de um filme, série ou anime, por exemplo um vídeo do TikTok guardado no telemóvel. Também podes partilhar um vídeo da Galeria diretamente para o CineFinder. Os fotogramas são enviados à Google.",
            color = AGrey
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
        Spacer(modifier = Modifier.height(24.dp))
        Text("Ou descreve o que te lembras", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Ex.: um homem acorda sem memória e tem tatuagens pelo corpo") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { startDescribe() },
            enabled = !busy && description.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Procurar pela descrição")
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
            Text("A IA não conseguiu identificar. Tenta um excerto mais claro ou uma descrição mais detalhada.")
        }
        if (found.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Resultados mais prováveis", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            found.forEach { ResultCard(it, onOpen) }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTab(
    favs: List<Movie>,
    want: List<Movie>,
    seen: List<Movie>,
    history: List<Movie>,
    onClear: (String) -> Unit,
    onOpen: (Movie) -> Unit
) {
    var which by remember { mutableStateOf("want") }
    val names = listOf(
        "want" to "Quero ver",
        "seen" to "Já vi",
        "fav" to "Favoritos",
        "history" to "Identificados"
    )
    val list = when (which) {
        "want" -> want
        "seen" -> seen
        "fav" -> favs
        else -> history
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "A minha biblioteca",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(names) { n ->
                FilterChip(
                    selected = which == n.first,
                    onClick = { which = n.first },
                    label = { Text(n.second) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (list.isEmpty()) {
            Text(
                "Esta lista está vazia. Abre um filme e usa os botões para o guardar aqui.",
                color = AGrey,
                modifier = Modifier.padding(16.dp)
            )
        }
        list.forEach { ResultCard(it, onOpen) }
        if (list.isNotEmpty()) {
            TextButton(onClick = { onClear(which) }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Limpar esta lista")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ListButton(label: String, on: Boolean, onClick: () -> Unit, modifier: Modifier) {
    if (on) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("✓ " + label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("+ " + label)
        }
    }
}

@Composable
fun MovieDetail(
    movie: Movie,
    tmdbKey: String,
    isFav: Boolean,
    isWant: Boolean,
    isSeen: Boolean,
    onFav: () -> Unit,
    onWant: () -> Unit,
    onSeen: () -> Unit,
    onOpen: (Movie) -> Unit,
    onBack: () -> Unit
) {
    // Corpo da função (fechado corretamente)
}
