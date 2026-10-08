package com.yash.dailyedge

import android.app.Application
import com.yash.dailyedge.data.ContentRepository
import com.yash.dailyedge.data.ProgressStore

/** Holds app-wide singletons (a tiny manual DI container). */
class DailyEdgeApp : Application() {
    val content by lazy { ContentRepository(this) }
    val progress by lazy { ProgressStore(this) }
}
