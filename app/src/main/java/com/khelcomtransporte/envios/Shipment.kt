package com.khelcomtransporte.envios

data class Shipment(
    val id: Long,
    val title: String?,
    val status: String?,
    val track: String?,
    val dest: String?
)
