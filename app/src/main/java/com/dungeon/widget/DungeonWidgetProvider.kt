package com.dungeon.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

class DungeonWidgetProvider:AppWidgetProvider(){
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){
        val h=DungeonGame.advance(context)
        ids.forEach{update(context,manager,it,h)}
    }
    override fun onAppWidgetOptionsChanged(context:Context,manager:AppWidgetManager,id:Int,newOptions:Bundle){
        super.onAppWidgetOptionsChanged(context,manager,id,newOptions)
        update(context,manager,id,DungeonGame.advance(context))
    }
    override fun onReceive(context:Context,intent:Intent){
        super.onReceive(context,intent)
        when(intent.action){
            "com.dungeon.widget.ACTION_TOGGLE"->{
                DungeonGame.toggle(context);refreshAll(context)
            }
            "com.dungeon.widget.ACTION_POTION"->{
                DungeonGame.usePotion(context);refreshAll(context)
            }
            "com.dungeon.widget.ACTION_TICK"->refreshAll(context)
        }
    }
    companion object{
        fun refreshAll(context:Context){
            val manager=AppWidgetManager.getInstance(context)
            val ids=manager.getAppWidgetIds(ComponentName(context,DungeonWidgetProvider::class.java))
            if(ids.isEmpty())return
            val hero=DungeonGame.advance(context)
            ids.forEach{update(context,manager,it,hero)}
        }
        private fun update(context:Context,manager:AppWidgetManager,id:Int,h:HeroState){
            val views=RemoteViews(context.packageName,R.layout.widget_dungeon)
            val size=manager.getAppWidgetOptions(id)
            val height=size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,120)
            val width=size.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,180)
            val tiny=height<115||width<155
            val large=height>210
            val title="⚔ ПОДЗЕМЕЛЬЕ   F"+h.floor+" · "+h.room+"/8"
            views.setTextViewText(R.id.widgetTitle,title)
            views.setTextViewText(R.id.widgetStatus,"♥ "+h.hp+"/"+h.maxHp+"   ★ "+h.level+"   ◉ "+h.gold+"   🧪 "+h.potions)
            views.setTextViewText(R.id.widgetDetail,
                if(large) h.biome+"  ·  "+h.log else h.biome+"   ·   "+h.className)
            views.setViewVisibility(R.id.widgetDetail,if(tiny)View.GONE else View.VISIBLE)
            views.setTextViewText(R.id.widgetAction,if(h.paused)"▶ ПУСК" else "Ⅱ ПАУЗА")
            views.setTextViewText(R.id.widgetPotion,"✚ ЗЕЛЬЕ")
            views.setInt(R.id.widgetRoot,"setBackgroundColor",
                Color.argb(h.opacity*255/100,17,24,34))
            views.setImageViewBitmap(R.id.widgetScene,DungeonScene.render(h,0,false))
            fun pending(action:String,code:Int):PendingIntent{
                val intent=Intent(context,DungeonWidgetProvider::class.java).apply{this.action=action}
                return PendingIntent.getBroadcast(context,code,intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
            views.setOnClickPendingIntent(R.id.widgetAction,pending("com.dungeon.widget.ACTION_TOGGLE",11))
            views.setOnClickPendingIntent(R.id.widgetPotion,pending("com.dungeon.widget.ACTION_POTION",12))
            val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetScene,open)
            views.setOnClickPendingIntent(R.id.widgetTitle,open)
            views.setOnClickPendingIntent(R.id.widgetStatus,open)
            manager.updateAppWidget(id,views)
        }
    }
}
