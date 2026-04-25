package com.example.nodegraph.data.repository

import com.example.nodegraph.data.remote.GraphStepApi
import com.example.nodegraph.data.remote.dto.toDomain
import com.example.nodegraph.domain.model.GraphStep
import com.example.nodegraph.domain.repository.GraphStepRepository

class GraphStepRepositoryImpl(
    private val api: GraphStepApi
) : GraphStepRepository {
    override suspend fun getNextStep(): GraphStep = api.fetchNextStep().toDomain()
}
