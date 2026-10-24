package com.cinefinder.app.api

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Favorites(context: Context) {
    private val prefs = context.getSharedPreferences("cinefinder_favs", Context.MODE_PRIVATE)

    fun all(): List<Movie> {
        val arr = try {
            JSONArray(prefs.getString("list", "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Movie(
                id = o.optInt("id"),
                title = o.optString("title"),
                year = o.optString("year"),
                rating = o.optDouble("rating", 0.0),
                overview = o.optString("overview"),
                posterUrl = o.optString("poster", "").ifBlank { null },
                backdropUrl = o.optString("backdrop", "").ifBlank { null },
                isTv = o.optBoolean("tv")
            )
        }
    }

    fun toggle(m: Movie): List<Movie> {
        val current = all().toMutableList()
        val index = current.indexOfFirst { it.id == m.id && it.isTv == m.isTv }
        if (index >= 0) current.removeAt(index) else current.add(0, m)
        val arr = JSONArray()
        current.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("year", it.year)
                    .put("rating", it.rating)
                    .put("overview", it.overview)
                    .put("poster", it.posterUrl ?: "")
                    .put("backdrop", it.backdropUrl ?: "")
                    .put("tv", it.isTv)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
        return current
    }
}
