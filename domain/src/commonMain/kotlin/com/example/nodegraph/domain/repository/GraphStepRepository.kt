package com.example.nodegraph.domain.repository

import com.example.nodegraph.domain.model.GraphStep

interface GraphStepRepository {
    suspend fun getNextStep(): GraphStep
}
