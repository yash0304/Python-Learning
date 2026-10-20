package com.yash.gyaanly.data

import android.content.Context

/** Loads every lesson pack in assets/content/ once and keeps them in memory. */
class ContentRepository(private val context: Context) {

    private val packs: List<TrackContent> = context.assets.list(DIR).orEmpty()
        .filter { it.endsWith(".json") }
        .map { file -> context.assets.open("$DIR/$file").bufferedReader().use { ContentParser.parse(it.readText()) } }
        .sortedWith(compareBy({ it.track.order }, { it.track.title }))

    private val byKey: Map<String, TrackContent> = packs.associateBy { it.track.key }
    private val byId: Map<String, Lesson> = packs.flatMap { it.lessons }.associateBy { it.id }

    val tracks: List<Track> = packs.map { it.track }

    fun pack(key: String): TrackContent? = byKey[key]

    fun lesson(id: String): Lesson? = byId[id]

    private val books = mutableMapOf<String, Book?>()

    /** Loaded on first open; books are large and most sessions never read one. */
    fun book(key: String): Book? = books.getOrPut(key) {
        runCatching { context.assets.open("$BOOKS/$key.json").bufferedReader().use { ContentParser.parseBook(it.readText()) } }.getOrNull()
    }

    /** The KDP-ready EPUB bundled next to the book, for saving to the phone. */
    fun bookEpub(key: String): ByteArray? =
        runCatching { context.assets.open("$BOOKS/$key.epub").use { it.readBytes() } }.getOrNull()

    private companion object {
        const val DIR = "content"
        const val BOOKS = "books"
    }
}
