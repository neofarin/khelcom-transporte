package com.khelcomtransporte.envios

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ApiClient {
    private var baseUrl: String = ""
    private var username: String = ""
    private var appPassword: String = ""

    private val client = OkHttpClient()
    private val gson = Gson()

    fun configure(baseUrl: String, username: String, appPassword: String) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.username = username
        this.appPassword = appPassword
    }

    private fun authHeader(): String = Credentials.basic(username, appPassword)

    fun getShipmentsParsed(): List<Shipment> {
        val req = Request.Builder()
            .url("$baseUrl/wp-json/envios/v1/list")
            .header("Authorization", authHeader())
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            val body = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw RuntimeException("Error ${res.code}: $body")
            val type = object : TypeToken<List<Shipment>>() {}.type
            return gson.fromJson(body, type) ?: emptyList()
        }
    }

    // ⬇⬇⬇ AHORA ACEPTA Long (no hace falta .toInt())
    fun updateEnvio(id: Long, status: String): String? {
        val json = """{"id": $id, "status": ${gson.toJson(status)}}"""
        val body: RequestBody = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$baseUrl/wp-json/envios/v1/update")
            .post(body)
            .header("Authorization", authHeader())
            .header("Accept", "application/json")
            .build()
        client.newCall(req).execute().use { res ->
            return if (res.isSuccessful) res.body?.string()
            else "Error: ${res.code} ${res.message}"
        }
    }
}
