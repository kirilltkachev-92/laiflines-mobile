package com.example.nodegraph

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.nodegraph.data.di.DataModule

private const val BASE_URL = "https://api.example.com"

fun MainViewController() = ComposeUIViewController {
    val repository = remember { DataModule.provideGraphStepRepository(BASE_URL) }
    App(repository)
}
