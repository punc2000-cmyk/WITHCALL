package com.withcall.app

data class Device(
    val rowIndex: Int,
    val phone119: String,   // D열
    val phoneCare: String,  // F열
    val contact1: String,   // H열
    val contact2: String,   // I열
    val contact3: String,   // J열
    val contact4: String,   // K열
    val contact5: String    // L열
) {
    fun contacts() = listOf(contact1, contact2, contact3, contact4, contact5)
}
