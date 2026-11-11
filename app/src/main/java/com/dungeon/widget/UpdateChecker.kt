package com.dungeon.widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val RELEASE_API = "https://api.github.com/repos/2013imamo5-ai/DungeonWidget/releases/latest"
    fun check(activity: Activity, showCurrent: Boolean = false) {
        Thread {
            try {
                val connection = URL(RELEASE_API).openConnection() as HttpURLConnection
                connection.connectTimeout = 7000
                connection.readTimeout = 7000
                connection.setRequestProperty("Accept","application/vnd.github+json")
                connection.setRequestProperty("User-Agent","DungeonWidget-Android")
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val json = JSONObject(response)
                val latest = json.getString("tag_name").removePrefix("v")
                val current = activity.packageManager.getPackageInfo(activity.packageName,0).versionName ?: "0.0.0"
                val newer = compareVersions(latest,current)>0
                val assets=json.optJSONArray("assets")
                var apkUrl:String?=null
                if(assets!=null) for(i in 0 until assets.length()) {
                    val asset=assets.getJSONObject(i)
                    if(asset.optString("name").endsWith(".apk")) {
                        apkUrl=asset.optString("browser_download_url")
                        break
                    }
                }
                val download=apkUrl
                Handler(Looper.getMainLooper()).post {
                    if(activity.isFinishing || activity.isDestroyed) return@post
                    if(newer && download!=null) {
                        AlertDialog.Builder(activity)
                            .setTitle("Доступно обновление $latest")
                            .setMessage("Текущая версия $current. Скачать новый APK? Установка поверх сохранит прогресс при совпадении подписи.")
                            .setPositiveButton("Скачать") { _,_ ->
                                activity.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(download)))
                            }
                            .setNegativeButton("Позже",null).show()
                    } else if(showCurrent) {
                        AlertDialog.Builder(activity).setMessage(
                            if(newer) "Найдена версия $latest, но APK отсутствует."
                            else "Установлена актуальная версия $current."
                        ).setPositiveButton("ОК",null).show()
                    }
                }
            } catch (_: Exception) {
                if(showCurrent) Handler(Looper.getMainLooper()).post {
                    if(!activity.isFinishing) AlertDialog.Builder(activity)
                        .setMessage("Не удалось проверить обновления. Проверь подключение к интернету.")
                        .setPositiveButton("ОК",null).show()
                }
            }
        }.start()
    }
    private fun compareVersions(a:String,b:String):Int {
        val aa=a.split(".").map { it.toIntOrNull() ?: 0 }
        val bb=b.split(".").map { it.toIntOrNull() ?: 0 }
        for(i in 0 until maxOf(aa.size,bb.size)) {
            val delta=(aa.getOrElse(i){0}).compareTo(bb.getOrElse(i){0})
            if(delta!=0) return delta
        }
        return 0
    }
}
