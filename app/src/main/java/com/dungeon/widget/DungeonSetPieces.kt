package com.dungeon.widget

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sin

/**
 * Ornamental pixel-art architecture. Every set piece is derived from the
 * deterministic RoomLayout; collision, fog, event positions and saves are
 * never modified. This layer is painted behind the hero and encounters.
 */
object DungeonSetPieces {
    private val p=Paint().apply { isAntiAlias=false;isFilterBitmap=false }
    private fun r(c:Canvas,x:Float,y:Float,w:Float,h:Float,col:Int){
        p.color=col
        c.drawRect(x,y,x+w,y+h,p)
    }
    private fun noise(seed:Int,x:Int,y:Int):Int {
        var v=seed xor (x*16807) xor (y*48271)
        v=(v xor (v ushr 15))*0x45d9f3b
        return (v xor (v ushr 16)) ushr 1
    }
    private fun stone(c:Canvas,x:Float,y:Float,w:Float,h:Float){
        r(c,x+2f,y+3f,w,h,0x99080D19.toInt())
        r(c,x,y,w,h,0xFF474456.toInt())
        r(c,x,y,w,3f,0xFF8A7C80.toInt())
        r(c,x,y+3f,3f,h-3f,0xFF6A6270.toInt())
        r(c,x+w-3f,y+3f,3f,h-3f,0xFF282936.toInt())
        r(c,x+5f,y+8f,w-10f,2f,0xFF302C3C.toInt())
        r(c,x+5f,y+13f,2f,4f,0xFF342F3F.toInt())
    }
    private fun burialNiche(c:Canvas,x:Float,y:Float,biome:Int){
        // A recessed funeral alcove, not a traversable doorway.
        r(c,x+2f,y+2f,28f,22f,0xFF716676.toInt())
        r(c,x+5f,y+4f,22f,20f,0xFF242435.toInt())
        r(c,x+8f,y+8f,16f,14f,0xFF171E2E.toInt())
        r(c,x+8f,y+5f,16f,3f,0xFF99858C.toInt())
        r(c,x+12f,y+10f,8f,6f,if(biome==3)0xFF9BC8D7.toInt()
            else 0xFFB8AE9B.toInt())
        r(c,x+13f,y+13f,2f,2f,0xFF364050.toInt())
        r(c,x+17f,y+13f,2f,2f,0xFF364050.toInt())
        r(c,x+13f,y+18f,6f,2f,0xFF8F8B90.toInt())
        r(c,x+3f,y+23f,26f,3f,0xFF7E7381.toInt())
    }
    private fun banner(c:Canvas,x:Float,y:Float,biome:Int){
        val fabric=when(biome){
            1->0xFF42634D.toInt()
            2->0xFF743B3F.toInt()
            3->0xFF385E78.toInt()
            else->0xFF653C4F.toInt()
        }
        r(c,x+3f,y-3f,10f,3f,0xFFAA8C65.toInt())
        r(c,x+4f,y,8f,17f,fabric)
        r(c,x+6f,y+3f,4f,2f,0xFFD5B67E.toInt())
        r(c,x+7f,y+7f,2f,7f,0xFFBD9A75.toInt())
        r(c,x+5f,y+10f,6f,2f,0xFFBD9A75.toInt())
        r(c,x+5f,y+17f,6f,3f,0xFF2E2735.toInt())
        r(c,x+6f,y+18f,4f,2f,fabric)
    }
    private fun masonryPillar(c:Canvas,x:Float,y:Float){
        r(c,x+2f,y+5f,17f,27f,0x88090D15.toInt())
        r(c,x+3f,y+4f,13f,23f,0xFF424052.toInt())
        r(c,x+5f,y+4f,3f,22f,0xFF686372.toInt())
        r(c,x+11f,y+4f,3f,22f,0xFF302D3D.toInt())
        r(c,x,y+1f,20f,6f,0xFF716772.toInt())
        r(c,x+2f,y,16f,3f,0xFFAB9586.toInt())
        r(c,x+2f,y+25f,16f,5f,0xFF544F60.toInt())
        r(c,x+1f,y+27f,18f,3f,0xFF8E7E7E.toInt())
    }
    private fun crystal(c:Canvas,x:Float,y:Float,biome:Int){
        val dark=if(biome==2)0xFFB16F60.toInt() else 0xFF486DAA.toInt()
        val mid=if(biome==2)0xFFE9A36C.toInt() else 0xFF7697E3.toInt()
        val shine=if(biome==2)0xFFFFD394.toInt() else 0xFFA6E9F9.toInt()
        r(c,x+2f,y+9f,14f,5f,0xAA111827.toInt())
        r(c,x+3f,y+2f,5f,11f,dark)
        r(c,x+5f,y,3f,12f,mid)
        r(c,x+6f,y+2f,1f,7f,shine)
        r(c,x+9f,y+6f,6f,7f,dark)
        r(c,x+11f,y+4f,3f,7f,mid)
        r(c,x+12f,y+6f,1f,3f,shine)
    }
    private fun metalGrate(c:Canvas,x:Float,y:Float,biome:Int){
        r(c,x+1f,y+1f,14f,14f,0xFF151B27.toInt())
        r(c,x+3f,y+3f,10f,10f,if(biome==2)0xFF984F36.toInt()
            else 0xFF2F5160.toInt())
        for(i in 0..3)r(c,x+2f+i*3f,y+1f,2f,14f,0xFF6C6570.toInt())
        r(c,x+1f,y+6f,14f,2f,0xFF898080.toInt())
    }
    private fun floorSigil(c:Canvas,x:Float,y:Float,biome:Int){
        val edge=if(biome==3)0xFF8CC0D6.toInt() else 0xFF9A7563.toInt()
        r(c,x+2f,y+2f,12f,1f,edge)
        r(c,x+2f,y+13f,12f,1f,edge)
        r(c,x+2f,y+3f,1f,10f,edge)
        r(c,x+13f,y+3f,1f,10f,edge)
        r(c,x+6f,y+5f,4f,6f,0xFF58404B.toInt())
        r(c,x+7f,y+3f,2f,10f,edge)
        r(c,x+5f,y+7f,6f,2f,edge)
    }
    private fun stairs(c:Canvas,exit:Tile,biome:Int,time:Long,animate:Boolean){
        val x=exit.x*16f
        val y=17f+exit.y*16f
        val col=when(biome){
            1->0xFF8FD8A9.toInt()
            2->0xFFE6A06D.toInt()
            3->0xFFA3DAF2.toInt()
            else->0xFFB0B6EA.toInt()
        }
        r(c,x-1f,y+2f,18f,14f,0xFF242639.toInt())
        for(i in 0..3){
            r(c,x+1f,y+4f+i*3f,14f-i,2f,0xFF88777E.toInt())
            r(c,x+2f,y+5f+i*3f,11f-i,1f,0xFFBAA38F.toInt())
        }
        val pulse=if(animate)sin(time/340.0).toFloat()*2f else 0f
        r(c,x+5f,y-3f+pulse,7f,4f,col)
        r(c,x+7f,y-5f+pulse,3f,2f,0xFFDDFBFF.toInt())
    }

    fun draw(c:Canvas,room:RoomLayout,biome:Int,time:Long,animate:Boolean){
        c.save()
        c.clipRect(0f,17f,320f,193f)
        // Set pieces always remain underneath enemy sprites and fog-of-war.
        for(y in 1..9)for(x in 1..18){
            val n=noise(room.seed,x,y)
            val px=x*16f
            val py=17f+y*16f
            val isFloor=room.isFloor(x,y)
            val below=room.isFloor(x,y+1)
            val above=room.isFloor(x,y-1)
            if(!isFloor){
                // Gravestones and masonry silhouettes belong to solid blocks.
                if(below&&n%23==3)masonryPillar(c,px-2f,py-13f)
                else if(below&&n%31==8)banner(c,px,py-3f,biome)
                if(below && x<17 && !room.isFloor(x+1,y) &&
                    n%47==11)burialNiche(c,px,py-5f,biome)
                if((below||above)&&n%37==7){
                    val t=if(animate)time else room.seed.toLong()
                    if((t/600+n)%5L!=0L)crystal(c,px,py,biome)
                }
                continue
            }
            // Flat decorations use no collision and preserve the path grid.
            val adjacent=room.isFloor(x-1,y)&&room.isFloor(x+1,y)&&
                room.isFloor(x,y-1)&&room.isFloor(x,y+1)
            val eventCell=room.events.any{it.cell.x==x&&it.cell.y==y}
            if(adjacent&&!eventCell&&n%29==6)floorSigil(c,px,py,biome)
            if(adjacent&&!eventCell&&n%53==17)metalGrate(c,px,py,biome)
            if(adjacent && !eventCell && n%43==9){
                // Stone chips and a few dry bones, never an impassable prop.
                r(c,px+3f,py+10f,5f,2f,0xFFAAA092.toInt())
                r(c,px+6f,py+7f,2f,4f,0xFFD7C6A7.toInt())
                r(c,px+10f,py+8f,4f,2f,0xFF877E85.toInt())
            }
        }
        // Boss encounters are marked by a worn crimson floor seal, like
        // the one visible in the dungeon reference. This is not a hazard.
        for(e in room.events){
            if(e.kind!="boss")continue
            val cx=e.cell.x*16f+8f
            val cy=17f+e.cell.y*16f+6f
            for(i in 0..23){
                val a=i*Math.PI/12.0
                val dx=sin(a).toFloat()*17f
                val dy=kotlin.math.cos(a).toFloat()*13f
                r(c,cx+dx-1f,cy+dy-1f,3f,2f,0xFF814450.toInt())
            }
            r(c,cx-5f,cy-3f,10f,7f,0xFF6B3549.toInt())
            r(c,cx-3f,cy-2f,6f,5f,0xFFBF7569.toInt())
            r(c,cx-2f,cy-1f,2f,2f,0xFF352332.toInt())
            r(c,cx+1f,cy-1f,2f,2f,0xFF352332.toInt())
        }
        // Every room has a distinct, recognizable staircase at its actual exit.
        stairs(c,room.exit,biome,time,animate)
        c.restore()
    }
}
