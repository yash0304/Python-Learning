package com.yash.dailyedge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yash.dailyedge.ui.AppNav
import com.yash.dailyedge.ui.AppViewModel
import com.yash.dailyedge.ui.theme.DailyEdgeTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyEdgeTheme { AppNav(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        // The app may stay open past midnight: make sure "today" is really today.
        vm.refreshPlan()
    }
}
