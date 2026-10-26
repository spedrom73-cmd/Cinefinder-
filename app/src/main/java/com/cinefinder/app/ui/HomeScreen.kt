package com.cinefinder.app.v3

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.cinefinder.app.api.*
import kotlinx.coroutines.delay

val HOME_SECTIONS = listOf(
    Triple("emalta", "Em Alta", "trending/movie/week"),
    Triple("lancamentos", "Últimos Lançamentos", "movie/now_playing"),
    Triple("filmes", "Filmes", "movie/popular"),
    Triple("series", "Séries", "tv/popular"),
    Triple("animes", "Animes", "discover/tv?with_genres=16&with_original_language=ja&sort_by=popularity.desc"),
    Triple("novelas", "Novelas", "discover/tv?with_genres=10766&with_original_language=pt&sort_by=popularity.desc"),
    Triple("populares", "Mais Bem Avaliados", "movie/top_rated")
)

@Composable
fun HomeScreen(
    tmdbKey: String,
    visible: Set<String>,
    myList: List<Movie>,
    onOpen: (Movie) -> Unit,
    onSearch: () -> Unit,
    onIdentify: () -> Unit,
    onSettings: () -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { CfLogoRow(34.dp, 24) }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Default.Search, contentDescription = "Pesquisar", tint = Cf.Silver)
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Cf.Card)
                    .clickable { onSearch() }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Cf.Grey)
                Spacer(Modifier.width(10.dp))
                Text("Pesquisa filmes, séries, animes...", color = Cf.Grey)
            }
        }
        item { IdentifyPromo(onIdentify) }
        if (tmdbKey.isBlank()) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Para ver filmes e séries, escreve primeiro a chave do TMDB nas Definições.")
                    Spacer(Modifier.height(12.dp))
                    GoldButton("Abrir Definições", onSettings)
                }
            }
        } else {
            if ("banners" in visible) {
                item { TrendingBanner(tmdbKey, onOpen) }
            }
            HOME_SECTIONS.forEach { s ->
                if (s.first in visible) {
                    item(key = s.first) { RemoteRow(s.second, tmdbKey, s.third, onOpen) }
                }
            }
            if ("minhalista" in visible && myList.isNotEmpty()) {
                item { PosterRow("Minha Lista", myList, onOpen) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun IdentifyPromo(onIdentify: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Cf.Blue, Cf.Night)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Não sabes o nome do filme?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Identifica por vídeo, imagem ou descrição.",
                color = Cf.Silver,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        GoldButton("Identificar", onIdentify)
    }
}

@Composable
private fun TrendingBanner(tmdbKey: String, onOpen: (Movie) -> Unit) {
    val r = rememberRemoteList(tmdbKey, "trending/movie/week")
    val list = r.items.filter { it.backdropUrl != null || it.posterUrl != null }.take(6)
    if (list.isNotEmpty()) {
        BannerPager(list, onOpen)
    } else if (r.loading) {
        CfLoading()
    } else if (r.failed) {
        Text(
            "Não consegui carregar os destaques. Toca para tentar outra vez.",
            color = Cf.Grey,
            modifier = Modifier.padding(16.dp).clickable { r.retry() }
        )
    }
}

@Composable
private fun BannerPager(movies: List<Movie>, onOpen: (Movie) -> Unit) {
    val pager = rememberPagerState(pageCount = { movies.size })
    LaunchedEffect(movies) {
        while (true) {
            delay(5000)
            pager.animateScrollToPage((pager.currentPage + 1) % movies.size)
        }
    }
    Column {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { i -> BannerCard(movies[i], onOpen) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(movies.size) { i ->
                val sel = i == pager.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (sel) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (sel) Cf.Gold else Cf.Grey.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun BannerCard(m: Movie, onOpen: (Movie) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen(m) }
    ) {
        AsyncImage(
            model = m.backdropUrl ?: m.posterUrl,
            contentDescription = m.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(Cf.Card)
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE080A10)))
            )
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                m.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                cfMeta(m) + " · " + cfStars(m),
                color = Cf.Silver,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(10.dp))
            GoldButton("▶  Ver detalhes", { onOpen(m) })
        }
    }
}
