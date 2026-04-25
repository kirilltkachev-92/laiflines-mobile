package com.example.nodegraph.data.remote.dto

import com.example.nodegraph.domain.model.GraphStep
import kotlinx.serialization.Serializable

@Serializable
data class GraphStepDto(
    val nodeCount: Int
)

fun GraphStepDto.toDomain(): GraphStep = GraphStep(nodeCount = nodeCount)
