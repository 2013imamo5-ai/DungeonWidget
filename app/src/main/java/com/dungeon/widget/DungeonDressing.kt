package com.dungeon.widget

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sin

/**
 * Cosmetic, deterministic decoration only. It does not alter walkability,
 * event placement or exploration saves. The scene's fog covers it naturally.
 */
object DungeonDressing {
    private val brush=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private fun r(c:Canvas,x:Float,y:Float,w:Float,h:Float,col:Int){
        brush.color=col
        c.drawRect(x,y,x+w,y+h,brush)
    }
    private fun hash(seed:Int,x:Int,y:Int):Int{
        var v=seed xor (x*73856093) xor (y*19349663)
        v=v xor (v ushr 16)
        v*=0x7feb352d
        v=v xor (v ushr 15)
        return v and Int.MAX_VALUE
    }
    fun draw(c:Canvas,room:RoomLayout,biome:Int,time:Long,animate:Boolean){
        val t=if(animate)time else room.seed.toLong()*225
        c.save()
        c.clipRect(0f,17f,320f,193f)
        for(y in 1..9)for(x in 1..18){
            val px=x*16f
            val py=17f+y*16f
            val noise=hash(room.seed,x,y)
            if(!room.isFloor(x,y)){
                if(room.isFloor(x,y+1) && noise%9==1){
                    // Wall-mounted ornaments visible from the corridor.
                    when(biome){
                        0->{
                            // Worn funeral banner with a narrow golden sigil.
                            r(c,px+5f,py+3f,7f,11f,0xFF5F465E.toInt())
                            r(c,px+6f,py+4f,5f,1f,0xFFD1AD6C.toInt())
                            r(c,px+8f,py+7f,1f,5f,0xFFA58C71.toInt())
                            r(c,px+7f,py+9f,3f,1f,0xFFA58C71.toInt())
                        }
                        1->{
                            // Hanging vines and luminescent spores.
                            r(c,px+4f,py+1f,2f,11f,0xFF4B7658.toInt())
                            r(c,px+6f,py+6f,5f,2f,0xFF658F62.toInt())
                            r(c,px+11f,py+7f,2f,3f,0xFF8FCB78.toInt())
                        }
                        2->{
                            // Chipped metal plate and glowing hot seam.
                            r(c,px+3f,py+3f,11f,8f,0xFF4A4245.toInt())
                            r(c,px+5f,py+4f,1f,6f,0xFF8C6056.toInt())
                            r(c,px+10f,py+5f,2f,4f,0xFFC47A47.toInt())
                            r(c,px+10f,py+6f,1f,2f,0xFFF1C071.toInt())
                        }
                        else->{
                            // Frost running through a carved wall relief.
                            r(c,px+7f,py+2f,3f,11f,0xFF648BA2.toInt())
                            r(c,px+4f,py+5f,9f,2f,0xFFB1DCE5.toInt())
                            r(c,px+6f,py+9f,6f,1f,0xFF83B9CF.toInt())
                        }
                    }
                }
                continue
            }
            // Avoid flooding narrow corridors with decorative props.
            val neighbors=DungeonMap.neighbors(room,Tile(x,y)).size
            if(noise%13==0 && neighbors<=2 && !room.events.any{
                    it.cell.x==x&&it.cell.y==y
                }){
                when(biome){
                    0->{
                        // Skull, broken ribs and a little dust in old crypts.
                        r(c,px+4f,py+9f,5f,4f,0xFFC2BAA8.toInt())
                        r(c,px+5f,py+10f,1f,1f,0xFF37404A.toInt())
                        r(c,px+7f,py+10f,1f,1f,0xFF37404A.toInt())
                        r(c,px+9f,py+11f,4f,1f,0xFF9D988F.toInt())
                    }
                    1->{
                        // Short mushrooms with slightly shifting spore flecks.
                        r(c,px+7f,py+10f,2f,5f,0xFFD0C5AC.toInt())
                        r(c,px+3f,py+8f,10f,4f,0xFFBA739B.toInt())
                        r(c,px+5f,py+7f,6f,2f,0xFFE093B7.toInt())
                        if((t/380+noise)%4L<2L)
                            r(c,px+12f,py+4f,2f,2f,0xFFB4E0BA.toInt())
                    }
                    2->{
                        // Glowing charcoal and intermittent ember.
                        r(c,px+4f,py+11f,10f,4f,0xFF44373A.toInt())
                        r(c,px+6f,py+12f,3f,2f,0xFFBD6B49.toInt())
                        if((t/240+noise)%3L!=0L)
                            r(c,px+11f,py+9f,2f,3f,0xFFFFB368.toInt())
                    }
                    else->{
                        // Small multi-shaded ice crystal cluster.
                        r(c,px+4f,py+8f,4f,7f,0xFF6799B3.toInt())
                        r(c,px+6f,py+5f,2f,10f,0xFFC6EDF1.toInt())
                        r(c,px+9f,py+10f,4f,5f,0xFF80B4D1.toInt())
                        r(c,px+10f,py+8f,1f,5f,0xFFD6F6F7.toInt())
                    }
                }
            }
            if(neighbors>=3 && noise%11==2){
                // Fragmented floor mosaic on some junctions, low and flat.
                val detail=when(biome){
                    0->0xFF807A8A.toInt()
                    1->0xFF6E9D7B.toInt()
                    2->0xFF9D7164.toInt()
                    else->0xFF94BDCF.toInt()
                }
                r(c,px+5f,py+5f,6f,1f,detail)
                r(c,px+5f,py+10f,6f,1f,detail)
                r(c,px+5f,py+6f,1f,4f,detail)
                r(c,px+10f,py+6f,1f,4f,detail)
            }
        }
        c.restore()
    }

    fun lootGlitter(c:Canvas,x:Float,y:Float,seed:Int,now:Long){
        val phase=(now/220+seed*17L)%9
        if(phase>5)return
        val size=if(phase<3)3f else 2f
        r(c,x+12f,y-19f,size,size,0xFFFFE59D.toInt())
        r(c,x-11f,y-13f,2f,2f,0xFFC5EEF0.toInt())
    }
}
