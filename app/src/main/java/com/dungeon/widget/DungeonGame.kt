package com.dungeon.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class Gear(
    val id:String, val name:String, val slot:String, val rarity:Int, val power:Int,
    val trait:String="", val set:Int=0
) {
    fun json()=JSONObject().apply {
        put("id",id);put("name",name);put("slot",slot);put("rarity",rarity)
        put("power",power);put("trait",trait);put("set",set)
    }
    companion object {
        fun read(o:JSONObject)=Gear(o.optString("id"),o.optString("name"),
            o.optString("slot"),o.optInt("rarity"),o.optInt("power"),
            o.optString("trait"),o.optInt("set"))
    }
}
data class HeroState(
    var hp:Int=40,var level:Int=1,var xp:Int=0,var gold:Int=0,
    var floor:Int=1,var room:Int=1,var weapon:Int=0,var armor:Int=0,
    var potions:Int=3,var kills:Int=0,var paused:Boolean=false,
    var lastTick:Long=System.currentTimeMillis(),var log:String="Герой входит в склеп",
    var gems:Int=0,var camp:Int=0,var bestFloor:Int=1,var deaths:Int=0,
    var chests:Int=0,var bosses:Int=0,var relics:Int=0,var roomsCleared:Int=0,
    var enemy:String="Скелет",var event:String="explore",var combo:Int=0,
    var weaponName:String="Ржавый меч",var armorName:String="Кожаный доспех",
    var pathStep:Int=0,var roomKind:String="battle",var heroClass:Int=0,
    var skillAttack:Int=0,var skillDefense:Int=0,var skillLuck:Int=0,
    var shards:Int=0,var forge:Int=0,var healer:Int=0,var scouts:Int=0,
    var lastDamage:Int=0,var cooldown:Int=0,var status:String="",
    var enemyHp:Int=0,var enemyMaxHp:Int=0,var battleRound:Int=0,
    var incomingDamage:Int=0,var lastCritical:Boolean=false,var lastDodge:Boolean=false,
    var abilityMeter:Int=0,var lastAbility:String="",var tactic:Int=0,
    var elite:Boolean=false,var elitesDefeated:Int=0,var bossPhase:Int=0,
    var attackVisual:String="slash",var enemyWarning:Boolean=false,
    var effectType:String="",var effectTicks:Int=0,
    var recovery:Int=0,var autoPotion:Int=35,var autoSalvage:Boolean=false,
    var autoEquip:Boolean=false,var autoSalvageRarity:Int=1,
    var theme:Int=0,var opacity:Int=92,var eco:Boolean=false,
    var questDay:String="",var questCount:Int=0,var questClaimed:Boolean=false,
    var offlineReport:String="",var lastReportAt:Long=0,
    // One compact, persistent 20x11 fog mask for the active room.
    var exploreRoom:String="",var exploreSeen:String="",var exploreVisited:String="",
    var exploreCompleted:Int=0,var activeEvent:Int=-1,
    var exploreX:Int=1,var exploreY:Int=5,
    var explorePreviousX:Int=1,var explorePreviousY:Int=5,
    var exploreFacing:Int=2,var exploreAction:String="scout",
    val gear:MutableList<Gear> = mutableListOf(),
    val equipment:MutableMap<String,String> = mutableMapOf(),
    val discovered:MutableSet<String> = mutableSetOf(),
    val achievements:MutableSet<String> = mutableSetOf(),
    val favorites:MutableSet<String> = mutableSetOf(),
    val newItems:MutableSet<String> = mutableSetOf(),
    val lootHistory:MutableList<String> = mutableListOf()
){
    val maxHp:Int get()=40+(level-1)*8+camp*6+healer*10+(if(heroClass==0)10 else 0)+equipmentStats.health
    val equipmentStats:EquipmentStats get()=EquipmentLogic.stats(this)
    val inventoryLimit:Int get()=60
    val biomeId:Int get()=((floor-1)/5)%4
    val biome:String get()=listOf("Забытый склеп","Грибные пещеры","Пепельная крепость","Ледяные катакомбы")[biomeId]
    val className:String get()=listOf("Воин","Следопыт","Маг")[heroClass.coerceIn(0,2)]
    val weaponItem:Gear? get()=gear.firstOrNull{it.id==equipment["weapon"]}
    val armorItem:Gear? get()=gear.firstOrNull{it.id==equipment["armor"]}
    val attack:Int get()=8+level*3+weapon*2+forge*2+skillAttack*3+
        equipmentStats.attack+(if(heroClass==2)level*2 else 0)
    val defense:Int get()=armor*2+camp/2+skillDefense*2+
        equipmentStats.defense+(if(heroClass==0)4 else 0)
    val setBonus:Int get()=equipmentStats.setBonus
    val dodge:Int get()=4+(if(heroClass==1)10 else 0)+skillLuck*2+equipmentStats.dodge
    val crit:Int get()=6+skillLuck*3+(if(heroClass==1)7 else 0)+equipmentStats.crit
    val progress:Int get()=pathStep.coerceAtLeast(0)
    fun asJson()=JSONObject().apply {
        val ints=mapOf("hp" to hp,"level" to level,"xp" to xp,"gold" to gold,
            "floor" to floor,"room" to room,"weapon" to weapon,"armor" to armor,
            "potions" to potions,"kills" to kills,"gems" to gems,"camp" to camp,
            "bestFloor" to bestFloor,"deaths" to deaths,"chests" to chests,
            "bosses" to bosses,"relics" to relics,"roomsCleared" to roomsCleared,
            "combo" to combo,"pathStep" to pathStep,"heroClass" to heroClass,
            "skillAttack" to skillAttack,"skillDefense" to skillDefense,
            "skillLuck" to skillLuck,"shards" to shards,"forge" to forge,
            "healer" to healer,"scouts" to scouts,"lastDamage" to lastDamage,
            "cooldown" to cooldown,"recovery" to recovery,"autoPotion" to autoPotion,
            "theme" to theme,"opacity" to opacity,"questCount" to questCount,
            "enemyHp" to enemyHp,"enemyMaxHp" to enemyMaxHp,
            "battleRound" to battleRound,"incomingDamage" to incomingDamage,
            "abilityMeter" to abilityMeter,"tactic" to tactic,
            "elitesDefeated" to elitesDefeated,"bossPhase" to bossPhase,
            "exploreCompleted" to exploreCompleted,"activeEvent" to activeEvent,
            "exploreX" to exploreX,"exploreY" to exploreY,
            "explorePreviousX" to explorePreviousX,"explorePreviousY" to explorePreviousY,
            "exploreFacing" to exploreFacing)
        ints.forEach{(k,v)->put(k,v)}
        put("paused",paused);put("lastTick",lastTick);put("log",log)
        put("enemy",enemy);put("event",event);put("weaponName",weaponName)
        put("armorName",armorName);put("roomKind",roomKind);put("status",status)
        put("autoSalvage",autoSalvage);put("eco",eco);put("questDay",questDay)
        put("questClaimed",questClaimed);put("offlineReport",offlineReport)
        put("lastReportAt",lastReportAt);put("effectType",effectType);put("effectTicks",effectTicks)
        put("attackVisual",attackVisual);put("enemyWarning",enemyWarning)
        put("lastAbility",lastAbility);put("elite",elite)
        put("exploreRoom",exploreRoom);put("exploreSeen",exploreSeen)
        put("exploreVisited",exploreVisited);put("exploreAction",exploreAction)
        put("lastCritical",lastCritical);put("lastDodge",lastDodge)
        put("autoEquip",autoEquip);put("autoSalvageRarity",autoSalvageRarity)
        put("favorites",JSONArray(favorites.toList()))
        put("newItems",JSONArray(newItems.toList()))
        put("lootHistory",JSONArray(lootHistory.toList()))
        put("gear",JSONArray().apply{gear.forEach{put(it.json())}})
        put("equipment",JSONObject().apply{equipment.forEach{(k,v)->put(k,v)}})
        put("discovered",JSONArray(discovered.toList()))
        put("achievements",JSONArray(achievements.toList()))
    }
    companion object{
        fun fromJson(raw:String):HeroState {
            val o=JSONObject(raw);val h=HeroState()
            h.hp=o.optInt("hp",40);h.level=o.optInt("level",1);h.xp=o.optInt("xp")
            h.gold=o.optInt("gold");h.floor=o.optInt("floor",1);h.room=o.optInt("room",1)
            h.weapon=o.optInt("weapon");h.armor=o.optInt("armor")
            h.potions=o.optInt("potions",3);h.kills=o.optInt("kills")
            h.paused=o.optBoolean("paused");h.lastTick=o.optLong("lastTick",System.currentTimeMillis())
            h.log=o.optString("log","Герой входит в склеп")
            h.gems=o.optInt("gems");h.camp=o.optInt("camp");h.bestFloor=o.optInt("bestFloor",1)
            h.deaths=o.optInt("deaths");h.chests=o.optInt("chests");h.bosses=o.optInt("bosses")
            h.relics=o.optInt("relics");h.roomsCleared=o.optInt("roomsCleared")
            h.enemy=o.optString("enemy","Скелет");h.event=o.optString("event","explore")
            h.combo=o.optInt("combo");h.weaponName=o.optString("weaponName","Ржавый меч")
            h.armorName=o.optString("armorName","Кожаный доспех")
            h.pathStep=o.optInt("pathStep").coerceIn(0,9999)
            h.roomKind=o.optString("roomKind","battle")
            h.heroClass=o.optInt("heroClass").coerceIn(0,2)
            h.skillAttack=o.optInt("skillAttack");h.skillDefense=o.optInt("skillDefense")
            h.skillLuck=o.optInt("skillLuck");h.shards=o.optInt("shards")
            h.forge=o.optInt("forge");h.healer=o.optInt("healer");h.scouts=o.optInt("scouts")
            h.lastDamage=o.optInt("lastDamage");h.cooldown=o.optInt("cooldown")
            h.enemyHp=o.optInt("enemyHp");h.enemyMaxHp=o.optInt("enemyMaxHp")
            h.battleRound=o.optInt("battleRound");h.incomingDamage=o.optInt("incomingDamage")
            h.abilityMeter=o.optInt("abilityMeter").coerceIn(0,4)
            h.tactic=o.optInt("tactic").coerceIn(0,2)
            h.lastAbility=o.optString("lastAbility","")
            h.elite=o.optBoolean("elite")
            h.elitesDefeated=o.optInt("elitesDefeated")
            h.bossPhase=o.optInt("bossPhase")
            h.exploreRoom=o.optString("exploreRoom","")
            h.exploreSeen=o.optString("exploreSeen","")
            h.exploreVisited=o.optString("exploreVisited","")
            h.exploreCompleted=o.optInt("exploreCompleted")
            h.activeEvent=o.optInt("activeEvent",-1)
            h.exploreX=o.optInt("exploreX",1).coerceIn(0,19)
            h.exploreY=o.optInt("exploreY",5).coerceIn(0,10)
            h.explorePreviousX=o.optInt("explorePreviousX",h.exploreX).coerceIn(0,19)
            h.explorePreviousY=o.optInt("explorePreviousY",h.exploreY).coerceIn(0,10)
            h.exploreFacing=o.optInt("exploreFacing",2).coerceIn(0,3)
            h.exploreAction=o.optString("exploreAction","scout")
            h.attackVisual=o.optString("attackVisual","slash")
            h.enemyWarning=o.optBoolean("enemyWarning")
            h.lastCritical=o.optBoolean("lastCritical");h.lastDodge=o.optBoolean("lastDodge")
            h.status=o.optString("status");h.effectType=o.optString("effectType")
            h.effectTicks=o.optInt("effectTicks");h.recovery=o.optInt("recovery")
            h.autoPotion=o.optInt("autoPotion",35).coerceIn(10,90)
            h.autoSalvage=o.optBoolean("autoSalvage")
            h.autoEquip=o.optBoolean("autoEquip")
            h.autoSalvageRarity=o.optInt("autoSalvageRarity",1).coerceIn(0,2)
            h.theme=o.optInt("theme").coerceIn(0,2)
            h.opacity=o.optInt("opacity",92).coerceIn(20,100);h.eco=o.optBoolean("eco")
            h.questDay=o.optString("questDay");h.questCount=o.optInt("questCount")
            h.questClaimed=o.optBoolean("questClaimed")
            h.offlineReport=o.optString("offlineReport");h.lastReportAt=o.optLong("lastReportAt")
            val arr=o.optJSONArray("gear")
            if(arr!=null)for(i in 0 until arr.length())runCatching{
                h.gear.add(Gear.read(arr.getJSONObject(i)))
            }
            val equipped=o.optJSONObject("equipment")
            if(equipped!=null)for(key in equipped.keys())h.equipment[key]=equipped.optString(key)
            val starred=o.optJSONArray("favorites")
            if(starred!=null)for(i in 0 until starred.length())h.favorites.add(starred.optString(i))
            val unread=o.optJSONArray("newItems")
            if(unread!=null)for(i in 0 until unread.length())h.newItems.add(unread.optString(i))
            val entries=o.optJSONArray("lootHistory")
            if(entries!=null)for(i in 0 until entries.length())
                h.lootHistory.add(entries.optString(i))
            val seen=o.optJSONArray("discovered")
            if(seen!=null)for(i in 0 until seen.length())h.discovered.add(seen.optString(i))
            val medals=o.optJSONArray("achievements")
            if(medals!=null)for(i in 0 until medals.length())h.achievements.add(medals.optString(i))
            return h
        }
    }
}

object DungeonGame{
    private const val SAVE="dungeon_save"
    private const val STEP=1050L
    private const val ROOM_STEPS=19
    private val enemyPools=listOf(
        listOf("Скелет","Крыса","Призрак","Рыцарь-нежить"),
        listOf("Паук","Споровик","Грибной голем","Пещерный охотник"),
        listOf("Культист","Огненный бес","Пепельный страж","Демон"),
        listOf("Ледяной дух","Волк","Морозный страж","Лич"))
    private val rarities=listOf("Обычный","Необычный","Редкий","Эпический","Легендарный")
    fun rarityName(i:Int)=rarities[i.coerceIn(0,4)]
    fun enemyName(biome:Int,variant:Int):String{
        val pool=enemyPools[biome.coerceIn(0,3)]
        return pool[(variant and Int.MAX_VALUE)%pool.size]
    }
    fun stepMillis()=STEP
    @Synchronized fun load(ctx:Context):HeroState=try{
        val raw=ctx.getSharedPreferences(SAVE,Context.MODE_PRIVATE).getString("hero",null)
        if(raw==null)HeroState() else HeroState.fromJson(raw)
    }catch(_:Exception){HeroState()}
    @Synchronized fun save(ctx:Context,h:HeroState){
        ctx.getSharedPreferences(SAVE,Context.MODE_PRIVATE).edit().putString("hero",h.asJson().toString()).apply()
    }
    private fun today()=LocalDate.now().toString()
    private fun quest(h:HeroState){
        if(h.questDay!=today()){h.questDay=today();h.questCount=0;h.questClaimed=false}
    }
    @Synchronized fun advance(ctx:Context):HeroState{
        val h=load(ctx);val now=System.currentTimeMillis();quest(h)
        val layout=DungeonMap.get(h.floor,h.room)
        if(h.exploreRoom!=DungeonExploration.key(h)){
            // v0.9 saves keep all items and levels; only the current room is
            // reset as it adopts the newly generated v1.0 maze.
            DungeonExploration.prepare(h,layout)
            save(ctx,h)
        }
        if(!h.paused){
            val elapsed=(now-h.lastTick).coerceAtLeast(0)
            val count=(elapsed/STEP).coerceAtMost(82000L).toInt()
            if(count>0){
                val wasGold=h.gold;val wasKills=h.kills;val wasRooms=h.roomsCleared
                repeat(count){step(h)}
                h.lastTick=if(elapsed/STEP>82000)now else h.lastTick+count*STEP
                if(count>=90 && now-h.lastReportAt>120000L){
                    val minutes=count*STEP/60000
                    h.offlineReport="За "+minutes+" мин.: +" +(h.gold-wasGold)+
                        " золота, "+(h.kills-wasKills)+" побед, "+(h.roomsCleared-wasRooms)+" комнат"
                    h.lastReportAt=now
                }
                save(ctx,h)
            }
        }else if(now-h.lastTick>STEP){h.lastTick=now;save(ctx,h)}
        return h
    }
    @Synchronized fun mutate(ctx:Context,action:(HeroState)->Boolean):Boolean{
        val h=advance(ctx);val okay=action(h)
        if(okay)save(ctx,h)
        return okay
    }
    fun toggle(ctx:Context)=mutate(ctx){h->
        h.paused=!h.paused;h.lastTick=System.currentTimeMillis()
        h.log=if(h.paused)"Отряд остановился" else "Исследование продолжается";true
    }
    fun usePotion(ctx:Context)=mutate(ctx){h->
        if(h.potions<=0||h.hp>=h.maxHp)false else {
            h.potions--;h.hp=min(h.maxHp,h.hp+30+h.level*4)
            h.event="potion";h.log="Герой использовал лечебное зелье";true
        }
    }
    fun buyPotion(ctx:Context)=mutate(ctx){h->
        val price=25+h.floor*2
        if(h.gold<price)false else{h.gold-=price;h.potions++;h.log="Куплено зелье";true}
    }
    fun upgrade(ctx:Context,which:String)=mutate(ctx){h->
        val lv=when(which){"camp"->h.camp;"forge"->h.forge;"healer"->h.healer;else->h.scouts}
        val cost=80+lv*85
        if(h.gold<cost||lv>=25)false else{
            h.gold-=cost
            when(which){"camp"->{h.camp++;h.hp+=6}
                "forge"->h.forge++
                "healer"->{h.healer++;h.hp+=10}
                else->h.scouts++}
            h.log="Улучшение лагеря: "+which+" ур. "+(lv+1);true
        }
    }
    fun chooseClass(ctx:Context,kind:Int)=mutate(ctx){h->
        if(kind !in 0..2)false else{h.heroClass=kind;h.hp=min(h.hp,h.maxHp);h.log="Класс: "+h.className;true}
    }
    fun skillPoints(h:HeroState)=h.level/2-h.skillAttack-h.skillDefense-h.skillLuck
    fun setTactic(ctx:Context,tactic:Int)=mutate(ctx){h->
        if(tactic !in 0..2)false else{
            h.tactic=tactic
            h.log="Тактика: "+listOf("Сбалансированная","Осторожная","Агрессивная")[tactic]
            true
        }
    }
    fun train(ctx:Context,which:Int)=mutate(ctx){h->
        if(skillPoints(h)<=0)false else{
            when(which){0->h.skillAttack++;1->h.skillDefense++;else->h.skillLuck++}
            h.log="Развит новый навык";true
        }
    }
    fun claimQuest(ctx:Context)=mutate(ctx){h->
        quest(h)
        if(h.questCount<10||h.questClaimed)false else{
            h.questClaimed=true;h.gold+=100;h.gems+=2
            h.log="Награда за задание: 100 золота и 2 кристалла";true
        }
    }
    fun equip(ctx:Context,id:String,secondRing:Boolean=false)=mutate(ctx){h->
        val item=h.gear.firstOrNull{it.id==id}
        if(item==null)false else{
            EquipmentLogic.equip(h,item,secondRing)
            h.newItems.remove(id)
            h.log="Экипировано: "+item.name
            true
        }
    }
    fun toggleFavorite(ctx:Context,id:String)=mutate(ctx){h->
        if(h.gear.none{it.id==id})false else{
            if(!h.favorites.add(id))h.favorites.remove(id)
            h.log=if(id in h.favorites)"Предмет защищён" else "Защита предмета снята"
            true
        }
    }
    fun viewItem(ctx:Context,id:String)=mutate(ctx){h->h.newItems.remove(id)}
    fun markAllViewed(ctx:Context)=mutate(ctx){h->
        if(h.newItems.isEmpty())false else{h.newItems.clear();true}
    }
    fun equipBest(ctx:Context,role:Int)=mutate(ctx){h->
        if(role !in 0..2)false else{
            h.heroClass=role
            val count=EquipmentLogic.best(h,role)
            h.hp=h.hp.coerceAtMost(h.maxHp)
            h.log="Подобран комплект для "+h.className+": "+count+" изменений"
            true
        }
    }
    fun salvage(ctx:Context,id:String,force:Boolean=false)=mutate(ctx){h->
        val item=h.gear.firstOrNull{it.id==id}
        if(item==null||h.equipment.values.contains(id)||
            (!force&&!EquipmentLogic.canSalvage(h,item)))false else{
            h.gear.remove(item);h.favorites.remove(id);h.newItems.remove(id)
            h.shards+=1+item.rarity*2
            h.log="Разобрано: "+item.name+" · материалы +"+(1+item.rarity*2)
            true
        }
    }
    fun salvageWeak(ctx:Context,maxRarity:Int)=mutate(ctx){h->
        val unwanted=h.gear.filter{
            it.rarity<=maxRarity && EquipmentLogic.canSalvage(h,it)
        }
        if(unwanted.isEmpty())false else{
            val removed=unwanted.map{it.id}.toSet()
            h.gear.removeAll{it.id in removed}
            h.newItems.removeAll(removed)
            h.shards+=unwanted.sumOf{1+it.rarity*2}
            h.log="Разобрано "+unwanted.size+" предметов. Материалы: +"+
                unwanted.sumOf{1+it.rarity*2}
            true
        }
    }
    fun salvageDuplicates(ctx:Context,includeLegendary:Boolean=false)=mutate(ctx){h->
        val dupes=EquipmentLogic.duplicateCandidates(h,includeLegendary)
        if(dupes.isEmpty())false else{
            val ids=dupes.map{it.id}.toHashSet()
            h.gear.removeAll{it.id in ids}
            // Safety: equipped / manually favorited / unreviewed are never candidates.
            h.shards+=dupes.sumOf{1+it.rarity*2}
            h.log="Разобраны дубликаты: "+dupes.size+" предметов · +"+
                dupes.sumOf{1+it.rarity*2}+" материалов"
            true
        }
    }
    fun salvageFiltered(ctx:Context,rarity:Int,slot:String="all")=mutate(ctx){h->
        val selected=h.gear.filter{
            it.rarity<=rarity && (slot=="all"||it.slot==slot) &&
                EquipmentLogic.canSalvage(h,it)
        }
        if(selected.isEmpty())false else{
            val ids=selected.map{it.id}.toSet()
            h.gear.removeAll{it.id in ids}
            h.newItems.removeAll(ids)
            h.shards+=selected.sumOf{1+it.rarity*2}
            h.log="Разобрано "+selected.size+" предметов"
            true
        }
    }
    fun improveWeapon(ctx:Context)=mutate(ctx){h->
        val item=h.weaponItem
        val cost=20+h.level*3
        val materials=4+h.forge
        if(item==null||h.gold<cost||h.shards<materials)false else{
            h.gold-=cost;h.shards-=materials
            val idx=h.gear.indexOfFirst{it.id==item.id}
            h.gear[idx]=item.copy(power=item.power+2+h.forge)
            h.log="Оружие усилено на кузнице";true
        }
    }
    fun forgeItem(ctx:Context)=mutate(ctx){h->
        val cost=30+h.forge*12;val mats=5+h.forge*2
        if(h.gold<cost||h.shards<mats)false else{
            h.gold-=cost;h.shards-=mats
            collect(h,createGear(h,true))
            h.log="Кузница создала новое снаряжение";true
        }
    }
    fun merchantOffer(h:HeroState,index:Int):Gear{
        val names=listOf("Клинок хранителя","Доспех пилигрима","Кольцо рассвета")
        val slots=listOf("weapon","armor","ring")
        val traits=listOf("крит","защита","регенерация")
        val rarity=if(h.bestFloor>=10)2 else 1
        return Gear("shop-"+today()+"-"+index,names[index.coerceIn(0,2)],
            slots[index.coerceIn(0,2)],rarity,3+h.floor/3+index*2,
            traits[index.coerceIn(0,2)],if(index==2)1 else 0)
    }
    fun buyOffer(ctx:Context,index:Int)=mutate(ctx){h->
        if(index !in 0..2)return@mutate false
        val cost=70+h.floor*3+index*25
        if(h.gold<cost)false else{
            h.gold-=cost
            val offer=merchantOffer(h,index)
            collect(h,offer.copy(id=System.nanoTime().toString()))
            h.log="Куплено: "+offer.name;true
        }
    }
    fun buyFromMerchant(ctx:Context)=mutate(ctx){h->
        val cost=70+h.floor*3
        if(h.gold<cost)false else{
            h.gold-=cost;collect(h,createGear(h,false))
            h.log="Торговец передал предмет";true
        }
    }
    fun changeSettings(ctx:Context,potion:Int?=null,auto:Boolean?=null,theme:Int?=null,
        opacity:Int?=null,eco:Boolean?=null,autoGear:Boolean?=null,
        autoRarity:Int?=null)=mutate(ctx){h->
        potion?.let{h.autoPotion=it.coerceIn(10,90)}
        auto?.let{h.autoSalvage=it}
        autoGear?.let{h.autoEquip=it}
        autoRarity?.let{h.autoSalvageRarity=it.coerceIn(0,2)}
        theme?.let{h.theme=it.coerceIn(0,2)}
        opacity?.let{h.opacity=it.coerceIn(20,100)}
        eco?.let{h.eco=it}
        true
    }
    private fun createGear(h:HeroState,crafted:Boolean=false):Gear{
        val slot=listOf("weapon","armor","helmet","gloves","boots","ring","amulet").random()
        val quality=Random.nextInt(100)+(if(crafted)12+h.forge*2 else 0)+h.scouts
        val rarity=when{quality>=105->4;quality>=93->3;quality>=76->2;quality>=46->1;else->0}
        val names=when(slot){
            "weapon"->listOf("Клинок","Топор","Лук","Посох")
            "armor"->listOf("Доспех","Мантия","Кольчуга","Панцирь")
            "helmet"->listOf("Шлем","Капюшон","Корона","Забрало")
            "gloves"->listOf("Перчатки","Латные рукавицы","Наручи","Рукавицы")
            "boots"->listOf("Сапоги","Ботинки","Поножи","Сандалии")
            "ring"->listOf("Кольцо","Перстень","Печать","Обруч")
            else->listOf("Амулет","Оберег","Талисман","Реликварий")
        }
        val prefix=listOf("Забытый","Ночной","Огненный","Рунный","Древний","Ледяной")
        val name=if(rarity==4)when(slot){
            "weapon"->"Гибель королей";"armor"->"Панцирь вечной ночи"
            "ring"->"Печать звёзд";"helmet"->"Корона праха"
            "gloves"->"Руки короля";"boots"->"Поступь тьмы"
            else->"Сердце бездны"
        }else prefix.random()+" "+names.random()
        val trait=when(Random.nextInt(7)){
            0->"крит";1->"яд";2->"регенерация";3->"уклонение"
            4->"защита";5->"здоровье";else->"атака"
        }
        return Gear(System.nanoTime().toString()+"-"+Random.nextInt(10000),
            name,slot,rarity,(1+h.floor/3+rarity*3+Random.nextInt(3)).coerceAtMost(180),trait,
            if(Random.nextInt(4)==0)Random.nextInt(1,4) else 0)
    }
    private fun recordLoot(h:HeroState,description:String){
        h.lootHistory.add(0,description)
        if(h.lootHistory.size>16)h.lootHistory.subList(16,h.lootHistory.size).clear()
    }
    private fun collect(h:HeroState,item:Gear){
        val safeToScrap=item.rarity<3 && item.set==0
        // Never discard a potentially useful item just because it is low rarity:
        // compare against equipped equipment first.
        val current=h.gear.firstOrNull{it.id==h.equipment[item.slot]}
        val upgrade=current==null || EquipmentLogic.weight(item,h.heroClass)>
            EquipmentLogic.weight(current,h.heroClass)+4
        if(h.autoSalvage && item.rarity<=h.autoSalvageRarity &&
            safeToScrap && !upgrade){
            h.shards+=1+item.rarity*2
            recordLoot(h,"⚒ Разобрано: "+item.name)
            return
        }
        if(!EquipmentLogic.makeInventoryRoom(h,item,h.inventoryLimit)){
            h.shards+=1+item.rarity*2
            recordLoot(h,"⚠ Рюкзак полон · разобрано: "+item.name)
            return
        }
        h.gear.add(item)
        h.newItems.add(item.id)
        var worn=false
        if(h.equipment[item.slot]==null &&
            !(item.slot=="ring" && h.equipment["ring2"]!=null)){
            EquipmentLogic.equip(h,item)
            worn=true
        }else{
            worn=EquipmentLogic.autoEquip(h,item)
        }
        recordLoot(h,(if(worn)"✓ Надето: " else "✦ Найдено: ")+item.name+
            " · "+rarityName(item.rarity))
    }
    private fun gainXp(h:HeroState,amount:Int){
        h.xp+=amount
        while(h.xp>=h.level*22){
            h.xp-=h.level*22;h.level++;h.hp=min(h.maxHp,h.hp+22)
            h.log="Новый уровень: "+h.level+"!";h.event="level"
        }
    }
    private fun death(h:HeroState){
        h.deaths++;h.hp=h.maxHp;h.floor=1;h.room=1
        h.pathStep=0;h.combo=0;h.recovery=4;h.status="";h.effectTicks=0;h.effectType=""
        h.enemyHp=0;h.enemyMaxHp=0;h.battleRound=0;h.enemyWarning=false
        h.elite=false;h.bossPhase=0;h.lastAbility=""
        h.exploreRoom="";h.activeEvent=-1;h.exploreSeen="";h.exploreVisited=""
        h.potions=max(2,h.potions);h.event="camp"
        h.log="Герой пал! Возвращение в лагерь. Экипировка сохранена"
    }
    private fun checkMedals(h:HeroState){
        val candidates=mapOf("Первый бой" to(h.kills>=1),"Охотник" to(h.kills>=100),
            "Истребитель" to(h.kills>=1000),"Исследователь" to(h.roomsCleared>=100),
            "Покоритель глубин" to(h.bestFloor>=20),"Боссоборец" to(h.bosses>=5),
            "Коллекционер" to(h.relics>=10),"Богач" to(h.gold>=1000))
        for((name,earned) in candidates)if(earned&&h.achievements.add(name))h.gems++
    }
    private fun roomRoll(h:HeroState):String{
        if(h.room==8&&h.floor%10==0)return "boss"
        if(DungeonMap.get(h.floor,h.room).secret!=null&&Random.nextInt(100)<25)return "secret"
        val roll=Random.nextInt(100)
        return when{
            roll<54->"battle";roll<72->"chest";roll<80->"potion"
            roll<87->"shrine";roll<93->"trap";roll<97->"merchant";else->"relic"
        }
    }
    private fun weaponStyle(h:HeroState):String{
        val name=(h.weaponItem?.name?:h.weaponName).lowercase()
        return when{
            "топор" in name ->"smash"
            "лук" in name ->"arrow"
            "посох" in name||"жезл" in name||h.heroClass==2 ->"magic"
            else->"slash"
        }
    }
    private fun initializeEnemy(h:HeroState,forcedElite:Boolean=false,variant:Int=0){
        val boss=h.roomKind=="boss"
        val enemyName=if(boss){
            listOf("Костяной король","Древний страж","Пепельный титан","Ледяной лич")[h.biomeId]
        }else enemyName(h.biomeId,variant)
        h.elite=!boss && (forcedElite||Random.nextInt(100)<12)
        h.enemy=if(h.elite)"Элитный "+enemyName else enemyName
        h.enemyMaxHp=if(boss)max(120+h.floor*18,h.attack*6+h.floor*5)
            else if(h.elite)max(70+h.floor*9,h.attack*4+h.floor*3)
            else max(24+h.floor*5,h.attack*2+h.floor*2)
        h.enemyHp=h.enemyMaxHp
        h.battleRound=0;h.bossPhase=0;h.enemyWarning=false
        h.incomingDamage=0;h.lastDamage=0;h.lastAbility=""
        h.event="explore"
    }
    // Three automatic abilities, one per class; no manual inputs required.
    // Tactics alter risk but do not stop autonomous exploration.
    private fun combat(h:HeroState,boss:Boolean){
        if(h.enemyHp<=0)return
        h.discovered.add(h.enemy)
        h.event=if(boss)"boss" else "battle"
        val crit=Random.nextInt(100)<h.crit.coerceIn(0,65)
        val dodge=Random.nextInt(100)<h.dodge.coerceIn(0,55)
        val powered=h.cooldown<=0
        val special=h.abilityMeter>=4
        h.lastAbility=""
        h.attackVisual=weaponStyle(h)
        var damage=h.attack+Random.nextInt(max(3,h.attack/5))
        if(crit)damage+=h.attack/2
        if(powered)damage+=10+h.skillAttack*2
        if(h.attackVisual=="magic")damage+=h.level
        if(h.attackVisual=="smash")damage+=h.attack/5
        val guard="страж" in h.enemy.lowercase()||
            "рыцарь" in h.enemy.lowercase()||"голем" in h.enemy.lowercase()
        if(guard)damage=(damage*80/100).coerceAtLeast(1)
        val effects=h.equipmentStats
        damage+=effects.poison
        if(special){
            h.lastAbility=when(h.heroClass){
                0->"Щитовой удар"
                1->"Тройной выстрел"
                else->"Магический всплеск"
            }
            damage+=when(h.heroClass){
                0->h.attack/2+h.defense/4
                1->h.attack
                else->h.attack+h.level*2
            }
            h.abilityMeter=0
            if(h.heroClass==2)h.hp=min(h.maxHp,h.hp+max(2,h.maxHp/15))
        }else h.abilityMeter=(h.abilityMeter+1).coerceAtMost(4)
        if(h.tactic==1)damage=damage*88/100
        if(h.tactic==2)damage=damage*128/100
        damage=damage.coerceAtLeast(1)
        h.lastDamage=damage
        h.lastCritical=crit
        h.lastDodge=dodge
        h.enemyHp=(h.enemyHp-damage).coerceAtLeast(0)
        h.battleRound++
        h.cooldown=if(powered)4 else h.cooldown-1
        // Phase two is announced BEFORE the empowered blow on the next turn.
        val phaseTransition=boss && h.enemyHp>0 &&
            h.bossPhase==0 && h.enemyHp*2<=h.enemyMaxHp
        val heavyPending=h.enemyWarning
        if(phaseTransition)h.bossPhase=1
        if(h.enemyHp>0){
            val heavy=heavyPending
            val raw=(6+h.floor*3+(if(boss)18 else if(h.elite)12 else 0)+
                Random.nextInt(12)-h.defense/2).coerceAtLeast(1)
            var taken=if(dodge)0 else if(heavy)raw*17/10 else raw
            if(boss && h.bossPhase==1)taken=taken*125/100
            if(h.tactic==1)taken=taken*65/100
            if(h.tactic==2)taken=taken*125/100
            if(special && h.heroClass==0)taken=taken*30/100
            h.incomingDamage=taken
            h.hp-=taken
            if(h.hp<=0){death(h);return}
            if(!dodge && Random.nextInt(100)<(if(boss)30 else if(h.elite)17 else 10)){
                h.effectType=when(h.biomeId){
                    0->if(Random.nextBoolean())"bleed" else "stun"
                    1->"poison";2->"burn";else->"stun"
                }
                h.effectTicks=if(h.effectType=="stun")1 else 3
            }
            h.enemyWarning=phaseTransition ||
                (boss && h.battleRound%(if(h.bossPhase>0)2 else 3)==1) ||
                (!boss && h.battleRound%4==2)
            h.status=when{
                special->h.lastAbility
                boss&&h.bossPhase==1->"Босс в ярости!"
                h.enemyWarning->"Враг готовит мощную атаку!"
                crit->"Критический удар"
                dodge->"Уклонение"
                powered->"Сильный удар"
                else->"В разгаре боя"
            }
            h.log=(if(h.elite)"★ " else "")+h.enemy+": "+
                h.enemyHp+"/"+h.enemyMaxHp+" HP · -"+damage+
                (if(dodge)" · Уклонение!" else " · Получено "+taken)+
                (if(special)" · "+h.lastAbility else "")+
                (if(h.enemyWarning)" · ОПАСНО!" else "")
            return
        }
        h.enemyWarning=false;h.incomingDamage=0
        h.kills++;h.combo++;h.questCount++
        val xp=6+h.floor*2+(if(boss)45 else if(h.elite)28 else 0)
        gainXp(h,xp)
        h.gold+=5+h.floor+Random.nextInt(10)+
            (if(boss)120 else if(h.elite)55+h.floor else 0)
        if(effects.regeneration>0||h.combo%6==0)
            h.hp=min(h.maxHp,h.hp+3+h.level+effects.regeneration)
        if(boss){h.bosses++;h.gems+=3;collect(h,createGear(h,true))}
        if(h.elite){h.elitesDefeated++;collect(h,createGear(h,true))}
        if(Random.nextInt(100)<7+h.scouts)collect(h,createGear(h))
        h.status=if(special)h.lastAbility else if(crit)"Критический удар" else "Победа"
        h.log=when{
            boss->"БОСС повержен: "+h.enemy+"! +3 кристалла"
            h.elite->"Элитный враг повержен! Редкая добыча."
            else->h.enemy+" побеждён · "+damage+" урона"
        }+(if(special)" · "+h.lastAbility else "")
    }
    private fun resolve(h:HeroState){
        h.status=""
        when(h.roomKind){
            "battle","boss"->combat(h,h.roomKind=="boss")
            "secret"->{
                h.relics++;h.gems+=2;h.gold+=45+h.floor*3
                collect(h,createGear(h,true));h.event="relic"
                h.log="Тайная комната! Найдены сокровища и реликвия"
            }
            "chest"->{
                h.chests++;h.gold+=15+h.floor*5+Random.nextInt(24)
                if(Random.nextInt(100)<48+h.scouts)collect(h,createGear(h))
                h.event="chest";h.log="Открыт сундук с сокровищами"
            }
            "potion"->{h.potions++;h.event="potion";h.log="Найдено лечебное зелье"}
            "shrine"->{
                h.hp=min(h.maxHp,h.hp+24+h.healer*4);h.event="shrine"
                if(Random.nextInt(4)==0)h.gems++
                h.log="Святилище восстановило здоровье"
            }
            "trap"->{
                val damage=(5+h.floor*2-h.defense/2).coerceAtLeast(1)
                if(Random.nextInt(100)<h.dodge){h.log="Ловушка обойдена"}
                else{h.hp-=damage;h.log="Ловушка: -"+damage+" HP"}
                h.event="trap"
                if(h.hp<=0)death(h)
            }
            "merchant"->{
                h.event="merchant";h.gold+=5+h.scouts*2
                h.log="Встречен странствующий торговец"
            }
            else->{
                h.relics++;h.gems+=1+Random.nextInt(2)
                h.event="relic";h.log="Найдена древняя реликвия"
            }
        }
        checkMedals(h)
    }
    private fun completeRoom(h:HeroState){
        h.pathStep=0;h.room++;h.roomsCleared++
        h.activeEvent=-1;h.enemyHp=0;h.enemyMaxHp=0
        h.exploreRoom=""
        if(h.room>8){
            h.room=1;h.floor++
            h.bestFloor=max(h.bestFloor,h.floor)
            h.event="stairs";h.log="Спуск на этаж "+h.floor+" · "+h.biome
        }else{
            h.event="stairs";h.log="Найдена лестница! Переход в комнату "+h.room
        }
        checkMedals(h)
        // Initialize the next room at its start before the next render.
        val msg=h.log
        DungeonExploration.prepare(h,DungeonMap.get(h.floor,h.room))
        h.log=msg;h.event="stairs"
    }
    private fun roomEncounter(h:HeroState,layout:RoomLayout,e:WorldEvent){
        val fights=e.kind=="enemy"||e.kind=="elite"||e.kind=="boss"
        if(fights){
            if(h.activeEvent!=e.id){
                h.roomKind=if(e.kind=="boss")"boss" else "battle"
                h.activeEvent=e.id
                initializeEnemy(h,e.kind=="elite",layout.seed+e.id)
                h.exploreAction="combat"
                h.log="Обнаружен "+h.enemy+"!"
            }
            combat(h,e.kind=="boss")
            // Death resets the room key; never mark a dead hero's event as done.
            if(h.exploreRoom!=DungeonExploration.key(h))return
            if(h.enemyHp<=0){
                DungeonExploration.complete(h,e)
                h.activeEvent=-1
                h.exploreAction="victory"
            }
            return
        }
        h.activeEvent=-1
        h.roomKind=when(e.kind){
            "cache","breakable"->"chest"
            else->e.kind
        }
        h.exploreAction="loot"
        if(e.kind=="breakable"){
            val shards=1+Random.nextInt(2+h.floor/12)
            h.shards+=shards
            h.gold+=8+h.floor
            if(Random.nextInt(100)<35)collect(h,createGear(h))
            h.event="chest"
            h.log="Разбиты старые ящики · найдено "+shards+" материалов"
        }else if(e.kind=="cache"){
            h.gold+=10+h.floor*2
            collect(h,createGear(h,Random.nextInt(100)<30))
            h.event="chest"
            h.log="Найден тайник в боковом коридоре"
        }else resolve(h)
        if(h.exploreRoom!=DungeonExploration.key(h))return
        DungeonExploration.complete(h,e)
    }
    fun step(h:HeroState){
        if(h.paused)return
        if(h.recovery>0){
            h.recovery--;h.hp=min(h.maxHp,h.hp+h.maxHp/5)
            h.event="camp"
            if(h.recovery==0)h.log="Герой покидает лагерь"
            return
        }
        if(h.hp*100<=h.maxHp*h.autoPotion && h.potions>0){
            h.potions--;h.hp=min(h.maxHp,h.hp+30+h.level*4)
            h.event="potion";h.log="Автоматически использовано зелье"
            return
        }
        if(h.effectTicks>0){
            when(h.effectType){
                "stun"->{
                    h.effectTicks--;h.status="Оглушение"
                    h.log="Герой оглушён";return
                }
                "poison","bleed","burn"->{
                    if(h.pathStep%4==0){
                        val damage=(2+h.floor/3).coerceAtLeast(2)
                        h.hp-=damage;h.effectTicks--
                        h.status=when(h.effectType){
                            "poison"->"Отравление"
                            "bleed"->"Кровотечение"
                            else->"Горение"
                        }
                        if(h.hp<=0){death(h);return}
                    }
                }
            }
            if(h.effectTicks<=0)h.effectType=""
        }
        val room=DungeonMap.get(h.floor,h.room)
        DungeonExploration.prepare(h,room)
        DungeonExploration.reveal(h,room)
        val at= DungeonExploration.atEvent(h,room)
        if(at!=null){
            h.explorePreviousX=h.exploreX
            h.explorePreviousY=h.exploreY
            roomEncounter(h,room,at)
            return
        }
        val next=DungeonExploration.next(h,room)?:run{
            completeRoom(h);return
        }
        if(next.x==h.exploreX && next.y==h.exploreY){
            if(next==room.exit)completeRoom(h)
            return
        }
        val fromX=h.exploreX
        val fromY=h.exploreY
        h.explorePreviousX=fromX
        h.explorePreviousY=fromY
        h.exploreX=next.x
        h.exploreY=next.y
        h.exploreFacing=when{
            next.y<fromY->0
            next.x>fromX->1
            next.y>fromY->2
            else->3
        }
        h.pathStep=(h.pathStep+1).coerceAtMost(9999)
        h.event="explore"
        DungeonExploration.reveal(h,room)
        h.log=when(h.exploreAction){
            "hunt"->"Герой заметил движение впереди"
            "loot"->"Ищем сокровища в проходах"
            "exit"->"Обнаружена лестница к следующей комнате"
            else->"Разведка: "+DungeonExploration.exploredPercent(h,room)+"% карты"
        }
        if(next==room.exit && (DungeonExploration.allEventsComplete(h,room) ||
            h.pathStep>350))completeRoom(h)
    }
    @Synchronized fun importSave(ctx:Context,raw:String):Boolean {
        return try{
            val h=HeroState.fromJson(raw)
            if(h.level<1||h.floor<1||h.hp<0)false else{
                h.lastTick=System.currentTimeMillis();save(ctx,h);true
            }
        }catch(_:Exception){false}
    }
    @Synchronized fun exportSave(ctx:Context)=advance(ctx).asJson().toString(2)
}
