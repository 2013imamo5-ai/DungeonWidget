package com.dungeon.widget

import kotlin.math.abs

/** Persistent fog masks plus local, reactive frontier search. */
object DungeonExploration{
    const val CELLS=220
    fun key(h:HeroState)=h.floor.toString()+":"+h.room.toString()
    fun index(x:Int,y:Int)=y*DungeonMap.WIDTH+x
    fun visited(h:HeroState,x:Int,y:Int)=
        h.exploreVisited.getOrNull(index(x,y))=='1'
    fun seen(h:HeroState,x:Int,y:Int)=
        h.exploreSeen.getOrNull(index(x,y))=='1'
    fun completed(h:HeroState,event:WorldEvent)=
        (h.exploreCompleted and (1 shl event.id))!=0
    fun complete(h:HeroState,event:WorldEvent){
        h.exploreCompleted=h.exploreCompleted or (1 shl event.id)
    }
    fun prepare(h:HeroState,room:RoomLayout){
        if(h.exploreRoom==key(h) &&
            room.isFloor(h.exploreX,h.exploreY) &&
            h.exploreSeen.length==CELLS &&
            h.exploreVisited.length==CELLS)return
        h.exploreRoom=key(h)
        h.exploreX=room.start.x;h.exploreY=room.start.y
        h.explorePreviousX=h.exploreX;h.explorePreviousY=h.exploreY
        h.exploreSeen="0".repeat(CELLS)
        h.exploreVisited="0".repeat(CELLS)
        h.exploreCompleted=0
        h.activeEvent=-1
        h.exploreFacing=2
        h.exploreAction="explore"
        h.pathStep=0;h.enemyHp=0;h.enemyMaxHp=0
        h.battleRound=0;h.enemyWarning=false
        h.lastDamage=0;h.lastAbility=""
        h.event="explore"
        h.log="Неизведанное подземелье. Герой осматривает проходы"
        reveal(h,room)
    }
    // Grid raycasting: solid walls occlude what lies behind them.
    fun visible(room:RoomLayout,from:Tile,to:Tile,radius:Int=4):Boolean{
        val dx=to.x-from.x
        val dy=to.y-from.y
        if(dx*dx+dy*dy>radius*radius)return false
        var x=from.x
        var y=from.y
        val adx=abs(dx);val ady=abs(dy)
        val sx=if(dx<0)-1 else 1
        val sy=if(dy<0)-1 else 1
        var err=adx-ady
        while(x!=to.x || y!=to.y){
            val error=2*err
            if(error> -ady){err-=ady;x+=sx}
            if(error<adx){err+=adx;y+=sy}
            // Target wall can be seen, but no tiles behind an intermediate wall.
            if((x!=to.x||y!=to.y)&&!room.isFloor(x,y))return false
        }
        return true
    }
    fun reveal(h:HeroState,room:RoomLayout){
        val current=Tile(h.exploreX,h.exploreY)
        val explored=h.exploreSeen.toCharArray()
        for(y in 0 until DungeonMap.HEIGHT)for(x in 0 until DungeonMap.WIDTH){
            if(visible(room,current,Tile(x,y)))explored[index(x,y)]='1'
        }
        h.exploreSeen=String(explored)
        val passed=h.exploreVisited.toCharArray()
        passed[index(current.x,current.y)]='1'
        h.exploreVisited=String(passed)
    }
    fun exploredPercent(h:HeroState,room:RoomLayout):Int{
        var all=0;var opened=0
        for(y in 0 until DungeonMap.HEIGHT)for(x in 0 until DungeonMap.WIDTH){
            if(room.isFloor(x,y)){
                all++
                if(seen(h,x,y))opened++
            }
        }
        return if(all==0)0 else opened*100/all
    }
    fun seenEvents(h:HeroState,room:RoomLayout)=
        room.events.count{seen(h,it.cell.x,it.cell.y)&&!completed(h,it)}
    fun allEventsComplete(h:HeroState,room:RoomLayout)=
        room.events.all{completed(h,it)}
    fun atEvent(h:HeroState,room:RoomLayout)=room.events.firstOrNull{
        it.cell.x==h.exploreX && it.cell.y==h.exploreY && !completed(h,it)
    }
    // Target is picked on each game step, not precomputed once for the room.
    // Search only through already revealed traversable cells.
    // Search follows worthwhile leads, rather than stepping on every
    // discovered empty tile to reach an arbitrary exploration percentage.
    fun next(h:HeroState,room:RoomLayout):Tile?{
        val from=Tile(h.exploreX,h.exploreY)
        if(from==room.exit){
            h.exploreAction="exit"
            return from
        }
        // Actual sightings have absolute priority: enemies first, then loot.
        val sightings=room.events.asSequence()
            .filter{!completed(h,it) && seen(h,it.cell.x,it.cell.y)}
            .mapNotNull{event->
                val path=DungeonMap.shortest(room,from,event.cell,h.exploreSeen)
                if(path.isEmpty())null else event to path
            }
            .sortedWith(compareBy<Pair<WorldEvent,List<Tile>>>{
                if(it.first.kind in listOf("enemy","elite","boss"))0 else 1
            }.thenBy{it.second.size})
            .firstOrNull()
        if(sightings!=null){
            h.exploreAction=if(sightings.first.kind in
                listOf("enemy","elite","boss"))"hunt" else "loot"
            return sightings.second.getOrNull(1)?:sightings.second.first()
        }

        // The stairway is the default objective. A side branch is explored
        // only if it offers a valuable event with a modest detour; barren
        // branches are skipped. Unseen encounters never appear on screen.
        val exitRoute=DungeonMap.shortest(room,from,room.exit)
        if(exitRoute.isEmpty())return null
        val possible=room.events.asSequence()
            .filter{!completed(h,it) && !seen(h,it.cell.x,it.cell.y)}
            .filter{it.kind in listOf(
                "boss","elite","chest","cache","relic","secret","shrine","merchant"
            )}
            .mapNotNull{event->
                val approach=DungeonMap.shortest(room,from,event.cell)
                val onward=DungeonMap.shortest(room,event.cell,room.exit)
                if(approach.isEmpty()||onward.isEmpty())null else{
                    val detour=approach.size+onward.size-exitRoute.size-1
                    Triple(event,approach,detour)
                }
            }
            .filter{(event,approach,detour)->
                event.kind=="boss" ||
                    (detour<=6 && approach.size<=18)
            }
            .minWithOrNull(compareBy<Triple<WorldEvent,List<Tile>,Int>>{
                if(it.first.kind=="boss")-1000 else it.third*8+it.second.size
            }.thenBy{it.first.id})
        if(possible!=null){
            h.exploreAction="scout"
            return possible.second.getOrNull(1)?:possible.second.first()
        }
        h.exploreAction="exit"
        return exitRoute.getOrNull(1)?:exitRoute.first()
    }

}
