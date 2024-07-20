package com.example.chatapplication.Utils

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.example.chatapplication.Utils.Constants.Companion.KEY_PREFERENCE_NAME

class PreferenceManager(private val context: Context) {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: Editor

    fun instance() {
        sharedPreferences =
            context.getSharedPreferences(KEY_PREFERENCE_NAME, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    fun putString(key: String, value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun clear() {
        editor.clear()
        editor.apply()
    }
}