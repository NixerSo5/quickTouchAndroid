package com.npro.nquicktouch

import DataItem
import kotlinx.serialization.Serializable

@Serializable
data class NResponse(
    val data: List<DataItem>,
    val status: Int,
    val msg: String
)