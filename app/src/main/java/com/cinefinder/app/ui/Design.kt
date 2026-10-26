package com.cinefinder.app.v3

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.cinefinder.app.R
import com.cinefinder.app.api.*
import kotlinx.coroutines.CancellationException
import java.util.Locale

object Cf {
    val Black = Color(0xFF080A10)
    val Night = Color(0xFF07152E)
    val Blue = Color(0xFF087CFF)
    val Silver = Color(0xFFF3F5FA)
    val Gold = Color(0xFFFFD21F)
    val Orange = Color(0xFFFF8A00)
    val Grey = Color(0xFFA9AFBC)
    val Card = Color(0xFF151821)
}

@Composable
fun CfTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cf.Gold,
            onPrimary = Color.Black,
            secondary = Cf.Blue,
            onSecondary = Color.White,
            background = Cf.Black,
            onBackground = Cf.Silver,
            surface = Cf.Card,
            onSurface = Cf.Silver,
            surfaceVariant = Cf.Card,
            onSurfaceVariant = Cf.Grey,
            outline = Color(0xFF2A3042),
            error = Color(0xFFFF6B6B)
        ),
        content = content
    )
}

fun cfOpenLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
    }
}

fun cfMeta(m: Movie): String {
    val kind = if (m.isTv) "Série" else "Filme"
    val y = if (m.year.isBlank()) "" else " · " + m.year
    return kind + y
}

fun cfStars(m: Movie): String = "★ " + String.format(Locale.US, "%.1f", m.rating)

fun cfShare(context: Context, m: Movie) {
    val send = Intent(Intent.ACTION_SEND)
    send.type = "text/plain"
    send.putExtra(Intent.EXTRA_TEXT, "Descobri \"" + m.title + "\" com o CineFinder! " + cfMeta(m) + " · " + cfStars(m))
    try {
        context.startActivity(Intent.createChooser(send, "Partilhar"))
    } catch (e: Exception) {
    }
}

@Composable
fun CfEmblem(size: Dp) {
    Image(
        painter = painterResource(R.drawable.cf_logo),
        contentDescription = "CineFinder",
        modifier = Modifier.size(size)
    )
}

@Composable
fun CfWordmark(fontSize: Int) {
    Row {
        Text("Cine", color = Cf.Silver, fontWeight = FontWeight.ExtraBold, fontSize = fontSize.sp)
        Text("Finder", color = Cf.Gold, fontWeight = FontWeight.ExtraBold, fontSize = fontSize.sp)
    }
}

@Composable
fun CfLogoRow(emblem: Dp, fontSize: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CfEmblem(emblem)
        Spacer(Modifier.width(6.dp))
        CfWordmark(fontSize)
    }
}

@Composable
fun CfScreen(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Cf.Black, contentColor = Cf.Silver) {
        Column(
            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Cf.Night, Cf.Black))),
            content = content
        )
    }
}

@Composable
fun CfTopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Cf.Silver)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, color = Cf.Grey, fontSize = 13.sp, modifier = Modifier.clickable { onAction() })
        }
    }
}

@Composable
fun PosterCard(m: Movie, onOpen: (Movie) -> Unit, modifier: Modifier = Modifier.width(112.dp)) {
    Column(modifier = modifier.clickable { onOpen(m) }) {
        AsyncImage(
            model = m.posterUrl,
            contentDescription = m.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.667f).clip(RoundedCornerShape(14.dp)).background(Cf.Card)
        )
        Text(
            m.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(if (m.isTv) "Série" else "Filme", color = Cf.Grey, fontSize = 11.sp)
    }
}

@Composable
fun PosterRow(title: String, movies: List<Movie>, onOpen: (Movie) -> Unit) {
    if (movies.isEmpty()) return
    Column {
        SectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { m -> PosterCard(m, onOpen) }
        }
    }
}

@Composable
fun CfChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Cf.Blue else Cf.Card)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else Cf.Grey
        )
    }
}

@Composable
fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { o -> CfChip(o, o == selected) { onSelect(o) } }
    }
}

@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Cf.Gold, contentColor = Color.Black),
        modifier = modifier.height(50.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BlueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Cf.Blue, contentColor = Color.White),
        modifier = modifier.height(50.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MovieListItem(m: Movie, onOpen: (Movie) -> Unit, badge: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(m) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = m.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(62.dp).height(92.dp).clip(RoundedCornerShape(10.dp)).background(Cf.Card)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(m.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(cfMeta(m), color = Cf.Grey, fontSize = 12.sp)
            if (m.overview.isNotBlank()) {
                Text(
                    m.overview,
                    color = Cf.Grey,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(cfStars(m), color = Cf.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (badge != null) {
                Text(badge, color = Cf.Blue, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun CfEmpty(text: String) {
    Text(text, color = Cf.Grey, modifier = Modifier.padding(16.dp))
}

@Composable
fun CfLoading() {
    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Cf.Gold)
    }
}

object CfCache {
    private val map = HashMap<String, List<Movie>>()
    fun get(key: String): List<Movie>? = map[key]
    fun put(key: String, value: List<Movie>) {
        map[key] = value
    }
    fun clear() {
        map.clear()
    }
}

class RemoteList(val items: List<Movie>, val loading: Boolean, val failed: Boolean, val retry: () -> Unit)

@Composable
fun rememberRemoteList(tmdbKey: String, path: String): RemoteList {
    val cacheKey = Api.language + "|" + path
    var items by remember(cacheKey) { mutableStateOf(CfCache.get(cacheKey) ?: emptyList()) }
    var loading by remember(cacheKey) { mutableStateOf(false) }
    var failed by remember(cacheKey) { mutableStateOf(false) }
    var attempt by remember(cacheKey) { mutableIntStateOf(0) }
    LaunchedEffect(cacheKey, tmdbKey, attempt) {
        if (items.isEmpty() && tmdbKey.isNotBlank()) {
            loading = true
            failed = false
            try {
                val r = Api.list(tmdbKey, path)
                CfCache.put(cacheKey, r)
                items = r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed = true
            }
            loading = false
        }
    }
    return RemoteList(items, loading, failed) { attempt++ }
}

@Composable
fun RemoteRow(title: String, tmdbKey: String, path: String, onOpen: (Movie) -> Unit) {
    val r = rememberRemoteList(tmdbKey, path)
    Column {
        SectionHeader(title)
        if (r.items.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(r.items) { m -> PosterCard(m, onOpen) }
            }
        } else if (r.loading) {
            Box(modifier = Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cf.Gold, modifier = Modifier.size(28.dp))
            }
        } else if (r.failed) {
            Text(
                "Não consegui carregar. Toca para tentar outra vez.",
                color = Cf.Grey,
                modifier = Modifier.padding(16.dp).clickable { r.retry() }
            )
        }
    }
}
