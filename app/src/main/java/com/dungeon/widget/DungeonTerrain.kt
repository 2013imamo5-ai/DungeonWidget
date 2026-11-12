package com.dungeon.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/**
 * Terrain is rendered from the EXACT walkability grid used by DungeonMap.
 * Drawing never changes the navigation data or the saved route index.
 */
object DungeonTerrain {
    private val paint=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private fun rect(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){
        paint.color=color
        c.drawRect(x,y,x+w,y+h,paint)
    }
    private fun lighter(color:Int,delta:Int)=Color.rgb(
        min(255,Color.red(color)+delta),
        min(255,Color.green(color)+delta),
        min(255,Color.blue(color)+delta))
    private fun darker(color:Int,delta:Int)=Color.rgb(
        max(0,Color.red(color)-delta),
        max(0,Color.green(color)-delta),
        max(0,Color.blue(color)-delta))
    fun draw(c:Canvas,room:RoomLayout,biome:Int,floor:Int,wall:Int){
        val seed=room.seed
        val lip=lighter(wall,56)
        // Level has 20 x 11 cells, each 16px. A floor tile is never drawn as a wall.
        for(y in 0 until 11) for(x in 0 until 20){
            val px=x*16f
            val py=17f+y*16f
            val up=room.isFloor(x,y-1)
            val down=room.isFloor(x,y+1)
            val left=room.isFloor(x-1,y)
            val right=room.isFloor(x+1,y)
            val noise=(seed+x*31+y*47)%17
            if(room.isFloor(x,y)){
                val degree=(if(up)1 else 0)+(if(down)1 else 0)+
                    (if(left)1 else 0)+(if(right)1 else 0)
                // Room intersections are brighter and more spacious than hallways.
                val broad=degree>=3
                val base=lighter(floor,if(broad)35 else 19)
                rect(c,px,py,16f,16f,if(noise>=12)lighter(base,6)else base)
                rect(c,px,py,16f,1f,darker(base,20))
                rect(c,px,py,1f,16f,darker(base,19))
                if(broad){
                    rect(c,px+3f,py+5f,10f,1f,lighter(base,13))
                }else{
                    if(left&&right)rect(c,px+1f,py+7f,14f,2f,lighter(base,9))
                    if(up&&down)rect(c,px+7f,py+1f,2f,14f,lighter(base,9))
                }
                // Darkening just inside the edges makes one-tile corridors visible.
                val shadow=0x98070B12.toInt()
                if(!up)rect(c,px,py,16f,3f,shadow)
                if(!down)rect(c,px,py+13f,16f,3f,shadow)
                if(!left)rect(c,px,py,3f,16f,shadow)
                if(!right)rect(c,px+13f,py,3f,16f,shadow)
                if(noise==4)rect(c,px+11f,py+10f,2f,2f,lighter(base,24))
            }else{
                val edge=up||down||left||right
                rect(c,px,py,16f,16f,if(edge)darker(wall,2)else darker(wall,32))
                if(edge){
                    // Raised masonry and carved wall edges, NOT walkable floor.
                    rect(c,px+1f,py+1f,14f,3f,lighter(wall,22))
                    rect(c,px+1f,py+5f,14f,10f,darker(wall,8))
                    if((x+y)%2==0)rect(c,px+8f,py+6f,1f,8f,darker(wall,27))
                    if(down){
                        rect(c,px,py+13f,16f,3f,lip)
                        rect(c,px,py+10f,16f,3f,darker(wall,20))
                    }
                    if(up){
                        rect(c,px,py,16f,2f,lip)
                        rect(c,px,py+2f,16f,3f,darker(wall,12))
                    }
                    if(left){
                        rect(c,px,py,3f,16f,lip)
                        rect(c,px+3f,py,3f,16f,darker(wall,20))
                    }
                    if(right){
                        rect(c,px+13f,py,3f,16f,lip)
                        rect(c,px+10f,py,3f,16f,darker(wall,20))
                    }
                    if((up||down)&&(left||right)){
                        rect(c,px+3f,py+3f,10f,10f,lighter(wall,27))
                        rect(c,px+5f,py+5f,6f,6f,darker(wall,17))
                    }
                    // Torches and biome details are attached only to SOLID walls.
                    if(down && (seed+x*23+y*29)%19==3){
                        when(biome){
                            0,2->{
                                rect(c,px+6f,py+4f,4f,9f,0xFF756C72.toInt())
                                rect(c,px+3f,py-3f,10f,9f,
                                    if(biome==2)0xFFF16D39.toInt() else 0xFFE39B41.toInt())
                                rect(c,px+6f,py-6f,4f,7f,0xFFFFDE8A.toInt())
                            }
                            1->{
                                rect(c,px+6f,py+5f,4f,9f,0xFFB6A3A1.toInt())
                                rect(c,px+2f,py,12f,7f,0xFFA26099.toInt())
                                rect(c,px+5f,py+1f,2f,2f,0xFFFFD6E3.toInt())
                            }
                            else->{
                                rect(c,px+6f,py+3f,5f,11f,0xFF4E91B7.toInt())
                                rect(c,px+4f,py-3f,9f,7f,0xFF9AE6F7.toInt())
                            }
                        }
                    }
                }else if(noise==6){
                    rect(c,px+9f,py+6f,2f,2f,lighter(wall,4))
                }
            }
        }
        // Occasional archways are strictly located ON navigable corridor cells.
        for(y in 2..8)for(x in 2..17){
            if(!room.isFloor(x,y))continue
            val px=x*16f
            val py=17f+y*16f
            val across=room.isFloor(x-1,y)&&room.isFloor(x+1,y)&&
                !room.isFloor(x,y-1)&&!room.isFloor(x,y+1)
            val vertical=room.isFloor(x,y-1)&&room.isFloor(x,y+1)&&
                !room.isFloor(x-1,y)&&!room.isFloor(x+1,y)
            val gate=(x*7+y*11+seed)%11==0
            if(gate&&across){
                rect(c,px+2f,py+1f,12f,2f,0xFFBDA07D.toInt())
                rect(c,px+2f,py+13f,12f,2f,0xFFBDA07D.toInt())
                rect(c,px+7f,py+4f,2f,8f,0x886E5944.toInt())
            }else if(gate&&vertical){
                rect(c,px+1f,py+2f,2f,12f,0xFFBDA07D.toInt())
                rect(c,px+13f,py+2f,2f,12f,0xFFBDA07D.toInt())
                rect(c,px+4f,py+7f,8f,2f,0x886E5944.toInt())
            }
        }
        // Low debris is confined to intersections and wider chamber cells.
        for(y in 2..9)for(x in 2..17){
            if(!room.isFloor(x,y))continue
            val degree=(if(room.isFloor(x-1,y))1 else 0)+
                (if(room.isFloor(x+1,y))1 else 0)+
                (if(room.isFloor(x,y-1))1 else 0)+
                (if(room.isFloor(x,y+1))1 else 0)
            if(degree>=3&&(x*17+y*31+seed)%19==5){
                val px=x*16f
                val py=17f+y*16f
                rect(c,px+5f,py+5f,6f,5f,0xFF6B6467.toInt())
                rect(c,px+6f,py+4f,4f,2f,0xFFA09581.toInt())
            }
        }
    }
}
