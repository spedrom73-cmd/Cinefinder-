package com.cinefinder.app.v3

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.cinefinder.app.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val CF_ANIME_TV = "discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc"

data class Genre(val name: String, val kind: String, val path: String)

private fun mg(id: Int): String = "discover/movie?with_genres=" + id + "&sort_by=popularity.desc"

val GENRES = listOf(
    Genre("Ação", "movie", mg(28)),
    Genre("Aventura", "movie", mg(12)),
    Genre("Animação", "movie", mg(16)),
    Genre("Anime", "anime", CF_ANIME_TV),
    Genre("Comédia", "movie", mg(35)),
    Genre("Crime", "movie", mg(80)),
    Genre("Documentário", "movie", mg(99)),
    Genre("Drama", "movie", mg(18)),
    Genre("Fantasia", "movie", mg(14)),
    Genre("Ficção científica", "movie", mg(878)),
    Genre("Romance", "movie", mg(10749)),
    Genre("Suspense", "movie", mg(53)),
    Genre("Terror", "movie", mg(27)),
    Genre("Mistério", "movie", mg(9648)),
    Genre("Guerra", "movie", mg(10752)),
    Genre("História", "movie", mg(36)),
    Genre("Família", "movie", mg(10751)),
    Genre("Musical", "movie", mg(10402)),
    Genre("Novelas", "novela", "discover/tv?with_genres=10766&with_original_language=pt&sort_by=popularity.desc"),
    Genre("Doramas", "novela", "discover/tv?with_original_language=ko&with_genres=18&sort_by=popularity.desc"),
    Genre("Séries", "tv", "discover/tv?sort_by=popularity.desc"),
    Genre("Filmes", "movie", "discover/movie?sort_by=popularity.desc")
)

data class Country(val code: String, val name: String, val flag: String)

val COUNTRIES = listOf(
    Country("US", "EUA", "🇺🇸"),
    Country("KR", "Coreia do Sul", "🇰🇷"),
    Country("JP", "Japão", "🇯🇵"),
    Country("BR", "Brasil", "🇧🇷"),
    Country("PT", "Portugal", "🇵🇹"),
    Country("FR", "França", "🇫🇷"),
    Country("GB", "Reino Unido", "🇬🇧"),
    Country("ES", "Espanha", "🇪🇸"),
    Country("IN", "Índia", "🇮🇳")
)

val DECADES = listOf(
    Triple("Anos 70", 1970, 1979),
    Triple("Anos 80", 1980, 1989),
    Triple("Anos 90", 1990, 1999),
    Triple("Anos 2000", 2000, 2009),
    Triple("Anos 2010", 2010, 2019),
    Triple("Anos 2020", 2020, 2029)
)

@Composable
fun MovieGrid(title: String, basePath: String, tmdbKey: String, onBack: () -> Unit, onOpen: (Movie) -> Unit) {
    var items by remember(basePath) { mutableStateOf<List<Movie>>(emptyList()) }
    var requested by remember(basePath) { mutableIntStateOf(1) }
    var loading by remember(basePath) { mutableStateOf(false) }
    var failed by remember(basePath) { mutableStateOf(false) }
    var ended by remember(basePath) { mutableStateOf(false) }
    var retryCount by remember(basePath) { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(basePath, requested, retryCount) {
        loading = true
        failed = false
        try {
            val sep = if (basePath.contains("?")) "&" else "?"
            val r = Api.list(tmdbKey, basePath + sep + "page=" + requested)
            if (r.isEmpty()) ended = true
            val known = items.map { it.id }.toSet()
            items = items + r.filter { it.id !in known }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
        loading = false
    }

    val lastVisible by remember { derivedStateOf { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 } }
    LaunchedEffect(lastVisible, items.size) {
        if (!loading && !ended && !failed && items.isNotEmpty() && lastVisible >= items.size - 6 && requested < 20) {
            requested += 1
        }
    }

    CfScreen {
        CfTopBar(title, onBack)
        if (items.isEmpty() && loading) {
            CfLoading()
        }
        if (failed) {
            Text(
                "Não consegui carregar. Toca para tentar outra vez.",
                color = Cf.Grey,
                modifier = Modifier.padding(16.dp).clickable { retryCount += 1 }
            )
        }
        if (items.isEmpty() && ended) {
            CfEmpty("Sem resultados.")
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(items) { m -> PosterCard(m, onOpen, Modifier.fillMaxWidth()) }
            if (loading && items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { CfLoading() }
            }
        }
    }
}

@Composable
fun GenreTile(g: Genre, tmdbKey: String, onClick: () -> Unit) {
    val r = rememberRemoteList(tmdbKey, g.path)
    val img = r.items.firstOrNull()?.let { it.backdropUrl ?: it.posterUrl }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Cf.Card)
            .clickable { onClick() }
    ) {
        if (img != null) {
            AsyncImage(
                model = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x33080A10), Color(0xDD080A10)))
            )
        )
        Text(
            g.name,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}

@Composable
fun CategoriesScreen(tmdbKey: String, onSearch: () -> Unit, onOpen: (Movie) -> Unit) {
    var filter by remember { mutableStateOf("Todos") }
    var open by remember { mutableStateOf<Genre?>(null) }
    val g = open
    if (g != null) {
        BackHandler { open = null }
        MovieGrid(g.name, g.path, tmdbKey, { open = null }, onOpen)
        return
    }
    val shown = GENRES.filter {
        when (filter) {
            "Filmes" -> it.kind == "movie"
            "Séries" -> it.kind == "tv" || it.kind == "novela"
            "Animes" -> it.kind == "anime"
            "Novelas" -> it.kind == "novela"
            else -> true
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        CfTopBar("Categorias", null) {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Cf.Silver)
            }
        }
        ChipRow(listOf("Todos", "Filmes", "Séries", "Animes", "Novelas"), filter) { filter = it }
        Spacer(Modifier.height(12.dp))
        if (tmdbKey.isBlank()) {
            CfEmpty("Escreve primeiro a chave do TMDB nas Definições.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(shown) { gen -> GenreTile(gen, tmdbKey) { open = gen } }
            }
        }
    }
}

@Composable
fun ExploreScreen(tmdbKey: String, onSearch: () -> Unit, onOpen: (Movie) -> Unit) {
    var open by remember { mutableStateOf<Pair<String, String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val o = open
    if (o != null) {
        BackHandler { open = null }
        MovieGrid(o.first, o.second, tmdbKey, { open = null }, onOpen)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            CfTopBar("Explorar", null) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Cf.Silver)
                }
            }
        }
        if (tmdbKey.isBlank()) {
            item { CfEmpty("Escreve primeiro a chave do TMDB nas Definições.") }
        } else {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(Cf.Blue, Cf.Orange)))
                        .clickable { open = "Tendências da semana" to "trending/movie/week" }
                        .padding(18.dp)
                ) {
                    Text("Tendências da Semana", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Os conteúdos mais falados agora.",
                        color = Cf.Silver,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                try {
                                    val m = Api2.random(tmdbKey)
                                    if (m != null) onOpen(m)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                }
                                busy = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(if (busy) "A escolher..." else "🎲  Surpreende-me com um filme")
                }
            }
            item { RemoteRow("Em alta agora", tmdbKey, "trending/movie/week", onOpen) }
            item { RemoteRow("Mais populares", tmdbKey, "movie/popular", onOpen) }
            item { RemoteRow("Lançamentos", tmdbKey, "movie/now_playing", onOpen) }
            item {
                Column {
                    SectionHeader("Por país")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(COUNTRIES) { c ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    open = ("Filmes: " + c.name) to
                                        ("discover/movie?with_origin_country=" + c.code + "&sort_by=popularity.desc")
                                }
                            ) {
                                Box(
                                    modifier = Modifier.size(58.dp).clip(CircleShape).background(Cf.Card),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(c.flag, fontSize = 26.sp)
                                }
                                Text(c.name, fontSize = 11.sp, color = Cf.Grey, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    SectionHeader("Por década")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DECADES) { d ->
                            CfChip(d.first, false) {
                                open = ("Filmes: " + d.first) to
                                    ("discover/movie?primary_release_date.gte=" + d.second + "-01-01&primary_release_date.lte=" +
                                        d.third + "-12-31&sort_by=popularity.desc&vote_count.gte=50")
                            }
                        }
                    }
                }
            }
            item {
                Column {
                    SectionHeader("Géneros populares")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(GENRES.take(12)) { gen ->
                            CfChip(gen.name, false) { open = gen.name to gen.path }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SearchScreen(
    tmdbKey: String,
    history: List<String>,
    onAddHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpen: (Movie) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf("Tudo") }
    var sort by remember { mutableStateOf("Relevância") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focus.requestFocus()
        } catch (e: Exception) {
        }
    }

    LaunchedEffect(query, attempt) {
        val q = query.trim()
        if (q.length < 2 || tmdbKey.isBlank()) {
            results = emptyList()
            error = null
            loading = false
            return@LaunchedEffect
        }
        delay(450)
        loading = true
        error = null
        try {
            results = Api.search(tmdbKey, q)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "Sem ligação ou chave inválida. Toca para tentar outra vez."
        }
        loading = false
    }

    val filtered = results.filter {
        when (filter) {
            "Filmes" -> !it.isTv
            "Séries" -> it.isTv
            else -> true
        }
    }
    val shown = when (sort) {
        "Nota" -> filtered.sortedByDescending { it.rating }
        "Ano" -> filtered.sortedByDescending { it.year }
        else -> filtered
    }

    CfScreen {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Cf.Silver)
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Pesquisa filmes, séries, animes...") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Cf.Grey)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) onAddHistory(query.trim())
                }),
                modifier = Modifier.weight(1f).focusRequester(focus)
            )
        }
        if (query.isBlank()) {
            if (history.isNotEmpty()) {
                SectionHeader("Pesquisas recentes", "Limpar", onClearHistory)
                history.forEach { h ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { query = h }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Cf.Grey)
                        Spacer(Modifier.width(12.dp))
                        Text(h)
                    }
                }
            } else {
                CfEmpty("Escreve o nome de um filme, série ou anime.")
            }
        } else {
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("Tudo", "Filmes", "Séries"), filter) { filter = it }
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf("Relevância", "Nota", "Ano"), sort) { sort = it }
            val err = error
            if (loading) {
                CfLoading()
            } else if (err != null) {
                Text(err, color = Cf.Grey, modifier = Modifier.padding(16.dp).clickable { attempt += 1 })
            } else if (shown.isEmpty() && query.trim().length >= 2) {
                CfEmpty("Sem resultados. Tenta outro nome ou usa Identificar.")
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(shown) { m ->
                    MovieListItem(m, { onAddHistory(query.trim()); onOpen(it) })
                }
            }
        }
    }
}
