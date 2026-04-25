package com.example.nodegraph.data.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpClientEngineFactory(): HttpClient = HttpClient(OkHttp)
