package com.dungeon.widget

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Direct, signed self-updates from the project's official GitHub Releases.
 * Uses any available network (Wi-Fi or cellular), including background
 * periodic checks. Android decides whether installer confirmation is needed.
 */
object UpdateChecker {
    private const val RELEASE_API =
        "https://api.github.com/repos/2013imamo5-ai/DungeonWidget/releases/latest"
    private const val ASSET_ROOT =
        "https://github.com/2013imamo5-ai/DungeonWidget/releases/download/"
    private const val PREFS="dungeon_auto_updates"
    private const val PERIOD=12L*60*60*1000
    private const val MIN_CHECK=4L*60*60*1000
    private const val MAX_APK=80L*1024*1024
    private const val JOB_ID=169420
    private val main=Handler(Looper.getMainLooper())
    private val checking=AtomicBoolean(false)
    private val installing=AtomicBoolean(false)
    private var permissionDialogOpen=false
    private var foreground:WeakReference<Activity>?=null
    private var systemConfirmation:Intent?=null

    private fun prefs(ctx:Context)=
        ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)

    fun enabled(ctx:Context)=prefs(ctx).getBoolean("enabled",true)
    fun status(ctx:Context):String{
        val p=prefs(ctx)
        val pending=p.getString("pending_version",null)
        val result=p.getString("last_status",null)
        return when{
            pending!=null && p.getBoolean("retry_confirmation",false)->
                "Обновление $pending ожидает подтверждения Android"
            pending!=null->"Обновление $pending скачано"
            result!=null->result
            else->"Автообновление включено"
        }
    }
    fun hasPending(ctx:Context)=
        prefs(ctx).getString("pending_version",null)!=null
    fun retryPending(activity:Activity){
        continuePending(activity,true)
    }
    fun setEnabled(ctx:Context,value:Boolean){
        prefs(ctx).edit().putBoolean("enabled",value).apply()
        schedule(ctx)
    }

    /** Registered jobs use ANY network; no Wi-Fi-only constraint. */
    fun schedule(context:Context){
        val ctx=context.applicationContext
        val scheduler=ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE)
            as? JobScheduler ?:return
        if(!enabled(ctx)){
            scheduler.cancel(JOB_ID)
            return
        }
        try{
            val job=JobInfo.Builder(
                JOB_ID,ComponentName(ctx,UpdateJobService::class.java)
            ).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(PERIOD,2L*60*60*1000)
                .build()
            scheduler.schedule(job)
        }catch(_:Exception){
            // Battery or OEM restrictions can delay periodic jobs. Launch-time
            // checks remain available even when scheduling is unavailable.
        }
    }

    fun onResume(activity:Activity){
        foreground=WeakReference(activity)
        val confirmation=systemConfirmation
        if(confirmation!=null){
            systemConfirmation=null
            launchConfirmation(activity,confirmation)
            return
        }
        continuePending(activity,false)
    }

    fun onPause(activity:Activity){
        if(foreground?.get()===activity)foreground=null
    }

    fun check(activity:Activity,showCurrent:Boolean=false){
        if(!showCurrent&&!enabled(activity))return
        doCheck(activity.applicationContext,activity,showCurrent){}
    }

    fun checkBackground(ctx:Context,done:()->Unit){
        if(!enabled(ctx)){
            done()
            return
        }
        doCheck(ctx.applicationContext,null,false,done)
    }

    private fun doCheck(ctx:Context,activity:Activity?,manual:Boolean,done:()->Unit){
        val p=prefs(ctx)
        if(!manual && System.currentTimeMillis()-
            p.getLong("last_check",0L)<MIN_CHECK){
            done();return
        }
        if(!checking.compareAndSet(false,true)){
            done();return
        }
        Thread{
            try{
                val connection=URL(RELEASE_API).openConnection() as HttpURLConnection
                val body=try{
                    connection.connectTimeout=10000
                    connection.readTimeout=12000
                    connection.setRequestProperty("Accept","application/vnd.github+json")
                    connection.setRequestProperty("User-Agent","DungeonWidget-Android")
                    if(connection.responseCode!=200)
                        throw IllegalStateException("GitHub API: "+connection.responseCode)
                    connection.inputStream.bufferedReader().use{it.readText()}
                }finally{connection.disconnect()}
                val json=JSONObject(body)
                val latest=json.getString("tag_name").removePrefix("v")
                val current=ctx.packageManager.getPackageInfo(ctx.packageName,0)
                    .versionName?:"0.0.0"
                p.edit().putLong("last_check",System.currentTimeMillis()).apply()
                if(compareVersions(latest,current)<=0){
                    if(manual)message(activity,"Установлена актуальная версия $current")
                    return@Thread
                }
                val assets=json.optJSONArray("assets")
                    ?:throw IllegalStateException("Выпуск не содержит APK")
                val expectedName="DungeonWidget-v$latest.apk"
                var url:String?=null
                var digest:String?=null
                for(i in 0 until assets.length()){
                    val a=assets.getJSONObject(i)
                    if(a.optString("name")==expectedName){
                        url=a.optString("browser_download_url")
                        digest=a.optString("digest")
                        if(a.optLong("size",0)>MAX_APK)
                            throw IllegalStateException("APK слишком большой")
                        break
                    }
                }
                val assetUrl=url?:throw IllegalStateException("APK не опубликован")
                if(!assetUrl.startsWith(ASSET_ROOT+"v$latest/"))
                    throw SecurityException("Неизвестный адрес обновления")
                val apk=File(ctx.cacheDir,"DungeonWidget-v$latest.apk")
                if(!verified(ctx,apk)){
                    apk.delete()
                    download(assetUrl,apk)
                }
                if(!verified(ctx,apk)){
                    apk.delete()
                    throw SecurityException("APK не совпадает с подписью игры")
                }
                if(!digest.isNullOrBlank() && digest.startsWith("sha256:")){
                    if(!digest.substringAfter("sha256:").equals(sha256(apk),true)){
                        apk.delete()
                        throw SecurityException("Контрольная сумма APK не совпала")
                    }
                }
                p.edit().putString("pending_version",latest)
                    .putString("last_status","Обновление $latest скачано")
                    .apply()
                message(activity,"Обновление $latest скачано. Подготавливаем установку.")
                if(activity!=null){
                    main.post{
                        if(!activity.isFinishing && !activity.isDestroyed)
                            continuePending(activity,true)
                    }
                }
                // In background, attempt a silent system-approved self-update
                // only after the app has been trusted to install packages.
                // If Android needs user interaction, its callback will defer
                // confirmation until the game is opened again.
                if(activity==null && ctx.packageManager.canRequestPackageInstalls()
                    && !p.getBoolean("retry_confirmation",false)
                    && installing.compareAndSet(false,true)){
                    try{
                        install(ctx,apk,latest,false)
                    }catch(_:Exception){
                        installing.set(false)
                        p.edit().putBoolean("retry_confirmation",true)
                            .putString("last_status",
                                "Открой игру для подтверждения установки")
                            .apply()
                    }
                }
            }catch(e:Exception){
                if(manual)message(activity,
                    "Не удалось обновиться: "+(e.message?:"ошибка сети"))
            }finally{
                checking.set(false)
                done()
            }
        }.start()
    }

    private fun download(url:String,destination:File){
        val tmp=File(destination.parentFile,"dungeon-update.tmp")
        tmp.delete()
        val c=URL(url).openConnection() as HttpURLConnection
        try{
            c.instanceFollowRedirects=true
            c.connectTimeout=12000
            c.readTimeout=25000
            c.setRequestProperty("User-Agent","DungeonWidget-Android")
            if(c.responseCode !in 200..299)
                throw IllegalStateException("HTTP "+c.responseCode)
            if(c.contentLengthLong>MAX_APK)
                throw IllegalStateException("Файл превышает допустимый размер")
            var total=0L
            c.inputStream.use{source->
                tmp.outputStream().use{target->
                    val buffer=ByteArray(32*1024)
                    while(true){
                        val n=source.read(buffer)
                        if(n<0)break
                        total+=n
                        if(total>MAX_APK)
                            throw IllegalStateException("Слишком большой APK")
                        target.write(buffer,0,n)
                    }
                }
            }
            if(total<10_000)
                throw IllegalStateException("Скачан некорректный файл")
            if(!tmp.renameTo(destination)){
                tmp.copyTo(destination,overwrite=true)
                tmp.delete()
            }
        }finally{
            c.disconnect()
            tmp.delete()
        }
    }

    private fun verified(ctx:Context,apk:File):Boolean{
        if(!apk.isFile||apk.length()<10_000L)return false
        return try{
            val pm=ctx.packageManager
            val flag=PackageManager.GET_SIGNING_CERTIFICATES
            val old=pm.getPackageInfo(ctx.packageName,flag)
            val next=pm.getPackageArchiveInfo(apk.absolutePath,flag)?:return false
            if(next.packageName!=ctx.packageName ||
                next.longVersionCode<=old.longVersionCode)return false
            val oldSign=old.signingInfo?.apkContentsSigners?:return false
            val nextSign=next.signingInfo?.apkContentsSigners?:return false
            oldSign.isNotEmpty() && oldSign.size==nextSign.size &&
                oldSign.toSet()==nextSign.toSet()
        }catch(_:Exception){false}
    }

    private fun sha256(file:File):String{
        val hash=MessageDigest.getInstance("SHA-256")
        file.inputStream().use{stream->
            val buffer=ByteArray(32*1024)
            while(true){
                val n=stream.read(buffer)
                if(n<0)break
                hash.update(buffer,0,n)
            }
        }
        return hash.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
    }

    private fun continuePending(activity:Activity,showPermission:Boolean){
        if(activity.isFinishing||activity.isDestroyed||installing.get())return
        val ctx=activity.applicationContext
        val p=prefs(ctx)
        if(!showPermission && p.getBoolean("manual_retry_required",false))return
        val version=p.getString("pending_version",null)?:return
        val apk=File(ctx.cacheDir,"DungeonWidget-v$version.apk")
        if(!verified(ctx,apk)){
            p.edit().remove("pending_version")
                .remove("retry_confirmation")
                .remove("manual_retry_required").apply()
            apk.delete()
            return
        }
        if(!activity.packageManager.canRequestPackageInstalls()){
            if(showPermission && !permissionDialogOpen){
                permissionDialogOpen=true
                AlertDialog.Builder(activity)
                    .setTitle("Разрешить обновления DungeonWidget")
                    .setMessage(
                        "APK уже безопасно скачан из нашего GitHub. "+
                        "Один раз разреши установку из Dungeon Widget. "+
                        "После этого игра сможет устанавливать новые версии "+
                        "на Wi-Fi и мобильном интернете без браузера. "+
                        "Android может дополнительно попросить подтверждение."
                    )
                    .setPositiveButton("РАЗРЕШИТЬ"){_,_->
                        try{
                            activity.startActivity(Intent(
                                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:"+activity.packageName)
                            ))
                        }catch(_:Exception){
                            message(activity,"Не удалось открыть настройки разрешений")
                        }
                    }
                    .setNegativeButton("Позже",null)
                    .setOnDismissListener{permissionDialogOpen=false}
                    .show()
            }
            return
        }
        if(!installing.compareAndSet(false,true))return
        // Keep the verified APK and its version until Android confirms success.
        // After an aborted attempt force a normal system-confirmed retry.
        val requireConfirmation=p.getBoolean("retry_confirmation",false)
        p.edit().putBoolean("manual_retry_required",false)
            .putString("last_status","Установка $version запущена")
            .apply()
        Thread{
            try{install(ctx,apk,version,requireConfirmation)}
            catch(e:Exception){
                installing.set(false)
                p.edit().putBoolean("retry_confirmation",true)
                    .putBoolean("manual_retry_required",true)
                    .putString("last_status","Не удалось установить $version")
                    .apply()
                message(activity,"Ошибка установки: "+(e.message?:"неизвестно"))
            }
        }.start()
    }

    private fun install(ctx:Context,apk:File,version:String,
        requireConfirmation:Boolean){
        val manager=ctx.packageManager.packageInstaller
        val params=PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(ctx.packageName)
        params.setSize(apk.length())
        if(Build.VERSION.SDK_INT>=31){
            params.setRequireUserAction(
                if(requireConfirmation)
                    PackageInstaller.SessionParams.USER_ACTION_REQUIRED
                else PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id=manager.createSession(params)
        try{
            manager.openSession(id).use{session->
                session.openWrite("base.apk",0,apk.length()).use{stream->
                    apk.inputStream().use{it.copyTo(stream)}
                    session.fsync(stream)
                }
                val intent=Intent(ctx,UpdateInstallReceiver::class.java).apply{
                    action="com.dungeon.widget.INSTALL_RESULT"
                    putExtra("target_version",version)
                    putExtra("forced_confirmation",requireConfirmation)
                }
                val callback=PendingIntent.getBroadcast(
                    ctx,id,intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                session.commit(callback.intentSender)
            }
        }catch(e:Exception){
            try{manager.abandonSession(id)}catch(_:Exception){}
            throw e
        }
    }

    fun onInstallResult(ctx:Context,intent:Intent){
        val p=prefs(ctx)
        val version=intent.getStringExtra("target_version")?:""
        val wasConfirmed=intent.getBooleanExtra("forced_confirmation",false)
        val status=intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,PackageInstaller.STATUS_FAILURE)
        when(status){
            PackageInstaller.STATUS_PENDING_USER_ACTION->{
                val confirmation=intent.getParcelableExtra(
                    Intent.EXTRA_INTENT) as? Intent
                // Keep installing=true while the confirmation UI is open.
                // Otherwise onResume could submit the same APK a second time.
                p.edit().putString("last_status",
                    "Android ожидает подтверждения установки").apply()
                if(confirmation!=null){
                    val activity=foreground?.get()
                    if(activity!=null&&!activity.isFinishing){
                        main.post{launchConfirmation(activity,confirmation)}
                    }else{
                        systemConfirmation=confirmation
                    }
                }else{
                    installing.set(false)
                    p.edit().putBoolean("retry_confirmation",true)
                        .putBoolean("manual_retry_required",true)
                        .apply()
                }
            }
            PackageInstaller.STATUS_SUCCESS->{
                installing.set(false)
                systemConfirmation=null
                p.edit().remove("pending_version")
                    .remove("retry_confirmation")
                    .remove("manual_retry_required")
                    .putString("last_status","Версия $version установлена")
                    .apply()
                File(ctx.cacheDir,"DungeonWidget-v$version.apk").delete()
            }
            else->{
                installing.set(false)
                val reason=intent.getStringExtra(
                    PackageInstaller.EXTRA_STATUS_MESSAGE)?:"ошибка Android"
                val activity=foreground?.get()
                val aborted=status==PackageInstaller.STATUS_FAILURE_ABORTED
                val canRetry=aborted && !wasConfirmed && version.isNotEmpty()
                p.edit().putBoolean("retry_confirmation",true)
                    .putBoolean("manual_retry_required",!canRetry)
                    .putString("last_status",
                        if(canRetry)"Повторная установка с подтверждением Android"
                        else "Ошибка установки: "+reason.take(110))
                    .apply()
                if(canRetry && activity!=null && !activity.isFinishing){
                    message(activity,
                        "Android прервал установку. Повторяем с подтверждением.")
                    main.post{
                        if(!activity.isFinishing && !activity.isDestroyed)
                            continuePending(activity,true)
                    }
                }else if(canRetry){
                    // Background installers cannot reliably display UI. Keep
                    // the downloaded APK and ask for confirmation on launch.
                    p.edit().putBoolean("manual_retry_required",false).apply()
                }else{
                    message(activity,"Установка не завершена: "+reason)
                }
            }
        }
    }

    private fun launchConfirmation(activity:Activity,intent:Intent){
        try{
            activity.startActivity(intent)
        }catch(_:Exception){
            try{
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.applicationContext.startActivity(intent)
            }catch(_:Exception){
                systemConfirmation=intent
                message(activity,"Открой игру для подтверждения установки")
            }
        }
    }

    private fun message(activity:Activity?,text:String){
        if(activity==null)return
        main.post{
            if(!activity.isFinishing&&!activity.isDestroyed)
                Toast.makeText(activity,text,Toast.LENGTH_LONG).show()
        }
    }
    private fun compareVersions(a:String,b:String):Int{
        val aa=a.split(".").map{it.toIntOrNull()?:0}
        val bb=b.split(".").map{it.toIntOrNull()?:0}
        for(i in 0 until maxOf(aa.size,bb.size)){
            val cmp=aa.getOrElse(i){0}.compareTo(bb.getOrElse(i){0})
            if(cmp!=0)return cmp
        }
        return 0
    }
}

class UpdateInstallReceiver:android.content.BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        UpdateChecker.onInstallResult(context,intent)
    }
}
