package com.cinefinder.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinefinder.app.api.Keys

private val SGrey = Color(0xFFAAAAAA)
private val SWine = Color(0xFF8E1F26)

private val LANGS = listOf(
    "Português (PT)" to "pt-PT",
    "Português (BR)" to "pt-BR",
    "English" to "en-US",
    "Español" to "es-ES"
)

private val SIZES = listOf(
    "Pequeno" to 0.85f,
    "Normal" to 1.0f,
    "Grande" to 1.2f,
    "Muito grande" to 1.4f
)

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("cinefinder_settings", Context.MODE_PRIVATE)
    var language: String
        get() = prefs.getString("language", "pt-PT") ?: "pt-PT"
        set(v) { prefs.edit().putString("language", v).apply() }
    var textScale: Float
        get() = prefs.getFloat("textScale", 1.0f)
        set(v) { prefs.edit().putFloat("textScale", v).apply() }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(22.dp))
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    keys: Keys,
    settings: AppSettings,
    favCount: Int,
    onKeysSaved: (String, String) -> Unit,
    onLanguage: (String) -> Unit,
    onTextScale: (Float) -> Unit,
    onClearFavs: () -> Unit,
    onBack: () -> Unit
) {
    var g by remember { mutableStateOf(keys.gemini) }
    var t by remember { mutableStateOf(keys.tmdb) }
    var saved by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(settings.language) }
    var scale by remember { mutableFloatStateOf(settings.textScale) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(SWine).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text("Definições", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionTitle("Idioma")
                Text("Muda o idioma dos títulos, sinopses e trailers dos filmes.", color = SGrey, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LANGS) { l ->
                        FilterChip(
                            selected = lang == l.second,
                            onClick = {
                                lang = l.second
                                settings.language = l.second
                                onLanguage(l.second)
                            },
                            label = { Text(l.first) }
                        )
                    }
                }

                SectionTitle("Tamanho do texto")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SIZES) { s ->
                        FilterChip(
                            selected = scale == s.second,
                            onClick = {
                                scale = s.second
                                settings.textScale = s.second
                                onTextScale(s.second)
                            },
                            label = { Text(s.first) }
                        )
                    }
                }

                SectionTitle("Chaves de acesso")
                Text("As chaves ficam guardadas só neste telemóvel.", color = SGrey, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(10.dp))
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
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        keys.gemini = g
                        keys.tmdb = t
                        onKeysSaved(g.trim(), t.trim())
                        saved = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Guardar chaves")
                }
                if (saved) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Guardado ✓")
                }

                SectionTitle("Dados")
                OutlinedButton(
                    onClick = { onClearFavs() },
                    enabled = favCount > 0,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apagar favoritos (" + favCount + ")")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        keys.gemini = ""
                        keys.tmdb = ""
                        g = ""
                        t = ""
                        saved = false
                        onKeysSaved("", "")
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apagar chaves guardadas")
                }

                SectionTitle("Sobre")
                Text("CineFinder versão 2.0", color = SGrey)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Os fotogramas dos vídeos que identificas são enviados à Google para análise.",
                    color = SGrey,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB.",
                    color = SGrey,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
