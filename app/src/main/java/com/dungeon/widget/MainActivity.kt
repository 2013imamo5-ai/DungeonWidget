package com.dungeon.widget

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.os.Handler
import android.os.Looper

class MainActivity:Activity() {
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var title:TextView
    private lateinit var stats:TextView
    private lateinit var loot:TextView
    private lateinit var scene:ImageView
    private lateinit var pause:Button
    private var frame=0
    private val loop=object:Runnable {
        override fun run() {
            if(!isFinishing) {
                redraw()
                handler.postDelayed(this,1500)
            }
        }
    }
    private fun label(text:String,size:Float=16f):TextView =
        TextView(this).apply {
            this.text=text;textSize=size;setTextColor(Color.rgb(228,231,222))
            setPadding(12,12,12,12);gravity=Gravity.CENTER
        }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(15,21,30)
        window.navigationBarColor=Color.rgb(15,21,30)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(17,23,32))
            setPadding(10,16,10,16)
        }
        title=label("DUNGEON WIDGET",23f).apply {
            setTextColor(Color.rgb(237,202,130));setTypeface(null,Typeface.BOLD)
        }
        root.addView(title)
        scene=ImageView(this).apply {
            adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER
        }
        root.addView(scene,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,2f))
        stats=label("",17f);root.addView(stats)
        loot=label("",15f);root.addView(loot)
        pause=Button(this).apply {
            setOnClickListener { DungeonGame.toggle(this@MainActivity);redraw() }
        }
        root.addView(pause)
        root.addView(label("Подземелье исследуется само. Прогресс сохраняется.\nДобавь Dungeon Widget через меню виджетов главного экрана.",13f))
        setContentView(root)
        redraw()
    }
    override fun onResume() {super.onResume();handler.removeCallbacks(loop);handler.post(loop)}
    override fun onPause() {handler.removeCallbacks(loop);super.onPause()}
    private fun redraw() {
        val h=DungeonGame.advance(this)
        title.text="⚔ DUNGEON · Этаж ${h.floor} · Комната ${h.room}"
        stats.text="❤ ${h.hp}/${h.maxHp}     ★ Уровень ${h.level}     🪙 ${h.gold}\n${h.log}"
        loot.text="⚔ Меч +${h.weapon}     🛡 Броня +${h.armor}     🧪 Зелья: ${h.potions}\nПобед: ${h.kills}     Опыт: ${h.xp}/${h.level*22}"
        pause.text=if(h.paused) "▶ ПРОДОЛЖИТЬ" else "⏸ ПАУЗА"
        scene.setImageBitmap(DungeonScene.render(h,frame++))
        DungeonWidgetProvider.refreshAll(this)
    }
}
