package com.withcall.app

data class Device(
    val id: Int,
    val phoneNumber: String,
    val msgB: String,
    val msgC: String,
    val msgD: String,
    val msgE: String,
    val msgF: String,
    val status: String = "pending"
)
