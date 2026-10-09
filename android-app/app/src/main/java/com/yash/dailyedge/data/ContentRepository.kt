package com.yash.dailyedge.data

import android.content.Context

/** Loads every lesson pack in assets/content/ once and keeps them in memory. */
class ContentRepository(context: Context) {

    private val packs: List<TrackContent> = context.assets.list(DIR).orEmpty()
        .filter { it.endsWith(".json") }
        .map { file -> context.assets.open("$DIR/$file").bufferedReader().use { ContentParser.parse(it.readText()) } }
        .sortedWith(compareBy({ it.track.order }, { it.track.title }))

    private val byKey: Map<String, TrackContent> = packs.associateBy { it.track.key }
    private val byId: Map<String, Lesson> = packs.flatMap { it.lessons }.associateBy { it.id }

    val tracks: List<Track> = packs.map { it.track }

    fun pack(key: String): TrackContent? = byKey[key]

    fun lesson(id: String): Lesson? = byId[id]

    private companion object {
        const val DIR = "content"
    }
}
