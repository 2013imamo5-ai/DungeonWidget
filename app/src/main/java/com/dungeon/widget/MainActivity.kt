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
import android.widget.Toast

class MainActivity:Activity() {
 private val handler=Handler(Looper.getMainLooper())
 private lateinit var title:TextView
 private lateinit var stats:TextView
 private lateinit var loot:TextView
 private lateinit var progress:TextView
 private lateinit var scene:ImageView
 private lateinit var pause:Button
 private lateinit var campButton:Button
 private lateinit var potionButton:Button
 private var frame=0
 private var lastStateRefresh=0L
 private val loop=object:Runnable {
  override fun run(){if(!isFinishing){redraw();handler.postDelayed(this,120)}}
 }
 private fun label(text:String,size:Float=16f):TextView=TextView(this).apply {
  this.text=text;textSize=size;setTextColor(Color.rgb(228,231,222))
  setPadding(12,10,12,10);gravity=Gravity.CENTER
 }
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=Color.rgb(15,21,30);window.navigationBarColor=Color.rgb(15,21,30)
  val root=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(17,23,32));setPadding(10,12,10,12)
  }
  val scroll=ScrollView(this).apply {isFillViewport=true}
  val body=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL}
  title=label("DUNGEON WIDGET",22f).apply {
   setTextColor(Color.rgb(237,202,130));setTypeface(null,Typeface.BOLD)
  }
  body.addView(title)
  scene=ImageView(this).apply {adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER}
  body.addView(scene,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,260))
  stats=label("",17f);body.addView(stats)
  loot=label("",15f);body.addView(loot)
  progress=label("",14f);body.addView(progress)
  pause=Button(this).apply {setOnClickListener {DungeonGame.toggle(this@MainActivity);redraw()}}
  body.addView(pause)
  campButton=Button(this).apply {
   setOnClickListener {
    if(!DungeonGame.upgradeCamp(this@MainActivity))Toast.makeText(this@MainActivity,"Недостаточно золота",Toast.LENGTH_SHORT).show()
    redraw()
   }
  };body.addView(campButton)
  potionButton=Button(this).apply {
   setOnClickListener {
    if(!DungeonGame.buyPotion(this@MainActivity))Toast.makeText(this@MainActivity,"Недостаточно золота",Toast.LENGTH_SHORT).show()
    redraw()
   }
  };body.addView(potionButton)
  body.addView(Button(this).apply {
   text="⬇ Проверить обновления";setOnClickListener{UpdateChecker.check(this@MainActivity,true)}
  })
  body.addView(label("Герой исследует подземелье самостоятельно.\nПри смерти возвращается в лагерь без потери снаряжения.",13f))
  scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,-1))
  setContentView(root);redraw();UpdateChecker.check(this)
 }
 override fun onResume(){super.onResume();handler.removeCallbacks(loop);handler.post(loop)}
 override fun onPause(){handler.removeCallbacks(loop);super.onPause()}
 private fun redraw(){
  val now=System.currentTimeMillis()
  val h=if(now-lastStateRefresh>=1500L){lastStateRefresh=now;DungeonGame.advance(this)} else DungeonGame.load(this)
  title.text="⚔ ${h.biome} · Этаж ${h.floor} · ${h.room}/8"
  stats.text="❤ ${h.hp}/${h.maxHp}    ★ ${h.level}    🪙 ${h.gold}\n${h.log}"
  loot.text="⚔ ${h.weaponName} +${h.weapon}\n🛡 ${h.armorName} +${h.armor}   🧪 ${h.potions}"
  progress.text="Опыт: ${h.xp}/${h.level*22}   💎 ${h.gems}   ⛺ Лагерь ${h.camp}\nПобед: ${h.kills}   Боссов: ${h.bosses}   Сундуков: ${h.chests}\nРеликвий: ${h.relics}   Рекорд: этаж ${h.bestFloor}   Смертей: ${h.deaths}"
  pause.text=if(h.paused)"▶ ПРОДОЛЖИТЬ" else "⏸ ПАУЗА"
  campButton.text="⛺ Улучшить лагерь (+6 HP) · ${80+h.camp*70} золота"
  potionButton.text="🧪 Купить зелье · ${25+h.floor*2} золота"
  scene.setImageBitmap(DungeonScene.render(h,frame++))
  if(now-lastStateRefresh<130L) DungeonWidgetProvider.refreshAll(this)
 }
}
