package com.florosoft.florobeat.data

import android.content.Context
import com.florosoft.florobeat.data.model.HomeShelf
import com.florosoft.florobeat.data.model.ShelfItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * High-performance, zero-latency disk cache for the FloroBeat Home feed.
 *
 * Persists the latest successfully loaded home shelves and recents to internal storage.
 * On application cold-start, `loadCached()` provides instantaneous UI state (<20ms),
 * completely bypassing skeleton/shimmer delays and achieving an immediate first meaningful frame.
 */
object HomeFeedCache {

    private const val FILE_NAME = "florobeat_home_cache.json"
    private var cacheFile: File? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var inMemoryCache: List<HomeShelf>? = null

    fun init(context: Context) {
        if (cacheFile == null) {
            cacheFile = File(context.filesDir, FILE_NAME)
        }
    }

    /**
     * Synchronously returns cached shelves if available.
     * Takes <20ms on modern storage, safe to call during ViewModel initialization.
     */
    fun loadCached(): List<HomeShelf>? {
        inMemoryCache?.let { return it }
        val file = cacheFile ?: return null
        if (!file.exists() || file.length() == 0L) return null

        return runCatching {
            val content = file.readText()
            val jsonArray = JSONArray(content)
            val list = ArrayList<HomeShelf>(jsonArray.length())

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title")
                val subtitle = obj.optString("subtitle", "")
                val moreBrowseId = obj.optString("moreBrowseId").ifBlank { null }
                val moreParams = obj.optString("moreParams").ifBlank { null }

                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                val items = ArrayList<ShelfItem>(itemsArray.length())
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    items.add(
                        ShelfItem(
                            title = itemObj.optString("title"),
                            subtitle = itemObj.optString("subtitle", ""),
                            thumbnailUrl = itemObj.optString("thumbnailUrl").ifBlank { null },
                            videoId = itemObj.optString("videoId").ifBlank { null },
                            browseId = itemObj.optString("browseId").ifBlank { null },
                        ),
                    )
                }
                list.add(
                    HomeShelf(
                        title = title,
                        subtitle = subtitle,
                        moreBrowseId = moreBrowseId,
                        moreParams = moreParams,
                        items = items,
                    ),
                )
            }
            inMemoryCache = list
            list
        }.getOrNull()
    }

    /**
     * Asynchronously writes shelves to disk so UI threads are never blocked.
     */
    fun saveCache(shelves: List<HomeShelf>) {
        if (shelves.isEmpty()) return
        inMemoryCache = shelves
        val file = cacheFile ?: return

        ioScope.launch {
            runCatching {
                val jsonArray = JSONArray()
                shelves.take(15).forEach { shelf ->
                    val shelfObj = JSONObject().apply {
                        put("title", shelf.title)
                        put("subtitle", shelf.subtitle)
                        put("moreBrowseId", shelf.moreBrowseId ?: "")
                        put("moreParams", shelf.moreParams ?: "")

                        val itemsArray = JSONArray()
                        shelf.items.take(30).forEach { item ->
                            itemsArray.put(
                                JSONObject().apply {
                                    put("title", item.title)
                                    put("subtitle", item.subtitle)
                                    put("thumbnailUrl", item.thumbnailUrl ?: "")
                                    put("videoId", item.videoId ?: "")
                                    put("browseId", item.browseId ?: "")
                                },
                            )
                        }
                        put("items", itemsArray)
                    }
                    jsonArray.put(shelfObj)
                }
                file.writeText(jsonArray.toString())
            }
        }
    }
}
