package com.example.nodegraph.data.remote

import com.example.nodegraph.data.remote.dto.GraphStepDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class GraphStepApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    suspend fun fetchNextStep(): GraphStepDto =
        client.get("$baseUrl/graph/next-step").body()
}
