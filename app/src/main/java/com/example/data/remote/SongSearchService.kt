package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SongSuggestion(
    val artist: String,
    val title: String,
    val album: String = "",
    val version: String = "",
    val genre: String = "",
    val artworkUrl: String? = null,
    val source: String = "Web"
) {
    val displayTrack: String
        get() = "$artist – $title"

    val displayWithAlbum: String
        get() = if (album.isNotBlank()) "$artist – $title ($album)" else "$artist – $title"
}

object SongSearchService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val curatedSongDatabase = listOf(
        SongSuggestion("Breaking Benjamin", "I Will Not Bow", "Dear Agony", "Studio", "Alternative Metal / Hard Rock", null, "Verified"),
        SongSuggestion("Breaking Benjamin", "I Will Not Bow", "Dear Agony (Live)", "Live / Acoustic", "Acoustic Rock", null, "Verified"),
        SongSuggestion("Breaking Benjamin", "I Will Not Bow", "Shallow Bay: The Best of Breaking Benjamin", "Deluxe", "Post-Grunge", null, "Verified"),
        SongSuggestion("Breaking Benjamin", "The Diary of Jane", "Phobia", "Studio", "Alternative Rock", null, "Verified"),
        SongSuggestion("Breaking Benjamin", "So Cold", "We Are Not Alone", "Studio", "Post-Grunge", null, "Verified"),
        SongSuggestion("Guns N' Roses", "Sweet Child O' Mine", "Appetite for Destruction", "Studio", "Hard Rock", null, "Verified"),
        SongSuggestion("Guns N' Roses", "Sweet Child O' Mine", "Live Era '87-'93", "Live / Acoustic", "Hard Rock", null, "Verified"),
        SongSuggestion("Pink Floyd", "Comfortably Numb", "The Wall", "Studio", "Progressive Rock", null, "Verified"),
        SongSuggestion("Pink Floyd", "Comfortably Numb", "Pulse", "Live", "Progressive Rock", null, "Verified"),
        SongSuggestion("Eli Soares", "Se Eu Cair", "Memórias", "Studio", "Gospel / Worship", null, "Verified"),
        SongSuggestion("Eli Soares", "Os Anjos Te Louvam", "Luz do Mundo", "Live", "Gospel Rock", null, "Verified"),
        SongSuggestion("Eli Soares", "Me Ajude a Melhorar", "Casa de Deus", "Live", "Worship", null, "Verified"),
        SongSuggestion("Oficina G3", "Depois da Guerra", "Depois da Guerra (DDG)", "Studio", "Progressive Metal", null, "Verified"),
        SongSuggestion("Oficina G3", "Incondicional", "Depois da Guerra (DDG)", "Studio", "Rock", null, "Verified"),
        SongSuggestion("Metallica", "Master of Puppets", "Master of Puppets", "Studio", "Thrash Metal", null, "Verified"),
        SongSuggestion("Metallica", "Enter Sandman", "Metallica (Black Album)", "Studio", "Heavy Metal", null, "Verified"),
        SongSuggestion("Stevie Ray Vaughan", "Texas Flood", "Texas Flood", "Studio", "Texas Blues", null, "Verified"),
        SongSuggestion("John Mayer", "Gravity", "Continuum", "Studio", "Blues Pop", null, "Verified"),
        SongSuggestion("Nirvana", "Smells Like Teen Spirit", "Nevermind", "Studio", "Grunge", null, "Verified"),
        SongSuggestion("Iron Maiden", "The Trooper", "Piece of Mind", "Studio", "Heavy Metal", null, "Verified"),
        SongSuggestion("AC/DC", "Back in Black", "Back in Black", "Studio", "Hard Rock", null, "Verified"),
        SongSuggestion("Red Hot Chili Peppers", "Californication", "Californication", "Studio", "Funk Rock", null, "Verified"),
        SongSuggestion("Avenged Sevenfold", "Hail to the King", "Hail to the King", "Studio", "Heavy Metal", null, "Verified"),
        SongSuggestion("Foo Fighters", "Everlong", "The Colour and the Shape", "Studio", "Alternative Rock", null, "Verified"),
        SongSuggestion("30 Seconds to Mars", "The Kill (Bury Me)", "A Beautiful Lie", "Studio", "Alternative Rock", null, "Verified")
    )

    /**
     * Searches for 3 to 5 matching song suggestions with Artist + Title + Album.
     * Uses iTunes Search API for fast zero-key music catalog lookups,
     * Gemini API if available for intelligent matching,
     * and a curated local database for instant offline support.
     */
    suspend fun searchSongs(query: String, customApiKey: String? = null): List<SongSuggestion> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        val results = mutableListOf<SongSuggestion>()

        // 1. Check local curated matches first (instant exact or partial matches)
        val localMatches = findLocalMatches(trimmed)
        results.addAll(localMatches)

        // 2. Try iTunes Search API (Fast, no API key needed, has artwork & official albums)
        try {
            val itunesResults = searchItunes(trimmed)
            for (item in itunesResults) {
                // Avoid exact duplicate artist + title + album
                if (results.none { it.artist.equals(item.artist, ignoreCase = true) && it.title.equals(item.title, ignoreCase = true) && it.album.equals(item.album, ignoreCase = true) }) {
                    results.add(item)
                }
            }
        } catch (e: Exception) {
            // Ignore network failures and proceed to Gemini or local
        }

        // If we already have 3 to 5 good matches, return them capped at 5
        if (results.size >= 3) {
            return@withContext results.take(5)
        }

        // 3. Try Gemini API for smart suggestions if API key is provided
        val apiKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> null
        }

        if (apiKey != null) {
            try {
                val geminiResults = searchGemini(trimmed, apiKey)
                for (item in geminiResults) {
                    if (results.none { it.artist.equals(item.artist, ignoreCase = true) && it.title.equals(item.title, ignoreCase = true) }) {
                        results.add(item)
                    }
                }
            } catch (e: Exception) {
                // Gemini search fallback
            }
        }

        // 4. If still under 3, generate synthetic variants from query if feasible
        if (results.isEmpty()) {
            results.addAll(generateGenericSuggestions(trimmed))
        }

        return@withContext results.take(5)
    }

    private fun findLocalMatches(query: String): List<SongSuggestion> {
        val q = query.lowercase().trim()
        return curatedSongDatabase.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            q.contains(it.title.lowercase()) ||
            it.album.lowercase().contains(q)
        }
    }

    private fun searchItunes(query: String): List<SongSuggestion> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=6"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MK20ToneGenerator/1.0")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val jsonArray = json.optJSONArray("results") ?: return emptyList()

        val list = mutableListOf<SongSuggestion>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val artist = item.optString("artistName", "").trim()
            val title = item.optString("trackName", "").trim()
            val album = item.optString("collectionName", "").trim()
            val genre = item.optString("primaryGenreName", "").trim()
            val artwork = item.optString("artworkUrl100", "").takeIf { it.isNotBlank() }

            if (artist.isNotBlank() && title.isNotBlank()) {
                val version = when {
                    title.contains("Live", ignoreCase = true) || album.contains("Live", ignoreCase = true) -> "Live"
                    title.contains("Acoustic", ignoreCase = true) || album.contains("Acoustic", ignoreCase = true) -> "Acoustic"
                    title.contains("Remaster", ignoreCase = true) -> "Remastered"
                    else -> "Studio"
                }

                list.add(
                    SongSuggestion(
                        artist = artist,
                        title = title,
                        album = album,
                        version = version,
                        genre = genre,
                        artworkUrl = artwork,
                        source = "Apple Music"
                    )
                )
            }
        }
        return list
    }

    private suspend fun searchGemini(query: String, apiKey: String): List<SongSuggestion> {
        val prompt = """
Encontre de 3 a 5 músicas ou versões reais correspondentes à busca: "$query".
Retorne ESTRITAMENTE um array JSON com objetos válidos (sem blocos ```json).
Cada objeto DEVE conter:
- "artist": Nome do artista ou banda
- "title": Título da música
- "album": Álbum original ou gravação
- "version": Versão (ex: "Studio", "Live / Acoustic", "Deluxe")
- "genre": Gênero musical ou estilo de timbre de guitarra

Exemplo de formato:
[
  {"artist": "Breaking Benjamin", "title": "I Will Not Bow", "album": "Dear Agony", "version": "Studio", "genre": "Alternative Metal"},
  {"artist": "Breaking Benjamin", "title": "I Will Not Bow", "album": "Dear Agony (Live)", "version": "Live / Acoustic", "genre": "Acoustic Rock"}
]
""".trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        val response = GeminiClient.service.generateContent(apiKey, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return emptyList()

        return parseGeminiSuggestions(text)
    }

    fun parseGeminiSuggestions(rawText: String): List<SongSuggestion> {
        val clean = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val startIdx = clean.indexOf('[')
        val endIdx = clean.lastIndexOf(']')
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) return emptyList()

        val jsonArray = JSONArray(clean.substring(startIdx, endIdx + 1))
        val list = mutableListOf<SongSuggestion>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val artist = obj.optString("artist", "").trim()
            val title = obj.optString("title", "").trim()
            val album = obj.optString("album", "").trim()
            val version = obj.optString("version", "").trim()
            val genre = obj.optString("genre", "").trim()

            if (artist.isNotBlank() && title.isNotBlank()) {
                list.add(
                    SongSuggestion(
                        artist = artist,
                        title = title,
                        album = album,
                        version = version,
                        genre = genre,
                        source = "Gemini AI"
                    )
                )
            }
        }
        return list
    }

    private fun generateGenericSuggestions(query: String): List<SongSuggestion> {
        val clean = query.trim()
        val parts = clean.split("-", "–", "by", "—").map { it.trim() }

        val (artist, title) = if (parts.size >= 2) {
            Pair(parts[0], parts[1])
        } else {
            Pair("Artista", clean)
        }

        return listOf(
            SongSuggestion(
                artist = if (artist.isNotBlank() && artist != "Artista") artist else "Versão Estúdio",
                title = title,
                album = "Versão Original de Estúdio",
                version = "Studio",
                genre = "Guitar Tone",
                source = "Sugerido"
            ),
            SongSuggestion(
                artist = if (artist.isNotBlank() && artist != "Artista") artist else "Versão Ao Vivo",
                title = title,
                album = "Ao Vivo / Live Acoustic",
                version = "Live / Acoustic",
                genre = "Acoustic / Stage",
                source = "Sugerido"
            ),
            SongSuggestion(
                artist = if (artist.isNotBlank() && artist != "Artista") artist else "Versão Remasterizada",
                title = title,
                album = "Greatest Hits / Deluxe",
                version = "Deluxe Remaster",
                genre = "Enhanced Tone",
                source = "Sugerido"
            )
        )
    }
}
