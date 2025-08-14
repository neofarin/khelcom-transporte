package com.khelcomtransporte.envios

import android.content.Context

object Prefs {
    private const val FILE = "khelcom_prefs"
    private const val K_URL = "base_url"
    private const val K_USER = "username"
    private const val K_PASS = "app_password"

    fun save(context: Context, baseUrl: String, user: String, pass: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(K_URL, baseUrl.trimEnd('/'))
            .putString(K_USER, user.trim())
            .putString(K_PASS, pass)
            .apply()
    }

    fun getBaseUrl(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_URL, null)

    fun getUser(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_USER, null)

    fun getPass(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(K_PASS, null)

    fun hasCreds(context: Context): Boolean =
        !getBaseUrl(context).isNullOrBlank() &&
                !getUser(context).isNullOrBlank() &&
                !getPass(context).isNullOrBlank()

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
