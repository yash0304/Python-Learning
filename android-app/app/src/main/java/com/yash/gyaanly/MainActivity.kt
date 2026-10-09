package com.yash.gyaanly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.yash.gyaanly.ui.AppNav
import com.yash.gyaanly.ui.AppViewModel
import com.yash.gyaanly.ui.theme.GyaanlyTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GyaanlyTheme { AppNav(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        // The app may stay open past midnight: make sure "today" is really today.
        vm.refreshPlan()
    }
}
