package com.aptv.app.data.purchase.model

/**
 * 用户高级版购买状态
 */
data class PurchaseState(
    val isPremium: Boolean = false,
    val isAdFree: Boolean = false,
    val purchaseTimeMs: Long = 0,
    val orderId: String? = null,
    val purchaseToken: String? = null,
    val productId: String = PRODUCT_LIFETIME,
    val isAcknowledged: Boolean = false,
    val lastVerificationTimeMs: Long = 0
) {
    companion object {
        const val PRODUCT_LIFETIME = "premium_lifetime"
        const val PRODUCT_AD_FREE = "ad_free"
    }
}

/**
 * 高级版功能枚举
 */
enum class PremiumFeature(
    val displayName: String,
    val description: String
) {
    UNLIMITED_PLAYLISTS("无限频道列表", "免费版仅允许添加 1 个播放列表，高级版可添加任意数量"),
    REAL_TIME_SUBTITLE("实时字幕识别", "通过 AI 语音识别生成视频字幕"),
    SUBTITLE_TRANSLATION("字幕翻译", "多引擎在线/离线翻译，支持 ML Kit / 百度 / 腾讯 / OpenAI / DeepL"),
    MULTI_SCREEN("多屏同播", "支持最多 9 分屏同时播放多个频道"),
    CLOUD_SYNC("云端同步", "通过 Google Drive / Firebase 同步您的播放列表与设置"),
    AD_FREE("去除广告", "移除 App 内所有广告，纯净观影体验")
}

/**
 * 字幕翻译模型枚举
 */
enum class TranslationModel(
    val displayName: String,
    val isOnline: Boolean,
    val supportedLanguages: List<String>
) {
    NONE("不翻译", false, emptyList()),
    ML_KIT_OFFLINE("ML Kit 离线翻译", false, listOf("zh", "en", "ja", "ko", "fr", "de", "es", "ru", "ar", "it", "pt")),
    OPENAI_GPT("OpenAI GPT-4o Mini", true, listOf("auto")),
    GOOGLE_CLOUD("Google Cloud Translation", true, listOf("auto")),
    DEEPL("DeepL Pro", true, listOf("auto")),
    BAIDU("百度翻译", true, listOf("zh", "en", "ja", "ko", "fr", "de", "es", "ru", "it", "pt", "ar")),
    TENCENT("腾讯翻译", true, listOf("zh", "zh-TW", "en", "ja", "ko", "fr", "de", "es", "ru", "it", "pt", "ar"));

    companion object {
        fun fromDisplayName(name: String): TranslationModel =
            values().firstOrNull { it.displayName == name } ?: NONE
    }
}

/**
 * 字幕与翻译设置
 */
data class SubtitleSettings(
    val engineType: SubtitleEngineType = SubtitleEngineType.NONE,
    val translationModel: TranslationModel = TranslationModel.NONE,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = "zh",
    val showOriginal: Boolean = true,
    val fontSize: Int = 18,
    val bgOpacity: Float = 0.7f,
    val apiKeyOpenAI: String = "",
    val apiKeyDeepL: String = "",
    val apiKeyGoogleCloud: String = "",
    val apiKeyBaiduAppId: String = "",
    val apiKeyBaiduSecret: String = "",
    val apiKeyTencentSecretId: String = "",
    val apiKeyTencentSecretKey: String = ""
)

/**
 * 字幕生成引擎类型
 */
enum class SubtitleEngineType(
    val displayName: String,
    val isOnline: Boolean
) {
    NONE("关闭字幕", false),
    WHISPER_CPP("Whisper.cpp (本地)", false),
    VOSK("Vosk 离线识别", false),
    GOOGLE_SPEECH("Google 语音识别", true),
    WHISPER_API("OpenAI Whisper API", true);

    companion object {
        fun fromDisplayName(name: String): SubtitleEngineType =
            values().firstOrNull { it.displayName == name } ?: NONE
    }
}

/**
 * 字幕段落数据模型
 */
data class SubtitleSegment(
    val id: Long = 0,
    val text: String,
    val translatedText: String? = null,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val language: String = "auto",
    val confidence: Float = 0f,
    val isFinal: Boolean = false
)
