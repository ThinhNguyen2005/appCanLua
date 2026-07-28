package com.giathinh.canlua.data.model

import org.json.JSONObject
import java.util.Date

fun Card.serialize(): String {
    return JSONObject().apply {
        put("schemaVersion", 1)
        put("name", name)
        put("cccd", cccd ?: JSONObject.NULL)
        put("traderName", traderName)
        put("date", date.time)
        put("totalWeight", totalWeight)
        put("bagWeight", bagWeight)
        put("impurityWeight", impurityWeight)
        put("netWeight", netWeight)
        put("depositAmount", depositAmount)
        put("pricePerKg", pricePerKg)
        put("totalAmount", totalAmount)
        put("paidAmount", paidAmount)
        put("remainingAmount", remainingAmount)
        put("bagCount", bagCount)
        put("isLocked", isLocked)
        put("isPaid", isPaid)
        put("riceVariety", riceVariety)
        put("moisturePercent", moisturePercent)
        put("seasonLabel", seasonLabel)
        put("qrToken", qrToken ?: JSONObject.NULL)
        put("lockedByTraderId", lockedByTraderId ?: JSONObject.NULL)
        put("latitude", latitude ?: JSONObject.NULL)
        put("longitude", longitude ?: JSONObject.NULL)
        put("traderPhone", traderPhone)
        put("fieldAddress", fieldAddress)
        put("impurityIsPercent", impurityIsPercent)
        put("bagMethodIsSampling", bagMethodIsSampling)
        put("bagSampleCount", bagSampleCount)
        put("bagSampleTotalWeight", bagSampleTotalWeight)
        put("weightInputMode", weightInputMode)
    }.toString()
}

fun deserializeCard(json: String): Card? = runCatching {
    val map = parseSimpleJson(json)
    Card(
        ownerUid = "",
        name = map["name"]?.toString().orEmpty(),
        cccd = map["cccd"] as? String,
        traderName = map["traderName"]?.toString().orEmpty(),
        date = Date((map["date"] as? Number)?.toLong() ?: 0L),
        totalWeight = (map["totalWeight"] as? Number)?.toDouble() ?: 0.0,
        bagWeight = (map["bagWeight"] as? Number)?.toDouble() ?: 0.0,
        impurityWeight = (map["impurityWeight"] as? Number)?.toDouble() ?: 0.0,
        netWeight = (map["netWeight"] as? Number)?.toDouble() ?: 0.0,
        depositAmount = (map["depositAmount"] as? Number)?.toDouble() ?: 0.0,
        pricePerKg = (map["pricePerKg"] as? Number)?.toDouble() ?: 0.0,
        totalAmount = (map["totalAmount"] as? Number)?.toDouble() ?: 0.0,
        paidAmount = (map["paidAmount"] as? Number)?.toDouble() ?: 0.0,
        remainingAmount = (map["remainingAmount"] as? Number)?.toDouble() ?: 0.0,
        bagCount = (map["bagCount"] as? Number)?.toInt() ?: 0,
        isLocked = map["isLocked"] as? Boolean ?: false,
        isPaid = map["isPaid"] as? Boolean ?: false,
        riceVariety = map["riceVariety"]?.toString().orEmpty(),
        moisturePercent = (map["moisturePercent"] as? Number)?.toDouble() ?: 0.0,
        seasonLabel = map["seasonLabel"]?.toString().orEmpty(),
        qrToken = map["qrToken"] as? String,
        lockedByTraderId = map["lockedByTraderId"] as? String,
        latitude = (map["latitude"] as? Number)?.toDouble(),
        longitude = (map["longitude"] as? Number)?.toDouble(),
        traderPhone = map["traderPhone"]?.toString().orEmpty(),
        fieldAddress = map["fieldAddress"]?.toString().orEmpty(),
        impurityIsPercent = map["impurityIsPercent"] as? Boolean ?: false,
        bagMethodIsSampling = map["bagMethodIsSampling"] as? Boolean ?: false,
        bagSampleCount = (map["bagSampleCount"] as? Number)?.toInt() ?: 0,
        bagSampleTotalWeight = (map["bagSampleTotalWeight"] as? Number)?.toDouble() ?: 0.0,
        weightInputMode = map["weightInputMode"]?.toString().takeIf { !it.isNullOrBlank() } ?: "SMALL"
    )
}.getOrNull()

private fun parseSimpleJson(json: String): Map<String, Any?> {
    val obj = JSONObject(json)
    return buildMap {
        obj.keys().forEach { k ->
            put(k, if (obj.isNull(k)) null else obj.get(k))
        }
    }
}
