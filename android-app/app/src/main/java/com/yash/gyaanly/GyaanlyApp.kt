package com.yash.gyaanly

import android.app.Application
import com.yash.gyaanly.data.ContentRepository
import com.yash.gyaanly.data.ProgressStore

/** Holds app-wide singletons (a tiny manual DI container). */
class GyaanlyApp : Application() {
    val content by lazy { ContentRepository(this) }
    val progress by lazy { ProgressStore(this) }
}
