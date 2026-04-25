package com.example.nodegraph.data.di

import com.example.nodegraph.data.remote.GraphStepApi
import com.example.nodegraph.data.repository.GraphStepRepositoryImpl
import com.example.nodegraph.domain.repository.GraphStepRepository

object DataModule {
    fun provideGraphStepRepository(baseUrl: String): GraphStepRepository {
        val client = createHttpClient()
        val api = GraphStepApi(client, baseUrl)
        return GraphStepRepositoryImpl(api)
    }
}
