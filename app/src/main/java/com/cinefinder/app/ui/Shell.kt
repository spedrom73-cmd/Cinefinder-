package com.cinefinder.app.v3

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.cinefinder.app.api.*
import com.cinefinder.app.ui.AppSettings
import com.cinefinder.app.ui.CrashLog
import com.cinefinder.app.ui.Incoming
import kotlinx.coroutines.delay

@Composable
fun SplashV3() {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Cf.Night, Cf.Black))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CfEmblem(170.dp)
            Spacer(Modifier.height(8.dp))
            CfWordmark(40)
            Spacer(Modifier.height(14.dp))
            Text("O que procuras, aqui encontras.", color = Cf.Grey, fontSize = 14.sp)
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(color = Cf.Gold, trackColor = Cf.Card, modifier = Modifier.width(120.dp))
            Spacer(Modifier.height(8.dp))
            Text("A carregar...", color = Cf.Grey, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NavItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Cf.Gold else Cf.Grey)
        Text(label, fontSize = 10.sp, color = if (selected) Cf.Gold else Cf.Grey)
    }
}

@Composable
private fun CenterItem(selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) Cf.Gold else Cf.Card),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Identificar",
                tint = if (selected) Color.Black else Cf.Gold
            )
        }
        Text("Identificar", fontSize = 10.sp, color = if (selected) Cf.Gold else Cf.Grey)
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0B0E16)).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("Início", Icons.Default.Home, tab == 0, Modifier.weight(1f)) { onTab(0) }
        NavItem("Categorias", Icons.Default.Apps, tab == 1, Modifier.weight(1f)) { onTab(1) }
        CenterItem(tab == 2, Modifier.weight(1f)) { onTab(2) }
        NavItem("Explorar", Icons.Default.Explore, tab == 3, Modifier.weight(1f)) { onTab(3) }
        NavItem("Conta", Icons.Default.Person, tab == 4, Modifier.weight(1f)) { onTab(4) }
    }
}

@Composable
fun CineFinderV3() {
    val context = LocalContext.current
    val keys = remember { Keys(context) }
    val favs = remember { Favorites(context) }
    val lib = remember { Library(context) }
    val settings = remember { AppSettings(context) }
    val store = remember { Store(context) }

    var splash by remember { mutableStateOf(true) }
    var tab by remember { mutableIntStateOf(0) }
    var stack by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var overlay by remember { mutableStateOf<String?>(null) }
    var geminiKey by remember { mutableStateOf(keys.gemini) }
    var tmdbKey by remember { mutableStateOf(keys.tmdb) }
    var favList by remember { mutableStateOf(favs.all()) }
    var want by remember { mutableStateOf(lib.get("want")) }
    var seen by remember { mutableStateOf(lib.get("seen")) }
    var history by remember { mutableStateOf(lib.get("history")) }
    var searchHistory by remember { mutableStateOf(store.history()) }
    var requests by remember { mutableStateOf(store.requests()) }
    var visible by remember { mutableStateOf(store.visible()) }
    var lang by remember { mutableStateOf(settings.language) }
    var textScale by remember { mutableFloatStateOf(settings.textScale) }
    var crash by remember { mutableStateOf(CrashLog.read(context)) }
    val incoming = Incoming.uri

    Api.language = lang

    val openMovie: (Movie) -> Unit = { m ->
        Toast.makeText(context, m.title, Toast.LENGTH_SHORT).show()
        stack = stack + m
    }

    LaunchedEffect(Unit) {
        delay(1200)
        splash = false
        if (keys.tmdb.isBlank() && Incoming.uri == null) overlay = "settings"
    }

    LaunchedEffect(incoming, splash) {
        if (incoming != null && !splash) {
            tab = 2
            stack = emptyList()
            overlay = null
        }
    }

    BackHandler(enabled = stack.isNotEmpty() || overlay != null) {
        if (stack.isNotEmpty()) {
            stack = stack.dropLast(1)
        } else {
            overlay = null
        }
    }

    val base = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * textScale)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Cf.Black, contentColor = Cf.Silver) {
            if (splash) {
                SplashV3()
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    key(lang) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Cf.Night, Cf.Black)))
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (tab) {
                                    0 -> HomeScreen(
                                        tmdbKey = tmdbKey,
                                        visible = visible,
                                        myList = want,
                                        onOpen = openMovie,
                                        onSearch = { overlay = "search" },
                                        onIdentify = { tab = 2 },
                                        onSettings = { overlay = "settings" }
                                    )
                                    1 -> CategoriesScreen(tmdbKey, { overlay = "search" }, openMovie)
                                    2 -> IdentifyScreen(
                                        tmdbKey = tmdbKey,
                                        geminiKey = geminiKey,
                                        incoming = incoming,
                                        onConsumed = { Incoming.uri = null },
                                        favs = favList,
                                        onFav = { favList = favs.toggle(it) },
                                        onFound = { history = lib.push("history", it, 40) },
                                        onOpen = openMovie
                                    )
                                    3 -> ExploreScreen(tmdbKey, { overlay = "search" }, openMovie)
                                    else -> AccountScreen(
                                        wantCount = want.size,
                                        historyCount = history.size,
                                        favCount = favList.size,
                                        requestCount = requests.size,
                                        onNav = { overlay = it }
                                    )
                                }
                            }
                            BottomBar(tab) { tab = it }
                        }
                    }

                    when (overlay) {
                        "search" -> SearchScreen(
                            tmdbKey = tmdbKey,
                            history = searchHistory,
                            onAddHistory = { searchHistory = store.addHistory(it) },
                            onClearHistory = {
                                store.clearHistory()
                                searchHistory = emptyList()
                            },
                            onOpen = openMovie,
                            onBack = { overlay = null }
                        )
                        "settings" -> SettingsScreen(
                            keys = keys,
                            settings = settings,
                            visible = visible,
                            onVisible = {
                                visible = it
                                store.setVisible(it)
                            },
                            onLanguage = {
                                lang = it
                                Api.language = it
                                CfCache.clear()
                            },
                            onTextScale = { textScale = it },
                            onKeysSaved = { g, t ->
                                geminiKey = g
                                tmdbKey = t
                            },
                            onClearHistory = {
                                store.clearHistory()
                                searchHistory = emptyList()
                            },
                            onClearCache = {
                                CfCache.clear()
                                coil.Coil.imageLoader(context).memoryCache?.clear()
                            },
                            onClearAll = {
                                favList.forEach { favs.toggle(it) }
                                favList = favs.all()
                                lib.clear("want")
                                lib.clear("seen")
                                lib.clear("history")
                                want = emptyList()
                                seen = emptyList()
                                history = emptyList()
                                store.clearHistory()
                                store.clearRequests()
                                searchHistory = emptyList()
                                requests = emptyList()
                            },
                            onBack = { overlay = null }
                        )
                        "want" -> MovieListScreen("Minha Lista", want, { overlay = null }, openMovie) {
                            lib.clear("want")
                            want = emptyList()
                        }
                        "seen" -> MovieListScreen("Já vistos", seen, { overlay = null }, openMovie) {
                            lib.clear("seen")
                            seen = emptyList()
                        }
                        "history" -> MovieListScreen("Histórico de identificações", history, { overlay = null }, openMovie) {
                            lib.clear("history")
                            history = emptyList()
                        }
                        "favs" -> MovieListScreen("Favoritos", favList, { overlay = null }, openMovie) {
                            favList.forEach { favs.toggle(it) }
                            favList = favs.all()
                        }
                        "requests" -> RequestsScreen(
                            requests = requests,
                            onAdd = { requests = store.addRequest(it) },
                            onClear = {
                                store.clearRequests()
                                requests = emptyList()
                            },
                            onBack = { overlay = null }
                        )
                        else -> {}
                    }

                    val top = stack.lastOrNull()
                    if (top != null) {
                        key(top.id, top.isTv) {
                            DetailScreen(
                                movie = top,
                                tmdbKey = tmdbKey,
                                isFav = favList.any { it.id == top.id && it.isTv == top.isTv },
                                isWant = want.any { it.id == top.id && it.isTv == top.isTv },
                                isSeen = seen.any { it.id == top.id && it.isTv == top.isTv },
                                onFav = { favList = favs.toggle(top) },
                                onWant = { want = lib.toggle("want", top) },
                                onSeen = { seen = lib.toggle("seen", top) },
                                onOpen = openMovie,
                                onBack = { stack = stack.dropLast(1) }
                            )
                        }
                    }

                    val c = crash
                    if (c != null) {
                        AlertDialog(
                            onDismissRequest = { crash = null },
                            confirmButton = { TextButton(onClick = { crash = null }) { Text("Fechar") } },
                            title = { Text("O app fechou antes") },
                            text = {
                                Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                                    Text(c.take(1800), fontSize = 10.sp)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
