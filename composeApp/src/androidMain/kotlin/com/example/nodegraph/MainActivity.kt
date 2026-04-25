package com.example.nodegraph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.nodegraph.data.di.DataModule

private const val BASE_URL = "https://api.example.com"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = DataModule.provideGraphStepRepository(BASE_URL)
        setContent { App(repository) }
    }
}
