package com.cinefinder.app.api

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Library(context: Context) {
    private val prefs = context.getSharedPreferences("cinefinder_library", Context.MODE_PRIVATE)

    fun get(name: String): List<Movie> {
        val arr = try {
            JSONArray(prefs.getString(name, "[]") ?: "[]")
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

    private fun save(name: String, list: List<Movie>) {
        val arr = JSONArray()
        list.forEach {
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
        prefs.edit().putString(name, arr.toString()).apply()
    }

    fun toggle(name: String, m: Movie): List<Movie> {
        val current = get(name).toMutableList()
        val index = current.indexOfFirst { it.id == m.id && it.isTv == m.isTv }
        if (index >= 0) current.removeAt(index) else current.add(0, m)
        save(name, current)
        return current
    }

    fun push(name: String, items: List<Movie>, max: Int): List<Movie> {
        val current = get(name).toMutableList()
        items.reversed().forEach { m ->
            current.removeAll { it.id == m.id && it.isTv == m.isTv }
            current.add(0, m)
        }
        val trimmed = current.take(max)
        save(name, trimmed)
        return trimmed
    }

    fun clear(name: String) {
        prefs.edit().remove(name).apply()
    }
}
