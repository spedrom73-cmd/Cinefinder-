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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.cinefinder.app.api.*
import kotlinx.coroutines.CancellationException
import java.util.Locale

private fun votesText(n: Int): String =
    if (n >= 1000) String.format(Locale.US, "%.1fk", n / 1000.0) + " avaliações" else n.toString() + " avaliações"

@Composable
fun DetailScreen(
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
    val context = LocalContext.current
    var details by remember(movie.id) { mutableStateOf<Details?>(null) }
    var loading by remember(movie.id) { mutableStateOf(true) }
    var failed by remember(movie.id) { mutableStateOf(false) }
    var attempt by remember(movie.id) { mutableIntStateOf(0) }
    var tab by remember(movie.id) { mutableStateOf("Sobre") }

    LaunchedEffect(movie.id, movie.isTv, attempt) {
        loading = true
        failed = false
        try {
            details = Api3.details(tmdbKey, movie)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
        }
        loading = false
    }

    val d = details
    val trailerUrl = d?.trailerUrl
    val whereLink = d?.whereLink
    val tabs = if (movie.isTv) listOf("Sobre", "Temporadas", "Elenco", "Relacionados") else listOf("Sobre", "Elenco", "Relacionados")

    Surface(modifier = Modifier.fillMaxSize(), color = Cf.Black, contentColor = Cf.Silver) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                AsyncImage(
                    model = movie.backdropUrl ?: movie.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Cf.Card)
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0x99080A10), Color.Transparent, Cf.Black))
                    )
                )
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    IconButton(onClick = { cfShare(context, movie) }) {
                        Icon(Icons.Default.Share, contentDescription = "Partilhar", tint = Color.White)
                    }
                    IconButton(onClick = onFav) {
                        Icon(
                            if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFav) Cf.Gold else Color.White
                        )
                    }
                }
                Text(
                    movie.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                )
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                val extra = if (d != null && d.runtime.isNotBlank()) " · " + d.runtime else ""
                Text(cfMeta(movie) + extra, color = Cf.Grey, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text(
                        "★ " + String.format(Locale.US, "%.1f", movie.rating) + "/10",
                        color = Cf.Gold,
                        fontWeight = FontWeight.Bold
                    )
                    if (d != null && d.voteCount > 0) {
                        Text("  ·  " + votesText(d.voteCount), color = Cf.Grey, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (trailerUrl != null) {
                        GoldButton("▶  Assistir trailer", { cfOpenLink(context, trailerUrl) }, Modifier.weight(1f))
                    } else {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text(if (loading) "A carregar..." else "Sem trailer")
                        }
                    }
                    OutlinedButton(
                        onClick = onWant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(if (isWant) "✓ Minha Lista" else "+ Minha Lista")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (whereLink != null) {
                        OutlinedButton(
                            onClick = { cfOpenLink(context, whereLink) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        ) {
                            Text("Onde assistir")
                        }
                    }
                    OutlinedButton(
                        onClick = onSeen,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(if (isSeen) "✓ Já vi" else "Marcar como visto")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            ChipRow(tabs, tab) { tab = it }
            Spacer(Modifier.height(14.dp))

            if (loading && d == null) {
                CfLoading()
            } else if (failed && d == null) {
                Text(
                    "Não consegui carregar os detalhes. Toca para tentar outra vez.",
                    color = Cf.Grey,
                    modifier = Modifier.padding(16.dp).clickable { attempt += 1 }
                )
                if (tab == "Sobre" && movie.overview.isNotBlank()) {
                    Text(movie.overview, color = Cf.Silver, modifier = Modifier.padding(horizontal = 16.dp))
                }
            } else if (d != null) {
                when (tab) {
                    "Sobre" -> {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            if (d.tagline.isNotBlank()) {
                                Text(d.tagline, color = Cf.Gold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            Text(
                                if (movie.overview.isNotBlank()) movie.overview else "Sem sinopse disponível neste idioma.",
                                color = Cf.Silver
                            )
                            if (d.genres.isNotEmpty()) {
                                Text(
                                    "Géneros: " + d.genres.joinToString(", "),
                                    color = Cf.Grey,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                            if (d.providers.isNotEmpty()) {
                                Text(
                                    "Disponível em: " + d.providers.joinToString(", "),
                                    color = Cf.Grey,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            } else {
                                Text(
                                    "Não encontrei serviços de streaming para este título na tua região.",
                                    color = Cf.Grey,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                    "Temporadas" -> {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            if (d.seasons.isEmpty()) Text("Sem informação de temporadas.", color = Cf.Grey)
                            d.seasons.forEach { s ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Cf.Card)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = s.posterUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.width(54.dp).height(80.dp).clip(RoundedCornerShape(8.dp)).background(Cf.Black)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(s.name, fontWeight = FontWeight.Bold)
                                        val y = if (s.year.isBlank()) "" else " · " + s.year
                                        Text(s.episodes.toString() + " episódios" + y, color = Cf.Grey, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    "Elenco" -> {
                        if (d.cast.isEmpty()) {
                            Text("Sem informação de elenco.", color = Cf.Grey, modifier = Modifier.padding(horizontal = 16.dp))
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(d.cast) { p ->
                                    Column(modifier = Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        AsyncImage(
                                            model = p.photoUrl,
                                            contentDescription = p.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(72.dp).clip(CircleShape).background(Cf.Card)
                                        )
                                        Text(
                                            p.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            textAlign = TextAlign.Center,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                        Text(
                                            p.role,
                                            fontSize = 10.sp,
                                            color = Cf.Grey,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (d.related.isEmpty()) {
                            Text("Sem títulos relacionados.", color = Cf.Grey, modifier = Modifier.padding(horizontal = 16.dp))
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().height(230.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(d.related) { m -> PosterCard(m, onOpen) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
