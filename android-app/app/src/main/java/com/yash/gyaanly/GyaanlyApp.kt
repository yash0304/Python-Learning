package com.yash.gyaanly

import android.app.Application
import com.yash.gyaanly.data.ContentRepository
import com.yash.gyaanly.data.PracticeStore
import com.yash.gyaanly.data.ProgressStore
import com.yash.gyaanly.domain.practice.PracticeParser

/** Holds app-wide singletons (a tiny manual DI container). */
class GyaanlyApp : Application() {
    val content by lazy { ContentRepository(this) }
    val progress by lazy { ProgressStore(this) }
    val practice by lazy { PracticeStore(this) }
    val scenarios by lazy {
        assets.open("practice/scenarios.json").bufferedReader().use { PracticeParser.parseAll(it.readText()) }
    }
}
