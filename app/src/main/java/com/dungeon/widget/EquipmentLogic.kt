package com.dungeon.widget

import kotlin.math.max

data class EquipmentStats(
    val attack:Int=0, val defense:Int=0, val health:Int=0,
    val crit:Int=0, val dodge:Int=0, val regeneration:Int=0,
    val poison:Int=0, val setBonus:Int=0
){
    operator fun minus(b:EquipmentStats)=EquipmentStats(
        attack-b.attack,defense-b.defense,health-b.health,crit-b.crit,
        dodge-b.dodge,regeneration-b.regeneration,poison-b.poison,
        setBonus-b.setBonus
    )
}

object EquipmentLogic {
    val slots=listOf("weapon","armor","helmet","gloves","boots","ring","ring2","amulet")
    fun label(slot:String)=when(slot){
        "weapon"->"Оружие";"armor"->"Броня";"helmet"->"Шлем"
        "gloves"->"Перчатки";"boots"->"Ботинки"
        "ring"->"Кольцо I";"ring2"->"Кольцо II"
        "amulet"->"Амулет";else->slot
    }
    fun icon(slot:String)=when(slot){
        "weapon"->"⚔";"armor"->"⛨";"helmet"->"♜"
        "gloves"->"✥";"boots"->"♣";"ring","ring2"->"◇"
        "amulet"->"✦";else->"◈"
    }
    fun equipped(h:HeroState, overrides:Map<String,String> = h.equipment):List<Gear>{
        val ids=overrides.values.toSet()
        return h.gear.filter{it.id in ids}
    }
    fun stats(h:HeroState, overrides:Map<String,String> = h.equipment):EquipmentStats {
        val items=equipped(h,overrides)
        var a=0;var d=0;var hp=0;var crit=0;var dodge=0;var regen=0;var poison=0
        items.forEach{g->
            when(g.slot){
                "weapon"->{a+=g.power}
                "armor"->{d+=g.power}
                "helmet"->{d+=g.power/2;hp+=g.power*2}
                "gloves"->{a+=g.power/2;d+=g.power/3}
                "boots"->{d+=g.power/2;dodge+=g.rarity+1}
                "ring"->{a+=g.power/3;d+=g.power/3}
                "amulet"->{a+=g.power/3;hp+=g.power*2}
            }
            when(g.trait){
                "крит"->crit+=3+g.rarity*2
                "уклонение"->dodge+=2+g.rarity
                "защита"->d+=3+g.rarity*2
                "регенерация"->regen+=2+g.rarity*2
                "яд"->poison+=2+g.rarity*2
                "здоровье"->hp+=10+g.rarity*10
                "атака"->a+=4+g.rarity*2
            }
        }
        val groups=items.filter{it.set>0}.groupingBy{it.set}.eachCount()
        val bonus=groups.values.fold(0){acc,count -> acc+(if(count>=4)12 else if(count>=2)4 else 0)}
        return EquipmentStats(a+bonus,d+bonus,hp+bonus*2,crit,dodge,regen,poison,bonus)
    }
    fun targetSlot(g:Gear,secondRing:Boolean=false)=if(g.slot=="ring"&&secondRing)"ring2" else g.slot
    fun preview(h:HeroState,g:Gear,secondRing:Boolean=false):EquipmentStats{
        val target=targetSlot(g,secondRing)
        val changed=h.equipment.toMutableMap()
        // A ring must never occupy both ring slots simultaneously.
        if(g.slot=="ring"){
            if(target=="ring") changed.remove("ring2",g.id)
            else changed.remove("ring",g.id)
        }
        changed[target]=g.id
        return stats(h,changed)-stats(h)
    }
    fun weight(g:Gear,role:Int):Int {
        var score=when(g.slot){
            "weapon"->g.power*10
            "armor"->g.power*(if(role==0)12 else 9)
            "helmet"->g.power*(if(role==0)8 else 7)
            "gloves"->g.power*(if(role==2)9 else 7)
            "boots"->g.power*(if(role==1)9 else 7)
            "ring"->g.power*8
            else->g.power*(if(role==2)9 else 7)
        }
        score+=g.rarity*7
        score+=when(g.trait){
            "крит"->if(role==1)45 else 20
            "уклонение"->if(role==1)40 else 15
            "защита"->if(role==0)40 else 14
            "регенерация"->22
            "яд"->if(role==2)32 else 18
            "здоровье"->if(role==0)35 else 16
            "атака"->if(role==2)38 else 25
            else->0
        }
        return score
    }
    fun isProtected(h:HeroState,g:Gear)=g.id in h.favorites || g.rarity>=3 || g.set>0 ||
        h.equipment.values.contains(g.id)
    fun canSalvage(h:HeroState,g:Gear)=!isProtected(h,g)
    fun equip(h:HeroState,g:Gear,secondRing:Boolean=false){
        val slot=targetSlot(g,secondRing)
        if(g.slot=="ring"){
            if(slot=="ring")h.equipment.remove("ring2",g.id)
            else h.equipment.remove("ring",g.id)
        }
        h.equipment[slot]=g.id
        h.hp=h.hp.coerceAtMost(h.maxHp)
    }
    private fun value(s:EquipmentStats,role:Int):Int = when(role){
        0->s.attack*4+s.defense*6+s.health/2+s.crit*3+s.dodge*3+
            s.regeneration*5+s.poison*2
        1->s.attack*5+s.defense*3+s.health/3+s.crit*6+s.dodge*6+
            s.regeneration*4+s.poison*3
        else->s.attack*7+s.defense*3+s.health/3+s.crit*5+s.dodge*3+
            s.regeneration*3+s.poison*6
    }
    fun improvement(h:HeroState,g:Gear,slot:String=g.slot,role:Int=h.heroClass):Int{
        if(g.slot=="ring" && slot !in listOf("ring","ring2"))return -100000
        if(g.slot!="ring" && slot!=g.slot)return -100000
        val equippedId=h.equipment[slot]
        if(equippedId==g.id)return 0
        if(equippedId!=null && equippedId in h.favorites)return -100000
        val changed=h.equipment.toMutableMap()
        if(g.slot=="ring"){
            if(slot=="ring")changed.remove("ring2",g.id)
            else changed.remove("ring",g.id)
        }
        changed[slot]=g.id
        return value(stats(h,changed),role)-value(stats(h),role)
    }
    fun recommendation(h:HeroState,g:Gear):Int{
        return if(g.slot=="ring")maxOf(improvement(h,g,"ring"),
            improvement(h,g,"ring2")) else improvement(h,g)
    }
    fun best(h:HeroState,role:Int,skipFavorites:Boolean=true):Int{
        var changed=0
        for(slot in slots){
            val itemSlot=if(slot=="ring2")"ring" else slot
            val current=h.gear.firstOrNull{it.id==h.equipment[slot]}
            if(skipFavorites && current!=null && current.id in h.favorites)continue
            val best=h.gear.filter{it.slot==itemSlot}
                .filter{it.id!=h.equipment[if(slot=="ring")"ring2" else "ring"] || itemSlot!="ring"}
                .maxByOrNull{improvement(h,it,slot,role)} ?: continue
            if(improvement(h,best,slot,role)>0){
                equip(h,best,slot=="ring2")
                changed++
            }
        }
        return changed
    }
    fun autoEquip(h:HeroState,g:Gear):Boolean{
        if(!h.autoEquip)return false
        val possible=if(g.slot=="ring")listOf("ring","ring2") else listOf(g.slot)
        val slot=possible.maxByOrNull{improvement(h,g,it)}?:return false
        if(improvement(h,g,slot)<=4)return false
        equip(h,g,slot=="ring2")
        return true
    }
    fun makeInventoryRoom(h:HeroState,item:Gear,capacity:Int=60):Boolean{
        if(h.gear.size<capacity)return true
        val replace=h.gear.filter{canSalvage(h,it)}
            .minByOrNull{weight(it,h.heroClass)}
        if(replace==null)return false
        if(weight(item,h.heroClass)<=weight(replace,h.heroClass))return false
        h.gear.remove(replace)
        h.newItems.remove(replace.id)
        h.favorites.remove(replace.id)
        h.shards+=1+replace.rarity*2
        return true
    }
}
