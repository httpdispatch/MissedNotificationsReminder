package com.app.missednotificationsreminder.payment.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Purchase(val sku: String, val price: String)
