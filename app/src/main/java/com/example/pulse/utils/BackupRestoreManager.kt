package com.example.pulse.utils

import android.content.Context
import android.net.Uri
import com.example.pulse.data.Subscription
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

class BackupRestoreManager(private val context: Context) {

    private val gson = Gson()

    fun generateBackupFileName(): String {
        return "subscriptions_backup.json"
    }

    fun exportSubscriptions(subscriptions: List<Subscription>, uri: Uri): Boolean {
        return try {
            val jsonString = gson.toJson(subscriptions)
            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    stream.write(jsonString.toByteArray())
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importSubscriptions(uri: Uri): List<Subscription>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            inputStream?.close()
            val jsonString = stringBuilder.toString()
            val type = object : TypeToken<List<Subscription>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}