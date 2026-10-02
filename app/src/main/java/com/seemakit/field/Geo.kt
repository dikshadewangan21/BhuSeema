package com.seemakit.field
import org.json.*
import kotlin.math.*

data class Fix(val lat: Double, val lon: Double, val quality: Int, val sats: Int, val hdop: Double)

object Nmea { // parses $xxGGA sentences streamed by the rover over Bluetooth SPP
    fun parse(s: String): Fix? {
        if (s.length < 10 || s[0] != '$' || !s.substring(3).startsWith("GGA")) return null
        val f = s.substringBefore('*').split(',')
        if (f.size < 9) return null
        val q = f[6].toIntOrNull() ?: return null
        if (q == 0 || f[2].isEmpty() || f[4].isEmpty()) return null
        return try {
            val lat = (f[2].substring(0, 2).toDouble() + f[2].substring(2).toDouble() / 60) * (if (f[3] == "S") -1 else 1)
            val lon = (f[4].substring(0, 3).toDouble() + f[4].substring(3).toDouble() / 60) * (if (f[5] == "W") -1 else 1)
            Fix(lat, lon, q, f[7].toIntOrNull() ?: 0, f[8].toDoubleOrNull() ?: 99.0)
        } catch (e: Exception) { null }
    }
}

object Geo {
    private fun xy(c: List<Corner>): List<Pair<Double, Double>> {
        if (c.isEmpty()) return emptyList()
        val kx = 111320.0 * cos(Math.toRadians(c[0].lat))
        return c.map { (it.lon - c[0].lon) * kx to (it.lat - c[0].lat) * 110540.0 }
    }
    fun area(c: List<Corner>): Double {
        val p = xy(c); if (p.size < 3) return 0.0
        var s = 0.0
        for (i in p.indices) { val j = (i + 1) % p.size; s += p[i].first * p[j].second - p[j].first * p[i].second }
        return abs(s) / 2
    }
    fun perimeter(c: List<Corner>): Double {
        val p = xy(c); if (p.size < 2) return 0.0
        return p.indices.sumOf { i -> val j = (i + 1) % p.size; hypot(p[i].first - p[j].first, p[i].second - p[j].second) }
    }
    fun dist(a: Double, b: Double, c: Double, d: Double): Double {
        val r = 6371000.0; val dl = Math.toRadians(c - a); val dn = Math.toRadians(d - b)
        val h = sin(dl / 2).pow(2) + cos(Math.toRadians(a)) * cos(Math.toRadians(c)) * sin(dn / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
    fun bearing(a: Double, b: Double, c: Double, d: Double): Double {
        val dn = Math.toRadians(d - b)
        val y = sin(dn) * cos(Math.toRadians(c))
        val x = cos(Math.toRadians(a)) * sin(Math.toRadians(c)) - sin(Math.toRadians(a)) * cos(Math.toRadians(c)) * cos(dn)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}

object Export {
    fun json(p: Parcel, c: List<Corner>): String {
        val ring = JSONArray(); c.forEach { ring.put(JSONArray().put(it.lon).put(it.lat)) }
        if (c.size > 2) ring.put(JSONArray().put(c[0].lon).put(c[0].lat))
        val pts = JSONArray(); c.forEach {
            pts.put(JSONObject().put("seq", it.seq).put("lat", it.lat).put("lon", it.lon).put("fix", it.quality).put("hdop", it.hdop)
                .put("sats", it.sats).put("time", it.time).put("gcp", it.isGcp).put("owner", it.ownerWitness).put("neighbour", it.neighbourWitness))
        }
        return JSONObject().put("type", "Feature")
            .put("geometry", JSONObject().put("type", "Polygon").put("coordinates", JSONArray().put(ring)))
            .put("properties", JSONObject().put("surveyNo", p.surveyNo).put("village", p.village).put("rorAreaSqm", p.rorAreaSqm)
                .put("measuredAreaSqm", Geo.area(c)).put("points", pts)).toString()
    }
}
