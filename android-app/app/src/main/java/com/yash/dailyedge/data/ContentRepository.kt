package com.yash.dailyedge.data

import android.content.Context

/** Loads the bundled, offline lesson packs once and keeps them in memory. */
class ContentRepository(context: Context) {

    private val packs: Map<Track, TrackContent> = Track.entries.associateWith { track ->
        val json = context.assets.open("content/${track.key}.json").bufferedReader().use { it.readText() }
        ContentParser.parse(json)
    }

    private val byId: Map<String, Lesson> = packs.values.flatMap { it.lessons }.associateBy { it.id }

    fun pack(track: Track): TrackContent = packs.getValue(track)

    fun lesson(id: String): Lesson? = byId[id]
}
