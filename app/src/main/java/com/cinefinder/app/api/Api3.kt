package com.cinefinder.app.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Person(val name: String, val role: String, val photoUrl: String?)

data class Season(val number: Int, val name: String, val episodes: Int, val year: String, val posterUrl: String?)

data class Details(
    val genres: List<String>,
    val runtime: String,
    val tagline: String,
    val voteCount: Int,
    val cast: List<Person>,
    val seasons: List<Season>,
    val trailerUrl: String?,
    val whereLink: String?,
    val providers: List<String>,
    val related: List<Movie>
)

object Api3 {
    private const val TMDB = "https://api.themoviedb.org/3/"
    private const val IMG = "https://image.tmdb.org/t/p/"
    private val MODELS = listOf("gemini-3.8-flash", "gemini-flash-latest", "gemini-flash-lite-latest")

    private fun req(url: String, body: String? = null, key: String? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 45000
            if (key != null) conn.setRequestProperty("x-goog-api-key", key)
            if (body != null) {
                val bytes = body.toByteArray()
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(bytes.size)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(bytes) }
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

    private fun pic(base: String, p: String): String? =
        if (p.isBlank() || p == "null") null else base + p

    private fun mv(o: JSONObject, isTv: Boolean): Movie? {
        val title = o.optString("title", "").ifBlank { o.optString("name", "") }
        if (title.isBlank()) return null
        val date = o.optString("release_date", "").ifBlank { o.optString("first_air_date", "") }
        return Movie(
            o.optInt("id"),
            title,
            date.take(4),
            o.optDouble("vote_average", 0.0),
            o.optString("overview", ""),
            pic(IMG + "w500", o.optString("poster_path", "")),
            pic(IMG + "w780", o.optString("backdrop_path", "")),
            isTv
        )
    }

    suspend fun details(tmdbKey: String, m: Movie): Details = withContext(Dispatchers.IO) {
        val kind = if (m.isTv) "tv" else "movie"
        val url = TMDB + kind + "/" + m.id + "?language=" + Api.language +
            "&include_video_language=pt,en,null&append_to_response=credits,videos,watch/providers,recommendations&api_key=" + tmdbKey
        val o = JSONObject(req(url))

        val genres = ArrayList<String>()
        val ga = o.optJSONArray("genres")
        if (ga != null) {
            for (i in 0 until ga.length()) genres.add(ga.getJSONObject(i).optString("name"))
        }

        var runtime = ""
        if (m.isTv) {
            val s = o.optInt("number_of_seasons", 0)
            val e = o.optInt("number_of_episodes", 0)
            if (s > 0) {
                runtime = s.toString() + (if (s == 1) " temporada" else " temporadas")
                if (e > 0) runtime = runtime + " · " + e + " episódios"
            }
        } else {
            val r = o.optInt("runtime", 0)
            if (r > 0) runtime = (r / 60).toString() + "h " + (r % 60) + "min"
        }

        val cast = ArrayList<Person>()
        val ca = o.optJSONObject("credits")?.optJSONArray("cast")
        if (ca != null) {
            for (i in 0 until minOf(ca.length(), 15)) {
                val c = ca.getJSONObject(i)
                cast.add(Person(c.optString("name"), c.optString("character"), pic(IMG + "w185", c.optString("profile_path", ""))))
            }
        }

        val seasons = ArrayList<Season>()
        val sa = o.optJSONArray("seasons")
        if (m.isTv && sa != null) {
            for (i in 0 until sa.length()) {
                val s = sa.getJSONObject(i)
                if (s.optInt("season_number") > 0) {
                    seasons.add(
                        Season(
                            s.optInt("season_number"),
                            s.optString("name"),
                            s.optInt("episode_count"),
                            s.optString("air_date", "").take(4),
                            pic(IMG + "w185", s.optString("poster_path", ""))
                        )
                    )
                }
            }
        }

        var trailer: String? = null
        var teaser: String? = null
        val va = o.optJSONObject("videos")?.optJSONArray("results")
        if (va != null) {
            for (i in 0 until va.length()) {
                val v = va.getJSONObject(i)
                if (v.optString("site") != "YouTube") continue
                val link = "https://www.youtube.com/watch?v=" + v.optString("key")
                if (v.optString("type") == "Trailer") {
                    trailer = link
                    break
                }
                if (teaser == null) teaser = link
            }
        }
        if (trailer == null) trailer = teaser

        var whereLink: String? = null
        val names = ArrayList<String>()
        val res = o.optJSONObject("watch/providers")?.optJSONObject("results")
        val region = res?.optJSONObject("AO") ?: res?.optJSONObject("PT") ?: res?.optJSONObject("BR") ?: res?.optJSONObject("US")
        if (region != null) {
            whereLink = region.optString("link", "").ifBlank { null }
            for (group in listOf("flatrate", "rent", "buy")) {
                val a = region.optJSONArray(group) ?: continue
                for (i in 0 until a.length()) {
                    val n = a.getJSONObject(i).optString("provider_name", "")
                    if (n.isNotBlank() && n !in names) names.add(n)
                }
            }
        }

        val related = ArrayList<Movie>()
        val ra = o.optJSONObject("recommendations")?.optJSONArray("results")
        if (ra != null) {
            for (i in 0 until ra.length()) {
                val r = mv(ra.getJSONObject(i), m.isTv)
                if (r != null) related.add(r)
            }
        }

        Details(
            genres,
            runtime,
            o.optString("tagline", ""),
            o.optInt("vote_count", 0),
            cast,
            seasons,
            trailer,
            whereLink,
            names.take(8),
            related
        )
    }

    private fun friendly(code: Int): String = when (code) {
        429 -> "Chegaste ao limite grátis da IA por agora. Tenta daqui a uns minutos ou amanhã."
        500, 503, 504 -> "A IA da Google está ocupada. Tenta outra vez daqui a pouco."
        400, 401, 403 -> "A chave da Gemini não está certa. Confirma-a nas Definições."
        404 -> "O modelo de IA mudou. É preciso atualizar o app."
        else -> "A IA devolveu o erro " + code + "."
    }

    private fun ask(body: String, key: String, onProgress: (Float, String) -> Unit): String {
        val started = System.currentTimeMillis()
        var last: Exception? = null
        for (model in MODELS) {
            for (attempt in 1..3) {
                if (System.currentTimeMillis() - started > 150000L) {
                    throw Exception("A IA demorou demasiado a responder. Verifica a ligação e tenta outra vez.")
                }
                try {
                    return req("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent", body, key)
                } catch (e: ApiException) {
                    last = e
                    if (e.code == 401 || e.code == 403) throw Exception(friendly(e.code))
                    if (e.code == 400 && (e.message ?: "").contains("API key")) throw Exception(friendly(e.code))
                    val retry = e.code == 429 || e.code == 500 || e.code == 503 || e.code == 504
                    if (!retry) break
                    onProgress(0.7f, "A IA está ocupada. A tentar outra vez (" + attempt + " de 3)...")
                    Thread.sleep(attempt * 3000L)
                } catch (e: IOException) {
                    last = e
                    onProgress(0.7f, "A ligação está lenta. A tentar outra vez...")
                    if (attempt >= 2) break
                }
            }
        }
        val l = last
        if (l is ApiException) throw Exception(friendly(l.code))
        throw Exception("Não consegui falar com a IA. Verifica a ligação e tenta outra vez.")
    }

    private fun titlesFrom(resp: String): List<String> {
        val text = JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val clean = text.trim().removePrefix("```json").removeSuffix("```").trim()
        val arr = JSONObject(clean).optJSONArray("titles") ?: JSONArray()
        return (0 until arr.length()).map { arr.getString(it) }
    }

    suspend fun identifyImage(
        context: Context,
        uri: Uri,
        geminiKey: String,
        hint: String,
        onProgress: (Float, String) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        onProgress(0.1f, "A preparar a imagem...")
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = sample
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw Exception("Não consegui abrir a imagem.")
        val ratio = 768f / maxOf(bmp.width, bmp.height)
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
        } else bmp
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        onProgress(0.5f, "A enviar para a IA...")
        val prompt = "Esta imagem é um fotograma, cartaz ou captura de ecrã de um filme, série ou anime. " +
            (if (hint.isNotBlank()) "Pista do utilizador: \"" + hint.replace("\"", "'") + "\". " else "") +
            "Diz-me quais são os 5 títulos mais prováveis, do mais provável para o menos provável. " +
            "Responde APENAS com JSON no formato {\"titles\":[\"Título 1\",\"Título 2\"]}, usando o título original ou o mais conhecido. " +
            "Se não tiveres ideia, responde {\"titles\":[]}."
        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", b64)))
        val body = JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            .toString()
        titlesFrom(ask(body, geminiKey, onProgress))
    }
}
