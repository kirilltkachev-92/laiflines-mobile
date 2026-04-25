package com.example.nodegraph.data.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun httpClientEngineFactory(): HttpClient = HttpClient(Darwin)
