package com.dungeon.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class DungeonWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context,manager: AppWidgetManager,ids: IntArray) {
        val hero = DungeonGame.advance(context)
        ids.forEach { update(context,manager,it,hero) }
    }
    override fun onReceive(context: Context,intent: Intent) {
        super.onReceive(context,intent)
        if(intent.action=="com.dungeon.widget.ACTION_TOGGLE") DungeonGame.toggle(context)
        if(intent.action=="com.dungeon.widget.ACTION_TOGGLE" || intent.action=="com.dungeon.widget.ACTION_TICK") refreshAll(context)
    }
    companion object {
        fun refreshAll(context: Context) {
            val manager=AppWidgetManager.getInstance(context)
            val ids=manager.getAppWidgetIds(ComponentName(context,DungeonWidgetProvider::class.java))
            val hero=DungeonGame.advance(context)
            ids.forEach { update(context,manager,it,hero) }
        }
        private fun update(context: Context,manager: AppWidgetManager,id: Int,hero:HeroState) {
            val views=RemoteViews(context.packageName,R.layout.widget_dungeon)
            views.setTextViewText(R.id.widgetTitle,"⚔ DUNGEON · Этаж ${hero.floor}")
            views.setTextViewText(R.id.widgetStatus,"❤ ${hero.hp}/${hero.maxHp}  ★ ${hero.level}  🪙 ${hero.gold}  · ${hero.log}")
            views.setTextViewText(R.id.widgetAction,if(hero.paused) "▶ Продолжить" else "⏸ Пауза")
            views.setImageViewBitmap(R.id.widgetScene,DungeonScene.render(hero))
            val toggle=Intent(context,DungeonWidgetProvider::class.java).apply {action="com.dungeon.widget.ACTION_TOGGLE"}
            val pending=PendingIntent.getBroadcast(context,0,toggle,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetAction,pending)
            val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widgetScene,open)
            views.setOnClickPendingIntent(R.id.widgetTitle,open)
            manager.updateAppWidget(id,views)
        }
    }
}
