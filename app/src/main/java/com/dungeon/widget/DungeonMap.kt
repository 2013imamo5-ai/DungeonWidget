package com.dungeon.widget

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.random.Random

data class Tile(val x:Int,val y:Int)
data class WorldEvent(val id:Int,val cell:Tile,val kind:String,val name:String="")
data class RoomLayout(
    val walkable:Array<BooleanArray>,
    val path:List<Tile>,
    val secret:Tile?,
    val seed:Int,
    val events:List<WorldEvent> = emptyList(),
    val start:Tile=Tile(1,5),
    val exit:Tile=Tile(18,5)
){
    fun isFloor(x:Int,y:Int)=y in walkable.indices &&
        x in walkable[y].indices && walkable[y][x]
}
object DungeonMap{
    const val WIDTH=20
    const val HEIGHT=11
    private val recent=object:LinkedHashMap<Long,RoomLayout>(48,0.75f,true){
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Long,RoomLayout>?):Boolean=size>48
    }
    @Synchronized fun get(floor:Int,room:Int):RoomLayout{
        val key=(floor.toLong() shl 16)+room.toLong()
        return recent.getOrPut(key){generate(floor,room)}
    }
    private val moves=listOf(Tile(1,0),Tile(-1,0),Tile(0,1),Tile(0,-1))
    fun neighbors(room:RoomLayout,t:Tile):List<Tile>{
        return moves.map{Tile(t.x+it.x,t.y+it.y)}
            .filter{room.isFloor(it.x,it.y)}
    }
    fun distance(a:Tile,b:Tile)=abs(a.x-b.x)+abs(a.y-b.y)

    // BFS returns a navigable route, or empty for an unreachable destination.
    // Optional seen mask prevents planning through unexplored cells.
    fun shortest(room:RoomLayout,from:Tile,to:Tile,seen:String?=null):List<Tile>{
        if(!room.isFloor(from.x,from.y)||!room.isFloor(to.x,to.y))return emptyList()
        if(from==to)return listOf(from)
        val q=ArrayDeque<Tile>()
        q.add(from)
        val parents=HashMap<Tile,Tile>(160)
        val visited=HashSet<Tile>(160)
        visited.add(from)
        while(q.isNotEmpty()){
            val at=q.removeFirst()
            if(at==to)break
            for(n in neighbors(room,at)){
                val index=n.y*WIDTH+n.x
                if(seen!=null && (index !in seen.indices || seen[index]!='1'))continue
                if(visited.add(n)){
                    parents[n]=at
                    q.addLast(n)
                }
            }
        }
        if(to !in visited)return emptyList()
        val result=ArrayList<Tile>()
        var at=to
        result.add(at)
        while(at!=from && result.size<WIDTH*HEIGHT){
            at=parents[at]?:return emptyList()
            result.add(at)
        }
        result.reverse()
        return result
    }
    private fun generate(floor:Int,room:Int):RoomLayout{
        val seed=(floor*137 xor room*1907 xor floor*room*47)+27119
        val rng=Random(seed)
        val tiles=Array(HEIGHT){BooleanArray(WIDTH)}
        val start=Tile(1,5)
        val exit=Tile(18,5)
        tiles[start.y][start.x]=true

        // Randomized depth-first maze with guaranteed connected odd-cell nodes.
        val stack=ArrayDeque<Tile>()
        val nodes=HashSet<Tile>()
        nodes.add(start);stack.addLast(start)
        val hops=listOf(Tile(2,0),Tile(-2,0),Tile(0,2),Tile(0,-2))
        while(stack.isNotEmpty()){
            val from=stack.peekLast()
            val candidates=hops.map{Tile(from.x+it.x,from.y+it.y)}
                .filter{it.x in 1..17 && it.y in 1..9 && it !in nodes}
            if(candidates.isEmpty()){stack.removeLast();continue}
            val pick=candidates[rng.nextInt(candidates.size)]
            tiles[(from.y+pick.y)/2][(from.x+pick.x)/2]=true
            tiles[pick.y][pick.x]=true
            nodes.add(pick);stack.addLast(pick)
        }
        // Connect exit to the fully connected maze.
        tiles[5][18]=true
        // Alternative shortcuts, loops and galleries.
        repeat(4+rng.nextInt(6)){
            val x=rng.nextInt(2,18)
            val y=rng.nextInt(2,9)
            if((tiles[y][x-1]&&tiles[y][x+1])||
                (tiles[y-1][x]&&tiles[y+1][x]))tiles[y][x]=true
        }
        // Wider rooms of different shapes. Adding floor cannot disconnect paths.
        repeat(2+rng.nextInt(3)){
            val center=nodes.elementAt(rng.nextInt(nodes.size))
            val w=if(rng.nextBoolean())1 else 2
            val h=if(rng.nextBoolean())1 else 2
            for(y in (center.y-h)..(center.y+h))
                for(x in (center.x-w)..(center.x+w))
                    if(x in 1..18 && y in 1..9 &&
                        rng.nextInt(100)<(if(x==center.x||y==center.y)96 else 64))
                        tiles[y][x]=true
        }
        // Always keep the start/exit and their approaches open.
        tiles[5][1]=true;tiles[5][2]=true
        tiles[5][17]=true;tiles[5][18]=true
        // If exit became isolated due to a layout change, carve a short connector.
        val preliminary=RoomLayout(tiles,emptyList(),null,seed,start=start,exit=exit)
        if(shortest(preliminary,start,exit).isEmpty()){
            for(x in 1..18)tiles[5][x]=true
        }
        val all=RoomLayout(tiles,emptyList(),null,seed,start=start,exit=exit)
        val deadEnds=(1..9).flatMap{y->(1..18).map{x->Tile(x,y)}}
            .filter{all.isFloor(it.x,it.y) &&
                neighbors(all,it).size==1 &&
                distance(it,start)>6 && distance(it,exit)>5}
        val secret=if(deadEnds.isNotEmpty() && rng.nextInt(100)<65)
            deadEnds[rng.nextInt(deadEnds.size)] else null
        val events=ArrayList<WorldEvent>()
        val candidates=(1..9).flatMap{y->(1..18).map{x->Tile(x,y)}}
            .filter{all.isFloor(it.x,it.y) &&
                distance(it,start)>=4 && distance(it,exit)>=3}.shuffled(rng).toMutableList()
        val bosses=room==8 && floor%10==0
        fun place(kind:String,preferred:Tile?=null):Boolean{
            val spot=if(preferred!=null&&
                events.none{distance(it.cell,preferred)<3})preferred
                else candidates.firstOrNull{t->
                    events.none{distance(it.cell,t)<4} &&
                    distance(t,start)>=4 && distance(t,exit)>=3
                }
            if(spot==null)return false
            val id=events.size
            events.add(WorldEvent(id,spot,kind))
            candidates.remove(spot)
            return true
        }
        if(boss)place("boss") else {
            place(if(rng.nextInt(100)<18)"elite" else "enemy")
            if(rng.nextInt(100)<75)place("enemy")
        }
        place(if(rng.nextBoolean())"chest" else "cache")
        place(listOf("enemy","chest","potion","shrine","relic","trap","merchant")[rng.nextInt(7)])
        if(secret!=null)place("secret",secret)
        if(rng.nextInt(100)<58)
            place(listOf("breakable","trap","cache","enemy","potion")[rng.nextInt(5)])
        if(rng.nextInt(100)<32)place("breakable")
        return RoomLayout(tiles,shortest(all,start,exit),secret,seed,
            events,start,exit)
    }
}
