package com.cinefinder.app.v3

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import com.cinefinder.app.api.*
import com.cinefinder.app.ui.AppSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DEFAULT_VISIBLE = setOf(
    "banners", "emalta", "lancamentos", "filmes", "series", "animes", "novelas", "populares", "minhalista"
)

class Store(context: Context) {
    private val p = context.getSharedPreferences("cinefinder_v3", Context.MODE_PRIVATE)

    fun visible(): Set<String> {
        val s = p.getString("visible", null) ?: return DEFAULT_VISIBLE
        return s.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun setVisible(v: Set<String>) {
        p.edit().putString("visible", v.joinToString(",")).apply()
    }

    fun history(): List<String> =
        (p.getString("search_history", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addHistory(q: String): List<String> {
        val list = history().toMutableList()
        list.remove(q)
        list.add(0, q)
        val out = list.take(15)
        p.edit().putString("search_history", out.joinToString("\n")).apply()
        return out
    }

    fun clearHistory() {
        p.edit().remove("search_history").apply()
    }

    fun requests(): List<String> =
        (p.getString("requests", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addRequest(text: String): List<String> {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val clean = text.replace("\n", " ").replace("|", "/")
        val out = (listOf(date + "|" + clean) + requests()).take(50)
        p.edit().putString("requests", out.joinToString("\n")).apply()
        return out
    }

    fun clearRequests() {
        p.edit().remove("requests").apply()
    }
}

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

@Composable
private fun Title(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp, bottom = 6.dp))
}

@Composable
fun SettingsScreen(
    keys: Keys,
    settings: AppSettings,
    visible: Set<String>,
    onVisible: (Set<String>) -> Unit,
    onLanguage: (String) -> Unit,
    onTextScale: (Float) -> Unit,
    onKeysSaved: (String, String) -> Unit,
    onClearHistory: () -> Unit,
    onClearCache: () -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableStateOf("Início") }
    var g by remember { mutableStateOf(keys.gemini) }
    var t by remember { mutableStateOf(keys.tmdb) }
    var saved by remember { mutableStateOf(false) }
    var lang by remember { mutableStateOf(settings.language) }
    var scale by remember { mutableFloatStateOf(settings.textScale) }
    var confirm by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    CfScreen {
        CfTopBar("Configurações", onBack)
        ChipRow(listOf("Início", "Experiência", "Sistema"), tab) { tab = it }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (tab == "Início") {
                Text("Organizar início", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Escolhe o que queres ver na página inicial.", color = Cf.Grey, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                val options = listOf("banners" to "Banners") +
                    HOME_SECTIONS.map { it.first to it.second } +
                    listOf("minhalista" to "Minha Lista")
                options.forEach { o ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(o.second, modifier = Modifier.weight(1f))
                        Switch(
                            checked = o.first in visible,
                            onCheckedChange = { on -> onVisible(if (on) visible + o.first else visible - o.first) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onVisible(DEFAULT_VISIBLE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restaurar padrão")
                }
            }
            if (tab == "Experiência") {
                Title("Idioma")
                Text(
                    "Muda o idioma dos títulos, sinopses e trailers. Os menus do app continuam em português.",
                    color = Cf.Grey,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LANGS) { l ->
                        CfChip(l.first, lang == l.second) {
                            lang = l.second
                            settings.language = l.second
                            onLanguage(l.second)
                        }
                    }
                }
                Title("Tamanho do texto")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SIZES) { s ->
                        CfChip(s.first, scale == s.second) {
                            scale = s.second
                            settings.textScale = s.second
                            onTextScale(s.second)
                        }
                    }
                }
                Title("Pesquisa")
                OutlinedButton(
                    onClick = {
                        onClearHistory()
                        note = "Histórico de pesquisa apagado."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apagar histórico de pesquisa")
                }
            }
            if (tab == "Sistema") {
                Title("Chaves de acesso")
                Text("As chaves ficam guardadas só neste telemóvel.", color = Cf.Grey, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(10.dp))
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
                Spacer(Modifier.height(12.dp))
                GoldButton(
                    "Guardar chaves",
                    {
                        keys.gemini = g
                        keys.tmdb = t
                        onKeysSaved(g.trim(), t.trim())
                        saved = true
                    },
                    Modifier.fillMaxWidth()
                )
                if (saved) {
                    Text("Guardado ✓", color = Cf.Gold, modifier = Modifier.padding(top = 8.dp))
                }
                Title("Cache")
                OutlinedButton(
                    onClick = {
                        onClearCache()
                        note = "Cache limpa."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Limpar cache de listas e imagens")
                }
                Title("Dados")
                OutlinedButton(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Limpar dados locais")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        keys.gemini = ""
                        keys.tmdb = ""
                        g = ""
                        t = ""
                        saved = false
                        onKeysSaved("", "")
                        note = "Chaves apagadas."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apagar chaves guardadas")
                }
                Title("Sobre")
                Text("CineFinder 3.0", color = Cf.Grey)
                Text(
                    "Os fotogramas, imagens e descrições que identificas são enviados à Google para análise.",
                    color = Cf.Grey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    "Este produto usa a API do TMDB, mas não é endossado nem certificado pelo TMDB.",
                    color = Cf.Grey,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            val n = note
            if (n != null) {
                Text(n, color = Cf.Gold, modifier = Modifier.padding(top = 14.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Limpar dados locais?") },
            text = { Text("Isto apaga favoritos, listas, histórico e pedidos guardados neste telemóvel. Não pode ser desfeito.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearAll()
                    confirm = false
                    note = "Dados locais apagados."
                }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } }
        )
    }
}
