package com.scoop.app.util

import com.tencent.mmkv.MMKV

/** Thin wrapper over the MMKV-backed key-value store used for lightweight app settings. */
object PreferenceUtil {
    private val kv by lazy { MMKV.defaultMMKV() }

    fun getBoolean(key: String, default: Boolean = false): Boolean = kv.decodeBool(key, default)

    fun putBoolean(key: String, value: Boolean) {
        kv.encode(key, value)
    }

    fun getInt(key: String, default: Int = 0): Int = kv.decodeInt(key, default)

    fun putInt(key: String, value: Int) {
        kv.encode(key, value)
    }

    fun getString(key: String, default: String = ""): String = kv.decodeString(key, default) ?: default

    fun putString(key: String, value: String) {
        kv.encode(key, value)
    }
}
