package com.cinefinder.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Api2 {
    private const val TMDB = "https://api.themoviedb.org/3/"
    private const val IMG = "https://image.tmdb.org/t/p/"
    private val MODELS = listOf("gemini-3.8-flash", "gemini-flash-latest", "gemini-flash-lite-latest")

    private fun call(url: String, body: String? = null, key: String? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            if (key != null) conn.setRequestProperty("x-goog-api-key", key)
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

    private fun parse(o: JSONObject, isTv: Boolean): Movie? {
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

    suspend fun similar(tmdbKey: String, movie: Movie): List<Movie> = withContext(Dispatchers.IO) {
        val kind = if (movie.isTv) "tv" else "movie"
        val json = JSONObject(call(TMDB + kind + "/" + movie.id + "/recommendations?language=" + Api.language + "&api_key=" + tmdbKey))
        val arr = json.optJSONArray("results") ?: JSONArray()
        (0 until arr.length()).mapNotNull { parse(arr.getJSONObject(it), movie.isTv) }
    }

    suspend fun random(tmdbKey: String): Movie? = withContext(Dispatchers.IO) {
        val page = (1..15).random()
        val json = JSONObject(call(TMDB + "discover/movie?sort_by=vote_average.desc&vote_count.gte=2000&page=" + page + "&language=" + Api.language + "&api_key=" + tmdbKey))
        val arr = json.optJSONArray("results") ?: JSONArray()
        val list = (0 until arr.length()).mapNotNull { parse(arr.getJSONObject(it), false) }
        list.randomOrNull()
    }

    private fun friendly(code: Int): String = when (code) {
        429 -> "Chegaste ao limite grátis da IA por agora. Tenta daqui a uns minutos ou amanhã."
        500, 503, 504 -> "A IA da Google está ocupada. Tenta outra vez daqui a pouco."
        400, 401, 403 -> "A chave da Gemini não está certa. Confirma-a nas Definições."
        404 -> "O modelo de IA mudou. É preciso atualizar o app."
        else -> "A IA não respondeu. Tenta outra vez."
    }

    suspend fun describe(geminiKey: String, text: String): List<String> = withContext(Dispatchers.IO) {
        val prompt = "Uma pessoa descreveu um filme, série ou anime de que não se lembra do nome: \"" + text.replace("\"", "'") + "\". Diz-me os 5 títulos mais prováveis, do mais provável para o menos provável. Responde APENAS com JSON no formato {\"titles\":[\"Título 1\",\"Título 2\"]}, usando o título original ou o mais conhecido. Se não souberes, responde {\"titles\":[]}."
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            .toString()
        var lastCode = 0
        var resp: String? = null
        for (model in MODELS) {
            if (resp != null) break
            for (attempt in 1..3) {
                try {
                    resp = call(
                        "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent",
                        body,
                        geminiKey
                    )
                    break
                } catch (e: ApiException) {
                    lastCode = e.code
                    if (e.code == 401 || e.code == 403) throw Exception(friendly(e.code))
                    if (e.code == 400 && (e.message ?: "").contains("API key")) throw Exception(friendly(e.code))
                    val canRetry = e.code == 429 || e.code == 500 || e.code == 503 || e.code == 504
                    if (!canRetry) break
                    Thread.sleep(attempt * 3000L)
                }
            }
        }
        val r = resp ?: throw Exception(friendly(lastCode))
        val answer = JSONObject(r).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val clean = answer.trim().removePrefix("```json").removeSuffix("```").trim()
        val titles = JSONObject(clean).optJSONArray("titles") ?: JSONArray()
        (0 until titles.length()).map { titles.getString(it) }
    }
}
