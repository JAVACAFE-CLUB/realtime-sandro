package com.sandro.realtime.codex.domain

data class WikiMetadata(
    val namespace: Int = 0,
    val pageId: Long? = null,
    val revisionId: Long? = null,
    val contributor: WikiContributor? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["namespace"] = namespace
        pageId?.let { map["pageId"] = it }
        revisionId?.let { map["revisionId"] = it }
        contributor?.let { map["contributor"] = it.toMap() }
        return map
    }
}

data class WikiContributor(
    val username: String? = null,
    val id: Long? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        username?.let { map["username"] = it }
        id?.let { map["id"] = it }
        return map
    }
}

data class NewsMetadata(
    val htmlContent: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val siteName: String? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        htmlContent?.let { map["htmlContent"] = it }
        description?.let { map["description"] = it }
        imageUrl?.let { map["imageUrl"] = it }
        siteName?.let { map["siteName"] = it }
        return map
    }
}

data class TweetMetadata(
    val tweetId: String? = null,
    val userId: String? = null,
    val retweetCount: Int = 0,
    val favoriteCount: Int = 0,
    val replyToTweetId: String? = null,
    val replyToUserId: String? = null,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val isRetweet: Boolean = false,
    val originalTweetId: String? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        tweetId?.let { map["tweetId"] = it }
        userId?.let { map["userId"] = it }
        map["retweetCount"] = retweetCount
        map["favoriteCount"] = favoriteCount
        replyToTweetId?.let { map["replyToTweetId"] = it }
        replyToUserId?.let { map["replyToUserId"] = it }
        map["hashtags"] = hashtags
        map["mentions"] = mentions
        map["isRetweet"] = isRetweet
        originalTweetId?.let { map["originalTweetId"] = it }
        return map
    }
}