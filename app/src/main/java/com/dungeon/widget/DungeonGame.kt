package com.dungeon.widget

import android.content.Context
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.max
import kotlin.random.Random

data class HeroState(
    var hp: Int = 40,
    var level: Int = 1,
    var xp: Int = 0,
    var gold: Int = 0,
    var floor: Int = 1,
    var room: Int = 1,
    var weapon: Int = 0,
    var armor: Int = 0,
    var potions: Int = 3,
    var kills: Int = 0,
    var paused: Boolean = false,
    var lastTick: Long = System.currentTimeMillis(),
    var log: String = "Герой входит в склеп"
) {
    val maxHp: Int get() = 40 + (level - 1) * 8
    fun asJson() = JSONObject().apply {
        put("hp",hp);put("level",level);put("xp",xp);put("gold",gold)
        put("floor",floor);put("room",room);put("weapon",weapon)
        put("armor",armor);put("potions",potions);put("kills",kills)
        put("paused",paused);put("lastTick",lastTick);put("log",log)
    }
    companion object {
        fun fromJson(raw: String): HeroState {
            val o = JSONObject(raw)
            return HeroState(
                o.optInt("hp",40),o.optInt("level",1),o.optInt("xp",0),
                o.optInt("gold",0),o.optInt("floor",1),o.optInt("room",1),
                o.optInt("weapon",0),o.optInt("armor",0),o.optInt("potions",3),
                o.optInt("kills",0),o.optBoolean("paused",false),
                o.optLong("lastTick",System.currentTimeMillis()),
                o.optString("log","Герой входит в склеп")
            )
        }
    }
}

object DungeonGame {
    private const val SAVE = "dungeon_save"
    private const val STEP_MILLIS = 12000L
    fun load(context: Context): HeroState {
        val raw = context.getSharedPreferences(SAVE,Context.MODE_PRIVATE).getString("hero",null)
        return try { if(raw == null) HeroState() else HeroState.fromJson(raw) }
        catch (_: Exception) { HeroState() }
    }
    fun save(context: Context,hero: HeroState) {
        context.getSharedPreferences(SAVE,Context.MODE_PRIVATE).edit()
            .putString("hero",hero.asJson().toString()).apply()
    }
    fun advance(context: Context): HeroState {
        val h = load(context)
        val now = System.currentTimeMillis()
        if(!h.paused) {
            val count = ((now - h.lastTick).coerceAtLeast(0)/STEP_MILLIS).coerceAtMost(800).toInt()
            repeat(count) { step(h) }
            h.lastTick = if(count >= 800) now else h.lastTick + count * STEP_MILLIS
        } else { h.lastTick = now }
        save(context,h)
        return h
    }
    fun toggle(context: Context): HeroState {
        val h = advance(context)
        h.paused = !h.paused
        h.lastTick = System.currentTimeMillis()
        h.log = if(h.paused) "Герой отдыхает" else "Поход продолжается"
        save(context,h)
        return h
    }
    fun step(h: HeroState) {
        if(h.paused) return
        if(h.hp < max(8,h.maxHp / 3) && h.potions > 0) {
            h.potions--
            h.hp = min(h.maxHp,h.hp + 23 + h.level*2)
            h.log = "Герой выпил зелье здоровья"
            return
        }
        val roll = Random.nextInt(100)
        when {
            roll < 58 -> {
                val enemy = if(h.floor >= 3 && Random.nextBoolean()) "Призрак" else if(Random.nextBoolean()) "Скелет" else "Крыса"
                val attack = 8 + h.level*3 + h.weapon*3 + Random.nextInt(6)
                val incoming = (3 + h.floor*2 + Random.nextInt(9) - h.armor*2).coerceAtLeast(1)
                h.hp -= incoming
                if(h.hp <= 0) {
                    h.gold = (h.gold * 0.75).toInt()
                    h.hp = h.maxHp
                    h.floor = 1;h.room = 1;h.potions += 2
                    h.log = "Герой пал! Возрождение в лагере"
                    return
                }
                h.kills++
                h.xp += 5 + h.floor * 2
                h.gold += 2 + Random.nextInt(7) + h.floor
                h.log = "$enemy повержен! ⚔ Урон $attack"
            }
            roll < 78 -> {
                val reward = 8 + h.floor*4 + Random.nextInt(20)
                h.gold += reward
                h.log = "Сундук! +$reward золота"
                if(Random.nextInt(100)<30) {
                    if(Random.nextBoolean()) {
                        h.weapon++;h.log="Найден меч +${h.weapon}!"
                    } else {
                        h.armor++;h.log="Найдена броня +${h.armor}!"
                    }
                }
            }
            roll < 91 -> {
                h.potions++
                h.log = "Найдено лечебное зелье"
            }
            else -> {
                h.hp = min(h.maxHp,h.hp + 8)
                h.log = "Тихая комната. Небольшой отдых"
            }
        }
        if(h.xp >= h.level * 22) {
            h.xp -= h.level * 22
            h.level++
            h.hp = min(h.maxHp,h.hp + 15)
            h.log = "Новый уровень: ${h.level}!"
        }
        h.room++
        if(h.room > 8) {
            h.floor++
            h.room = 1
            h.log = "Спуск на этаж ${h.floor}"
        }
    }
}
