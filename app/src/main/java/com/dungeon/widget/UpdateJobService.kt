package com.dungeon.widget

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * Android schedules these checks using any connected network, including
 * mobile data. Work can be deferred by battery saver or OEM restrictions.
 */
class UpdateJobService:JobService(){
    override fun onStartJob(params:JobParameters):Boolean{
        UpdateChecker.checkBackground(applicationContext){
            jobFinished(params,false)
        }
        return true
    }
    override fun onStopJob(params:JobParameters):Boolean{
        // Existing thread exits naturally; next periodic check will re-run.
        return false
    }
}
