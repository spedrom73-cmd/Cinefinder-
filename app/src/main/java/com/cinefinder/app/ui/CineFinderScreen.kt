package com.cinefinder.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinefinder.app.api.Api
import com.cinefinder.app.api.Extras
import com.cinefinder.app.api.Favorites
import com.cinefinder.app.api.Keys
import com.cinefinder.app.api.Movie
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val Grey = Color(0xFFAAAAAA)
private val Dark = Color(0xFF141414)
private val Wine = Color(0xFF8E1F26)
private const val ANIME_TV = "discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc"
private const val ANIME_MOVIE = "discover/movie?with_genres=16&with_original_language=ja&sort_by=popularity.desc"

private val GENRES = listOf(
    "Animes" to ANIME_TV,
    "Filmes de anime" to ANIME_MOVIE,
    "Séries" to "discover/tv?sort_by=popularity.desc",
    "Ação" to "discover/movie?with_genres=28&sort_by=popularity.desc",
    "Comédia" to "discover/movie?with_genres=35&sort_by=popularity.desc",
    "Terror" to "discover/movie?with_genres=27&sort_by=popularity.desc",
    "Drama" to "discover/movie?with_genres=18&sort_by=popularity.desc",
    "Ficção científica" to "discover/movie?with_genres=878&sort_by=popularity.desc",
    "Romance" to "discover/movie?with_genres=10749&sort_by=popularity.desc"
)

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

suspend fun loadList(key: String, path: String): List<Movie> =
    try {
        Api.list(key, path)
    } catch (e: Exception) {
        emptyList()
    }

@Composable
fun Logo(dp: Dp, color: Color = Color.White) {
    Canvas(modifier = Modifier.size(dp)) {
        val w = this.size.width
        val stroke = w * 0.09f
        drawCircle(
            color = color,
            radius = w * 0.30f,
            center = Offset(w * 0.45f, w * 0.45f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.67f, w * 0.67f),
            end = Offset(w * 0.92f, w * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val p = Path()
        p.moveTo(w * 0.38f, w * 0.30f)
        p.lineTo(w * 0.38f, w * 0.60f)
        p.lineTo(w * 0.62f, w * 0.45f)
        p.close()
        drawPath(p, color)
    }
}

@Composable
fun Splash() {
    Box(
        modifier = Modifier.fillMaxSize().background(Wine),
        contentAlignment = Alignment.Center
    ) {
        Logo(110.dp)
        CircularProgressIndicator(
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).size(36.dp)
        )
    }
}

@Composable
fun TopBar(onProfile: () -> Unit, onSearch: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).background(Wine).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onProfile) {
            Icon(Icons.Default.Person, contentDescription = "Perfil", tint = Color.White)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Logo(34.dp)
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Color.White)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Definições", tint = Color.White)
        }
    }
}

@Composable
fun CineFinderScreen() {
    val context = LocalContext.current
    val keys = remember { Keys(context) }
    val favs = remember { Favorites(context) }
    val settings = remember { AppSettings(context) }
    var splash by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Movie?>(null) }
    var profile by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var geminiKey by remember { mutableStateOf(keys.gemini) }
    var tmdbKey by remember { mutableStateOf(keys.tmdb) }
    var favList by remember { mutableStateOf(favs.all()) }
    var lang by remember { mutableStateOf(settings.language) }
    var textScale by remember { mutableFloatStateOf(settings.textScale) }

    Api.language = lang

    LaunchedEffect(Unit) {
        delay(1800)
        splash = false
        if (keys.tmdb.isBlank()) showSettings = true
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
                                    icon = { Icon(Icons.Default.Favorite, null) },
                                    label = { Text("Favoritos") }
                                )
                            }
                        }
                    ) { padding ->
                        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                            when (tab) {
                                0 -> HomeTab(tmdbKey, { selected = it }, { showSettings = true })
                                1 -> ExploreTab(tmdbKey) { selected = it }
                                2 -> IdentifyTab(geminiKey, tmdbKey) { selected = it }
                                else -> FavoritesTab(favList) { selected = it }
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
                    val isFav = favList.any { it.id == m.id && it.isTv == m.isTv }
                    DetailScreen(
                        movie = m,
                        tmdbKey = tmdbKey,
                        isFav = isFav,
                        onFav = { favList = favs.toggle(m) },
                        onBack = { selected = null }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTab(tmdbKey: String, onOpen: (Movie) -> Unit, onSettings: () -> Unit) {
    var trending by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var series by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var anime by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var animeMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var top by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(tmdbKey) {
        loaded = false
        if (tmdbKey.isNotBlank()) {
            trending = loadList(tmdbKey, "trending/movie/week")
            series = loadList(tmdbKey, "trending/tv/week")
            anime = loadList(tmdbKey, ANIME_TV)
            animeMovies = loadList(tmdbKey, ANIME_MOVIE)
            top = loadList(tmdbKey, "movie/top_rated")
        }
        loaded = true
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (tmdbKey.isBlank()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Para ver filmes, escreve primeiro a chave do TMDB nas Definições.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onSettings) { Text("Abrir Definições") }
            }
        } else if (!loaded) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (trending.isEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Não consegui carregar os filmes. Confirma a chave do TMDB nas Definições e a ligação à internet.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onSettings) { Text("Abrir Definições") }
            }
        } else {
            Hero(trending.first(), onOpen)
            MovieRow("Filmes em alta", trending.drop(1), onOpen)
            MovieRow("Séries em alta", series, onOpen)
            MovieRow("Animes em alta", anime, onOpen)
            MovieRow("Filmes de anime", animeMovies, onOpen)
            MovieRow("Mais bem avaliados", top, onOpen)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun Hero(movie: Movie, onOpen: (Movie) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(460.dp).clickable { onOpen(movie) }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTab(tmdbKey: String, onOpen: (Movie) -> Unit) {
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(genre) {
        val g = genre
        if (g != null && tmdbKey.isNotBlank()) {
            loading = true
            message = null
            results = loadList(tmdbKey, g)
            if (results.isEmpty()) message = "Sem resultados."
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Pesquisar filmes, séries e animes") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                if (tmdbKey.isNotBlank() && query.isNotBlank()) {
                    genre = null
                    scope.launch {
                        loading = true
                        message = null
                        try {
                            results = Api.search(tmdbKey, query)
                            if (results.isEmpty()) message = "Sem resultados."
                        } catch (e: Exception) {
                            message = "Erro na pesquisa. Confirma a chave do TMDB."
                        }
                        loading = false
                    }
                }
            }),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GENRES) { g ->
                FilterChip(
                    selected = genre == g.second,
                    onClick = {
                        if (genre == g.second) {
                            genre = null
                            results = emptyList()
                        } else {
                            genre = g.second
                        }
                    },
                    label = { Text(g.first) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (tmdbKey.isBlank()) {
            Text("Escreve primeiro a chave do TMDB nas Definições.", color = Grey, modifier = Modifier.padding(16.dp))
        }
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        message?.let { Text(it, color = Grey, modifier = Modifier.padding(16.dp)) }
        results.forEach { ResultCard(it, onOpen) }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FavoritesTab(list: List<Movie>, onOpen: (Movie) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            "Favoritos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
        if (list.isEmpty()) {
            Text(
                "Ainda não tens favoritos. Abre um filme e toca no coração.",
                color = Grey,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        list.forEach { ResultCard(it, onOpen) }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DetailScreen(
    movie: Movie,
    tmdbKey: String,
    isFav: Boolean,
    onFav: () -> Unit,
    onBack: () -> Unit
) {
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
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                IconButton(onClick = onFav, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFav) Color(0xFFE50914) else Color.White
                    )
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
            error = "Vai às Definições (⚙) e escreve as duas chaves."
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
            "Escolhe um vídeo curto de um filme, série ou anime (pode ser uma gravação de ecrã do TikTok). A IA vê alguns fotogramas e tenta descobrir qual é. Os fotogramas são enviados à Google.",
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
fun ProfileScreen(onSettings: () -> Unit, onBack: () -> Unit) {
    var soon by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(Wine).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text("Perfil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(52.dp), tint = Grey)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Faça login", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Acesse o seu perfil e os seus favoritos.", color = Grey)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { soon = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Wine),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Entrar na conta", fontWeight = FontWeight.Bold)
                }
                if (soon) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("As contas chegam numa próxima versão.", color = Grey)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Abrir Definições")
                }
            }
        }
    }
}
