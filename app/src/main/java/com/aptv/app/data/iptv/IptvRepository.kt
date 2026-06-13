package com.aptv.app.data.iptv

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aptv.app.data.iptv.model.Channel
import com.aptv.app.data.iptv.model.ChannelGroup
import com.aptv.app.data.iptv.model.PlaylistSource
import com.aptv.app.data.purchase.PurchaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

private val Context.dataStore by preferencesDataStore(name = "iptv_data")

/**
 * IPTV 数据仓库（内存 + DataStore 持久化）
 * 管理播放源、频道、收藏等
 */
class IptvRepository(
    private val context: Context,
    private val purchaseManager: PurchaseManager,
    private val fetcher: PlaylistFetcher = PlaylistFetcher(),
    private val parser: M3uParser = M3uParser()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _sources = MutableStateFlow<List<PlaylistSource>>(emptyList())
    val sources: StateFlow<List<PlaylistSource>> = _sources.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _currentSourceId = MutableStateFlow<Long?>(null)
    val currentSourceId: StateFlow<Long?> = _currentSourceId.asStateFlow()

    private val _favorites = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        scope.launch {
            loadFromDataStore()
        }
    }

    fun getChannelsForSource(sourceId: Long): Flow<List<Channel>> {
        return channels.map { list -> list.filter { it.sourceId == sourceId } }
    }

    fun getGroupedChannels(sourceId: Long): Flow<List<ChannelGroup>> {
        return channels.map { list ->
            val filtered = list.filter { it.sourceId == sourceId }
            filtered.groupBy { it.group }
                .map { (group, channels) -> ChannelGroup(group, channels) }
                .sortedBy { it.name }
        }
    }

    fun getFavoriteChannels(): Flow<List<Channel>> {
        return channels.map { list -> list.filter { it.id in _favorites.value } }
    }

    fun toggleFavorite(channelId: Long) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(channelId)) {
            current.remove(channelId)
        } else {
            current.add(channelId)
        }
        _favorites.value = current
        scope.launch {
            saveFavorites()
        }
    }

    suspend fun addPlaylistSource(url: String, name: String = url): Boolean {
        if (!purchaseManager.hasFeature(com.aptv.app.data.purchase.model.PremiumFeature.UNLIMITED_PLAYLISTS) && _sources.value.isNotEmpty()) {
            _errorMessage.value = "免费版仅支持 1 个播放源，升级高级版可添加更多"
            return false
        }

        _isLoading.value = true
        _errorMessage.value = null

        val sourceId = System.currentTimeMillis()
        val fetched = fetcher.fetch(url)

        return if (fetched.isSuccess) {
            val parsed = parser.parse(fetched.getOrThrow(), sourceId, name)
            val source = PlaylistSource(
                id = sourceId,
                name = name,
                url = url,
                channelCount = parsed.channels.size
            )

            val newSources = _sources.value.toMutableList().apply { add(source) }
            val newChannels = _channels.value.toMutableList().apply { addAll(parsed.channels) }

            _sources.value = newSources
            _channels.value = newChannels
            _currentSourceId.value = sourceId
            _isLoading.value = false

            scope.launch { saveToDataStore() }
            true
        } else {
            _errorMessage.value = "加载失败: ${fetched.exceptionOrNull()?.message}"
            _isLoading.value = false
            false
        }
    }

    fun removeSource(sourceId: Long) {
        _sources.value = _sources.value.filter { it.id != sourceId }
        _channels.value = _channels.value.filter { it.sourceId != sourceId }
        if (_currentSourceId.value == sourceId) {
            _currentSourceId.value = _sources.value.firstOrNull()?.id
        }
        scope.launch { saveToDataStore() }
    }

    fun selectSource(sourceId: Long) {
        _currentSourceId.value = sourceId
    }

    fun getChannelById(channelId: Long): Channel? {
        return channels.value.firstOrNull { it.id == channelId }
    }

    suspend fun refreshCurrentSource(): Boolean {
        val sourceId = _currentSourceId.value ?: return false
        val source = _sources.value.firstOrNull { it.id == sourceId } ?: return false

        _isLoading.value = true
        val fetched = fetcher.fetch(source.url)
        return if (fetched.isSuccess) {
            val parsed = parser.parse(fetched.getOrThrow(), sourceId, source.name)
            val remaining = _channels.value.filter { it.sourceId != sourceId }
            _channels.value = remaining + parsed.channels
            _sources.value = _sources.value.map {
                if (it.id == sourceId) it.copy(channelCount = parsed.channels.size) else it
            }
            _isLoading.value = false
            scope.launch { saveToDataStore() }
            true
        } else {
            _isLoading.value = false
            _errorMessage.value = "刷新失败"
            false
        }
    }

    private suspend fun loadFromDataStore() {
        context.dataStore.data.collect { prefs ->
            val favs = prefs[stringSetPreferencesKey("favorites")] ?: emptySet()
            _favorites.value = favs.map { it.toLong() }.toSet()
        }
    }

    private suspend fun saveToDataStore() {
        context.dataStore.edit { prefs ->
            val urls = _sources.value.joinToString("|") { "${it.name}::${it.url}" }
            prefs[stringPreferencesKey("sources")] = urls
        }
    }

    private suspend fun saveFavorites() {
        context.dataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("favorites")] = _favorites.value.map { it.toString() }.toSet()
        }
    }
}
