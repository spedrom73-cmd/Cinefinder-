package com.cinefinder.app.api

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Movie(
    val id: Int,
    val title: String,
    val year: String,
    val rating: Double,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val isTv: Boolean
)

data class Extras(
    val trailerUrl: String?,
    val whereLink: String?,
    val providers: List<String>
)

class ApiException(val code: Int, message: String) : Exception(message)

class Keys(context: Context) {
    private val prefs = context.getSharedPreferences("cinefinder_keys", Context.MODE_PRIVATE)
    var gemini: String
        get() = prefs.getString("gemini", "") ?: ""
        set(v) { prefs.edit().putString("gemini", v.trim()).apply() }
    var tmdb: String
        get() = prefs.getString("tmdb", "") ?: ""
        set(v) { prefs.edit().putString("tmdb", v.trim()).apply() }
}

object Api {
    var language: String = "pt-PT"
    private const val IMG = "https://image.tmdb.org/t/p/"
    private val MODELS = listOf("gemini-3.8-flash", "gemini-flash-latest", "gemini-flash-lite-latest")

    private fun request(url: String, body: String? = null, headers: Map<String, String> = emptyMap()): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw ApiException(code, "Erro " + code + ": " + text.take(200))
            return text
        } finally {
            conn.disconnect()
        }
    }

    private fun img(base: String, p: String): String? =
        if (p.isBlank() || p == "null") null else base + p

    private fun parseMovie(o: JSONObject, forceTv: Boolean? = null): Movie? {
        val type = o.optString("media_type", "")
        if (forceTv == null && type != "movie" && type != "tv") return null
        val isTv = forceTv ?: (type == "tv")
        val title = o.optString("title", "").ifBlank { o.optString("name", "") }
        if (title.isBlank()) return null
        val date = o.optString("release_date", "").ifBlank { o.optString("first_air_date", "") }
        return Movie(
            id = o.optInt("id"),
            title = title,
            year = date.take(4),
            rating = o.optDouble("vote_average", 0.0),
            overview = o.optString("overview", ""),
            posterUrl = img(IMG + "w500", o.optString("poster_path", "")),
            backdropUrl = img(IMG + "w780", o.optString("backdrop_path", "")),
            isTv = isTv
        )
    }

    suspend fun list(tmdbKey: String, path: String): List<Movie> = withContext(Dispatchers.IO) {
        val isTv = path.contains("tv")
        val sep = if (path.contains("?")) "&" else "?"
        val json = JSONObject(request("https://api.themoviedb.org/3/" + path + sep + "language=" + language + "&api_key=" + tmdbKey))
        val arr = json.optJSONArray("results") ?: JSONArray()
        (0 until arr.length()).mapNotNull { parseMovie(arr.getJSONObject(it), isTv) }
    }

    suspend fun search(tmdbKey: String, query: String): List<Movie> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val json = JSONObject(request("https://api.themoviedb.org/3/search/multi?language=" + language + "&query=" + q + "&api_key=" + tmdbKey))
        val arr = json.optJSONArray("results") ?: JSONArray()
        (0 until arr.length()).mapNotNull { parseMovie(arr.getJSONObject(it)) }
    }

    suspend fun extras(tmdbKey: String, movie: Movie): Extras = withContext(Dispatchers.IO) {
        val kind = if (movie.isTv) "tv" else "movie"
        var trailer: String? = null
        try {
            val v = JSONObject(request("https://api.themoviedb.org/3/" + kind + "/" + movie.id + "/videos?api_key=" + tmdbKey))
            val arr = v.optJSONArray("results") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("site") == "YouTube" && o.optString("type") == "Trailer") {
                    trailer = "https://www.youtube.com/watch?v=" + o.optString("key")
                    break
                }
            }
        } catch (e: Exception) {
        }
        var link: String? = null
        val names = mutableListOf<String>()
        try {
            val p = JSONObject(request("https://api.themoviedb.org/3/" + kind + "/" + movie.id + "/watch/providers?api_key=" + tmdbKey))
            val res = p.optJSONObject("results")
            val region = res?.optJSONObject("AO") ?: res?.optJSONObject("PT") ?: res?.optJSONObject("BR") ?: res?.optJSONObject("US")
            if (region != null) {
                link = region.optString("link", "").ifBlank { null }
                for (group in listOf("flatrate", "rent", "buy")) {
                    val a = region.optJSONArray(group) ?: continue
                    for (i in 0 until a.length()) {
                        val n = a.getJSONObject(i).optString("provider_name", "")
                        if (n.isNotBlank() && n !in names) names.add(n)
                    }
                }
            }
        } catch (e: Exception) {
        }
        Extras(trailer, link, names.take(8))
    }

    private fun toBase64(bmp: Bitmap): String {
        val ratio = 768f / maxOf(bmp.width, bmp.height)
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
        } else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun friendly(code: Int): String = when (code) {
        429 -> "Chegaste ao limite grátis da IA por agora. Tenta daqui a uns minutos ou amanhã."
        500, 503, 504 -> "A IA da Google está ocupada. Tenta outra vez daqui a pouco."
        400, 401, 403 -> "A chave da Gemini não está certa. Confirma-a nas Definições."
        404 -> "O modelo de IA mudou. É preciso atualizar o app."
        else -> "A IA devolveu o erro " + code + "."
    }

    private fun askGemini(body: String, key: String, onProgress: (Float, String) -> Unit): String {
        var lastError: Exception? = null
        for (model in MODELS) {
            for (attempt in 1..3) {
                try {
                    return request(
                        "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                        body,
                        mapOf("x-goog-api-key" to key)
                    )
                } catch (e: ApiException) {
                    lastError = e
                    if (e.code == 401 || e.code == 403) throw e
                    if (e.code == 400 && (e.message ?: "").contains("API key")) throw e
                    val canRetry = e.code == 429 || e.code == 500 || e.code == 503 || e.code == 504
                    if (!canRetry) break
                    onProgress(0.7f, "A IA está ocupada. A tentar outra vez (" + attempt + " de 3)...")
                    Thread.sleep(attempt * 3000L)
                }
            }
        }
        throw lastError ?: Exception("A IA não respondeu.")
    }

    suspend fun identify(
        context: Context,
        uri: Uri,
        geminiKey: String,
        onProgress: (Float, String) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<String>()
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
            } ?: throw Exception("Não consegui abrir o vídeo.")
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0L) throw Exception("Vídeo inválido.")
            val n = 6
            for (i in 1..n) {
                val timeUs = durationMs * 1000L * i / (n + 1)
                val bmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) frames.add(toBase64(bmp))
                onProgress(0.5f * i / n, "A preparar imagens " + i + " de " + n + "...")
            }
        } finally {
            retriever.release()
        }
        if (frames.isEmpty()) throw Exception("Não consegui ler imagens do vídeo.")

        onProgress(0.6f, "A perguntar à IA...")
        val parts = JSONArray()
        parts.put(JSONObject().put("text", "Estas imagens são fotogramas de um mesmo vídeo curto de um filme, série ou anime. Diz-me quais são os 3 títulos mais prováveis, do mais provável para o menos provável. Responde APENAS com JSON no formato {\"titles\":[\"Título 1\",\"Título 2\",\"Título 3\"]}, usando o título original ou o mais conhecido. Se não fizeres ideia, responde {\"titles\":[]}."))
        frames.forEach { f ->
            parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", f)))
        }
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            .toString()
        val resp = try {
            askGemini(body, geminiKey, onProgress)
        } catch (e: ApiException) {
            throw Exception(friendly(e.code))
        }
        val text = JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val clean = text.trim().removePrefix("```json").removeSuffix("```").trim()
        val titles = JSONObject(clean).optJSONArray("titles") ?: JSONArray()
        (0 until titles.length()).map { titles.getString(it) }
    }
}
