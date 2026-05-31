package com.GiaThinh.canlua.data.model

import org.json.JSONObject
import java.util.Date

fun Card.serialize(): String {
    val sb = StringBuilder("{")
    sb.append("\"name\":${jsonString(name)},")
    sb.append("\"cccd\":${jsonNullable(cccd)},")
    sb.append("\"traderName\":${jsonString(traderName)},")
    sb.append("\"date\":${date.time},")
    sb.append("\"totalWeight\":${totalWeight},")
    sb.append("\"bagWeight\":${bagWeight},")
    sb.append("\"impurityWeight\":${impurityWeight},")
    sb.append("\"netWeight\":${netWeight},")
    sb.append("\"depositAmount\":${depositAmount},")
    sb.append("\"pricePerKg\":${pricePerKg},")
    sb.append("\"totalAmount\":${totalAmount},")
    sb.append("\"paidAmount\":${paidAmount},")
    sb.append("\"remainingAmount\":${remainingAmount},")
    sb.append("\"bagCount\":${bagCount},")
    sb.append("\"isLocked\":${isLocked},")
    sb.append("\"isPaid\":${isPaid},")
    sb.append("\"riceVariety\":${jsonString(riceVariety)},")
    sb.append("\"moisturePercent\":${moisturePercent},")
    sb.append("\"seasonLabel\":${jsonString(seasonLabel)},")
    sb.append("\"qrToken\":${jsonNullable(qrToken)},")
    sb.append("\"lockedByTraderId\":${jsonNullable(lockedByTraderId)},")
    sb.append("\"latitude\":${latitude ?: "null"},")
    sb.append("\"longitude\":${longitude ?: "null"},")
    sb.append("\"traderPhone\":${jsonString(traderPhone)},")
    sb.append("\"fieldAddress\":${jsonString(fieldAddress)},")
    sb.append("\"impurityIsPercent\":${impurityIsPercent},")
    sb.append("\"bagMethodIsSampling\":${bagMethodIsSampling},")
    sb.append("\"bagSampleCount\":${bagSampleCount},")
    sb.append("\"bagSampleTotalWeight\":${bagSampleTotalWeight},")
    sb.append("\"weightInputMode\":${jsonString(weightInputMode)}")
    sb.append("}")
    return sb.toString()
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

private fun jsonString(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun jsonNullable(s: String?): String =
    if (s == null) "null" else jsonString(s)

private fun parseSimpleJson(json: String): Map<String, Any?> {
    val obj = JSONObject(json)
    return buildMap {
        obj.keys().forEach { k ->
            put(k, if (obj.isNull(k)) null else obj.get(k))
        }
    }
}
