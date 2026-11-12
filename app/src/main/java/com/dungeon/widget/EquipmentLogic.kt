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
        val bonus=groups.values.sumOf{if(it>=4)12 else if(it>=2)4 else 0}
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
    fun best(h:HeroState,role:Int,skipFavorites:Boolean=true):Int{
        var changed=0
        for(slot in slots){
            val itemSlot=if(slot=="ring2")"ring" else slot
            val candidates=h.gear.filter{it.slot==itemSlot}
                .filter{slot!="ring2"||it.id!=h.equipment["ring"]}
                .filter{slot!="ring"||it.id!=h.equipment["ring2"]}
            val optimal=candidates.maxByOrNull{weight(it,role)}?:continue
            val current=h.gear.firstOrNull{it.id==h.equipment[slot]}
            if(current!=null&&current.id in h.favorites&&skipFavorites)continue
            if(current==null || weight(optimal,role)>weight(current,role)){
                equip(h,optimal,slot=="ring2")
                changed++
            }
        }
        return changed
    }
    fun autoEquip(h:HeroState,g:Gear):Boolean{
        if(!h.autoEquip)return false
        if(g.slot=="ring"){
            val a=h.gear.firstOrNull{it.id==h.equipment["ring"]}
            val b=h.gear.firstOrNull{it.id==h.equipment["ring2"]}
            if(a==null){equip(h,g);return true}
            if(b==null&&a.id!=g.id){equip(h,g,true);return true}
            val options=listOfNotNull(a?.let{"ring" to it},b?.let{"ring2" to it})
                .filter{it.second.id !in h.favorites}
            val worst=options.minByOrNull{weight(it.second,h.heroClass)}?:return false
            if(weight(g,h.heroClass)>weight(worst.second,h.heroClass)+4){
                equip(h,g,worst.first=="ring2");return true
            }
            return false
        }
        val current=h.gear.firstOrNull{it.id==h.equipment[g.slot]}
        if(current==null){equip(h,g);return true}
        if(current.id in h.favorites)return false
        if(weight(g,h.heroClass)>weight(current,h.heroClass)+4){
            equip(h,g);return true
        }
        return false
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
