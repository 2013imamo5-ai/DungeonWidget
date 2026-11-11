package com.dungeon.widget

import android.content.Context
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

data class HeroState(
 var hp:Int=40,var level:Int=1,var xp:Int=0,var gold:Int=0,
 var floor:Int=1,var room:Int=1,var weapon:Int=0,var armor:Int=0,
 var potions:Int=3,var kills:Int=0,var paused:Boolean=false,
 var lastTick:Long=System.currentTimeMillis(),var log:String="Герой входит в склеп",
 var gems:Int=0,var camp:Int=0,var bestFloor:Int=1,var deaths:Int=0,
 var chests:Int=0,var bosses:Int=0,var relics:Int=0,var roomsCleared:Int=0,
 var enemy:String="Скелет",var event:String="explore",var combo:Int=0,
 var weaponName:String="Ржавый меч",var armorName:String="Кожаный доспех"
) {
 val maxHp:Int get()=40+(level-1)*8+camp*6
 val biome:String get()=when((floor-1)/5%4){0->"Забытый склеп";1->"Грибные пещеры";2->"Пепельная крепость";else->"Ледяные катакомбы"}
 fun asJson()=JSONObject().apply {
  put("hp",hp);put("level",level);put("xp",xp);put("gold",gold);put("floor",floor);put("room",room)
  put("weapon",weapon);put("armor",armor);put("potions",potions);put("kills",kills)
  put("paused",paused);put("lastTick",lastTick);put("log",log)
  put("gems",gems);put("camp",camp);put("bestFloor",bestFloor);put("deaths",deaths)
  put("chests",chests);put("bosses",bosses);put("relics",relics);put("roomsCleared",roomsCleared)
  put("enemy",enemy);put("event",event);put("combo",combo);put("weaponName",weaponName);put("armorName",armorName)
 }
 companion object {
  fun fromJson(raw:String):HeroState {
   val o=JSONObject(raw)
   return HeroState(
    o.optInt("hp",40),o.optInt("level",1),o.optInt("xp",0),o.optInt("gold",0),
    o.optInt("floor",1),o.optInt("room",1),o.optInt("weapon",0),o.optInt("armor",0),
    o.optInt("potions",3),o.optInt("kills",0),o.optBoolean("paused",false),
    o.optLong("lastTick",System.currentTimeMillis()),o.optString("log","Герой входит в склеп"),
    o.optInt("gems"),o.optInt("camp"),o.optInt("bestFloor",1),o.optInt("deaths"),
    o.optInt("chests"),o.optInt("bosses"),o.optInt("relics"),o.optInt("roomsCleared"),
    o.optString("enemy","Скелет"),o.optString("event","explore"),o.optInt("combo"),
    o.optString("weaponName","Ржавый меч"),o.optString("armorName","Кожаный доспех")
   )
  }
 }
}

object DungeonGame {
 private const val SAVE="dungeon_save"
 private const val STEP_MILLIS=12000L
 @Synchronized fun load(context:Context):HeroState {
  val raw=context.getSharedPreferences(SAVE,Context.MODE_PRIVATE).getString("hero",null)
  return try {if(raw==null) HeroState() else HeroState.fromJson(raw)}catch(_:Exception){HeroState()}
 }
 @Synchronized fun save(context:Context,h:HeroState){
  context.getSharedPreferences(SAVE,Context.MODE_PRIVATE).edit().putString("hero",h.asJson().toString()).commit()
 }
 @Synchronized fun advance(context:Context):HeroState {
  val h=load(context);val now=System.currentTimeMillis()
  if(!h.paused) {
   val count=((now-h.lastTick).coerceAtLeast(0)/STEP_MILLIS).coerceAtMost(800).toInt()
   repeat(count){step(h)}
   h.lastTick=if(count>=800) now else h.lastTick+count*STEP_MILLIS
  } else h.lastTick=now
  save(context,h);return h
 }
 @Synchronized fun toggle(context:Context):HeroState {
  val h=advance(context);h.paused=!h.paused;h.lastTick=System.currentTimeMillis()
  h.log=if(h.paused)"Герой остановился на привал" else "Поход продолжается"
  save(context,h);return h
 }
 @Synchronized fun upgradeCamp(context:Context):Boolean {
  val h=advance(context);val price=80+h.camp*70
  if(h.gold<price)return false
  h.gold-=price;h.camp++;h.hp=min(h.maxHp,h.hp+6)
  h.log="Лагерь улучшен до уровня ${h.camp} (+6 макс. HP)"
  save(context,h);return true
 }
 @Synchronized fun buyPotion(context:Context):Boolean {
  val h=advance(context);val price=25+h.floor*2
  if(h.gold<price)return false
  h.gold-=price;h.potions++;h.log="Куплено лечебное зелье"
  save(context,h);return true
 }
 private fun enemyFor(h:HeroState):String {
  val pool=when((h.floor-1)/5%4){
   0->listOf("Скелет","Крыса","Призрак","Рыцарь-нежить")
   1->listOf("Паук","Споровик","Грибной голем","Пещерный охотник")
   2->listOf("Культист","Огненный бес","Пепельный страж","Демон")
   else->listOf("Ледяной дух","Волк","Морозный страж","Лич")
  }
  return pool.random()
 }
 fun step(h:HeroState) {
  if(h.paused)return
  if(h.hp<max(10,h.maxHp/3)&&h.potions>0){
   h.potions--;h.hp=min(h.maxHp,h.hp+26+h.level*3);h.log="Герой выпил зелье здоровья";h.event="potion";return
  }
  val boss=h.room==8 && h.floor%5==0
  val roll=if(boss)0 else Random.nextInt(100)
  when {
   roll<55 -> {
    h.enemy=if(boss)"Страж этажа ${h.floor}" else enemyFor(h)
    h.event=if(boss)"boss" else "battle"
    val defense=(h.armor*2+h.camp/2)
    val incoming=(3+h.floor*2+Random.nextInt(8)+(if(boss)9 else 0)-defense).coerceAtLeast(1)
    h.hp-=incoming
    if(h.hp<=0){
     h.deaths++;h.hp=h.maxHp;h.floor=1;h.room=1;h.potions=max(2,h.potions)
     h.combo=0;h.event="camp";h.log="Герой пал! Возвращение в лагерь · снаряжение сохранено";return
    }
    h.kills++;h.combo++;val xpGain=6+h.floor*2+(if(boss)40 else 0)
    h.xp+=xpGain;h.gold+=3+h.floor+Random.nextInt(9)+(if(boss)80 else 0)
    if(boss){h.bosses++;h.gems+=2+Random.nextInt(3);h.log="Босс повержен! +${xpGain} опыта, кристаллы!"}
    else h.log="${h.enemy} повержен! +${xpGain} опыта"
   }
   roll<74 -> {
    h.event="chest";h.chests++;val reward=12+h.floor*4+Random.nextInt(22)
    h.gold+=reward;h.log="Сундук: +${reward} золота"
    if(Random.nextInt(100)<32){
     if(Random.nextBoolean()){
      h.weapon++;h.weaponName=listOf("Стальной клинок","Клинок теней","Рунный меч","Меч охотника").random()
      h.log="Найдено оружие: ${h.weaponName} +${h.weapon}"
     }else{
      h.armor++;h.armorName=listOf("Кольчуга","Пластинчатый доспех","Теневой панцирь","Рунная броня").random()
      h.log="Найдена броня: ${h.armorName} +${h.armor}"
     }
    }
   }
   roll<84 -> {h.event="potion";h.potions++;h.log="Найдено лечебное зелье"}
   roll<91 -> {
    h.event="shrine";h.hp=min(h.maxHp,h.hp+18+h.camp*2)
    if(Random.nextInt(4)==0){h.gems++;h.log="Древнее святилище: восстановлено HP и найден кристалл"}
    else h.log="Древнее святилище: восстановлено здоровье"
   }
   roll<96 -> {
    h.event="trap";val damage=(4+h.floor*2-h.armor).coerceAtLeast(1)
    h.hp-=damage
    if(h.hp<=0){
     h.deaths++;h.hp=h.maxHp;h.floor=1;h.room=1;h.event="camp"
     h.log="Ловушка! Герой возвращается в лагерь";return
    }
    h.log="Ловушка! Потеряно ${damage} HP"
   }
   else -> {
    h.event="relic";h.relics++;h.gems+=1+Random.nextInt(2)
    h.log="Найдена древняя реликвия и кристаллы"
   }
  }
  while(h.xp>=h.level*22){
   h.xp-=h.level*22;h.level++;h.hp=min(h.maxHp,h.hp+20)
   h.log="Новый уровень: ${h.level}!";h.event="level"
  }
  h.roomsCleared++;h.room++
  if(h.room>8){
   h.floor++;h.bestFloor=max(h.bestFloor,h.floor);h.room=1
   h.log="Спуск на этаж ${h.floor} · ${h.biome}"
   h.event="stairs"
  }
 }
}
