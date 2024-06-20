package com.npro.nquicktouch

import kotlinx.serialization.Serializable

@Serializable
data class GridItem(
    val id: Int,
    val base64Image: String
)