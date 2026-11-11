package com.dungeon.widget

import java.util.ArrayDeque

data class Tile(val x:Int,val y:Int)
data class RoomLayout(
    val walkable:Array<BooleanArray>,
    val path:List<Tile>,
    val secret:Tile?,
    val seed:Int
){
    fun isFloor(x:Int,y:Int)=y in walkable.indices &&
        x in walkable[y].indices && walkable[y][x]
}
object DungeonMap{
    private val recent=object:LinkedHashMap<Long,RoomLayout>(48,0.75f,true){
        override fun removeEldestEntry(eldest:MutableMap.MutableEntry<Long,RoomLayout>?):Boolean=size>48
    }
    @Synchronized fun get(floor:Int,room:Int):RoomLayout{
        val key=(floor.toLong() shl 16)+room.toLong()
        return recent.getOrPut(key){generate(floor,room)}
    }
    private fun generate(floor:Int,room:Int):RoomLayout{
        val seed=floor*137+room*257
        val a=Array(11){BooleanArray(20)}
        // Three parallel passages and connecting corridors form a navigable maze.
        for(x in 1..18){a[3][x]=true;a[5][x]=true;a[7][x]=true}
        for(x in listOf(3,7,11,15,18))for(y in 3..8)a[y][x]=true
        // The layout changes by room; the main passage is interrupted by walls.
        val blocks=listOf(6,10,14).filterIndexed{index,_->(seed+index*13)%4!=0}
        for(x in blocks)a[5][x]=false
        // Optional rooms with alcoves and hidden chambers.
        for(x in 3..6){a[1][x]=true;a[2][x]=true}
        for(x in 11..15){a[9][x]=true}
        for(y in 7..9)a[y][15]=true
        val secret=if((seed and 3)==0){a[2][6]=true;Tile(6,2)} else null
        // A deterministic breadth-first search finds the actual corridor route.
        val start=Tile(1,5);val goal=Tile(18,8)
        val path=if(secret==null)shortest(a,seed,start,goal) else{
            shortest(a,seed,start,secret)+shortest(a,seed,secret,goal).drop(1)
        }
        return RoomLayout(a,path,secret,seed)
    }
    private fun shortest(a:Array<BooleanArray>,seed:Int,start:Tile,goal:Tile):List<Tile>{
        val queue=ArrayDeque<Tile>();queue.add(start)
        val parent=HashMap<Tile,Tile>();val visited=HashSet<Tile>();visited.add(start)
        val dirs=listOf(Tile(1,0),Tile(0,1),Tile(0,-1),Tile(-1,0))
        while(queue.isNotEmpty()){
            val v=queue.removeFirst()
            if(v==goal)break
            val directions=if(seed%2==0)dirs else dirs.reversed()
            for(d in directions){
                val u=Tile(v.x+d.x,v.y+d.y)
                if(u.y in 0..10 && u.x in 0..19 && a[u.y][u.x] && visited.add(u)){
                    parent[u]=v;queue.addLast(u)
                }
            }
        }
        val path=ArrayList<Tile>();var at=goal
        path.add(at)
        while(at!=start&&path.size<160){
            at=parent[at]?:break
            path.add(at)
        }
        return if(at==start)path.asReversed() else listOf(start,goal)
    }

}
