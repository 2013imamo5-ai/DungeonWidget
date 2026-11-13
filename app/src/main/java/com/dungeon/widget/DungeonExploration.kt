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
    fun next(h:HeroState,room:RoomLayout):Tile?{
        val from=Tile(h.exploreX,h.exploreY)
        val start=index(from.x,from.y)
        // One BFS for every goal in the room: O(20*11) per step instead of
        // starting a new search for each unvisited floor tile.
        val distance=IntArray(CELLS){-1}
        val first=IntArray(CELLS){-1}
        val queue=java.util.ArrayDeque<Int>()
        distance[start]=0;first[start]=start;queue.add(start)
        while(queue.isNotEmpty()){
            val pos=queue.removeFirst()
            val cell=Tile(pos%DungeonMap.WIDTH,pos/DungeonMap.WIDTH)
            for(next in DungeonMap.neighbors(room,cell)){
                val idx=index(next.x,next.y)
                if(!seen(h,next.x,next.y)||distance[idx]!=-1)continue
                distance[idx]=distance[pos]+1
                first[idx]=if(pos==start)idx else first[pos]
                queue.addLast(idx)
            }
        }
        fun routeTo(index:Int):Tile?{
            val step=first.getOrNull(index)?:return null
            if(step<0)return null
            return Tile(step%DungeonMap.WIDTH,step/DungeonMap.WIDTH)
        }
        val discovered=room.events.filter{
            !completed(h,it)&&seen(h,it.cell.x,it.cell.y)&&
                distance[index(it.cell.x,it.cell.y)]>=0
        }
        if(discovered.isNotEmpty()){
            val e=discovered.minWithOrNull(
                compareBy<WorldEvent>{distance[index(it.cell.x,it.cell.y)]}
                    .thenBy{it.id})!!
            h.exploreAction=when(e.kind){
                "enemy","elite","boss"->"hunt"
                else->"loot"
            }
            return routeTo(index(e.cell.x,e.cell.y))
        }
        val pct=exploredPercent(h,room)
        val exitIndex=index(room.exit.x,room.exit.y)
        if(allEventsComplete(h,room) && pct>=85 && distance[exitIndex]>=0){
            h.exploreAction="exit"
            return routeTo(exitIndex)
        }
        var best=-1
        var score=Int.MAX_VALUE
        for(y in 1..9)for(x in 1..18){
            if(!room.isFloor(x,y)||!seen(h,x,y)||visited(h,x,y))continue
            val i=index(x,y)
            if(distance[i]<0)continue
            val frontier=DungeonMap.neighbors(room,Tile(x,y)).any{
                !seen(h,it.x,it.y)
            }
            val point=distance[i]*100+(if(frontier)0 else 20)+
                ((x*7+y*13+room.seed) and 15)
            if(point<score){score=point;best=i}
        }
        if(best>=0){
            h.exploreAction="scout"
            return routeTo(best)
        }
        val route=DungeonMap.shortest(room,from,room.exit)
        if(route.isNotEmpty()){
            h.exploreAction="exit"
            return route.getOrNull(1)?:route.first()
        }
        return null
    }

}
