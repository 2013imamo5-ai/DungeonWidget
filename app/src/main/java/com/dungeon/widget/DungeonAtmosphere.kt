package com.dungeon.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.sin

/** Low-cost, nearest-neighbour pixel lighting and ambience. No bitmap assets or shaders. */
object DungeonAtmosphere {
    private val brush=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private fun rect(c:Canvas,x:Float,y:Float,w:Float,h:Float,col:Int){
        brush.color=col
        c.drawRect(x,y,x+w,y+h,brush)
    }
    fun draw(c:Canvas,room:RoomLayout,biome:Int,now:Long,animate:Boolean){
        val tick=if(animate)now else room.seed.toLong()*107L
        c.save()
        c.clipRect(0f,17f,320f,193f)
        // Every halo is attached to a torch on a SOLID masonry cell.
        for(y in 1..9)for(x in 1..18){
            if(room.isFloor(x,y)||!room.isFloor(x,y+1))continue
            if((room.seed+x*23+y*29)%19!=3)continue
            val cx=x*16f+8f
            val cy=y*16f+17f
            val flicker=sin(tick/245.0+x*2.1+y).toFloat()
            val a=(27+flicker*5).toInt().coerceIn(20,35)
            val glow=when(biome){
                1->Color.argb(a,144,225,151)
                3->Color.argb(a,112,207,245)
                else->Color.argb(a,255,177,95)
            }
            rect(c,cx-26f,cy-15f,52f,44f,glow)
            rect(c,cx-15f,cy-10f,30f,30f,
                Color.argb((a+13).coerceAtMost(65),if(biome==3)126 else 255,
                    if(biome==1)245 else 199,if(biome==3)255 else 106))
            rect(c,cx-6f,cy-11f,12f,13f,
                Color.argb((a+24).coerceAtMost(85),255,234,176))
            if(animate && biome!=1){
                // Tiny flickering embers rise rather than moving the torch base.
                val up=((tick/140+x*7+y*3)%12).toFloat()
                rect(c,cx-3f+(x%3)*2f,cy-12f-up,2f,3f,
                    if(biome==3)0xFFABEDFA.toInt() else 0xFFFFC778.toInt())
            }
        }
        // Sparse atmospheric motes in navigable corridors. The walkability
        // check keeps them out of the black void beyond the masonry.
        for(i in 0..23){
            val x=(room.seed*3+i*17).ushr(1).rem(18)+1
            val y=((room.seed*7+i*13).ushr(1)).rem(9)+1
            if(!room.isFloor(x,y))continue
            val shimmer=(i+tick/190)%23
            if(shimmer>=15)continue
            val drift=if(animate)((tick/170+i*7)%7).toFloat() else 0f
            val px=x*16f+4f+(i%3)*3f
            val py=17f+y*16f+3f+drift
            val shade=when(biome){
                0->Color.argb(73,200,199,178)
                1->Color.argb(96,140,241,166)
                2->Color.argb(94,255,153,100)
                else->Color.argb(100,174,231,247)
            }
            rect(c,px,py,if(i%4==0)2f else 1f,if(i%5==0)2f else 1f,shade)
        }
        c.restore()
    }

    /**
     * Soft 3-layer lantern glow and short-lived footfall dust. It appears
     * behind the hero and remains subject to normal wall/fog occlusion.
     */
    fun heroLight(c:Canvas,x:Float,y:Float,biome:Int,now:Long,walking:Boolean){
        val color=when(biome){
            1->intArrayOf(149,224,158)
            2->intArrayOf(249,157,108)
            3->intArrayOf(159,224,245)
            else->intArrayOf(242,199,137)
        }
        val flicker=sin(now/360.0).toFloat()
        c.save()
        c.clipRect(0f,17f,320f,193f)
        rect(c,x-21f,y-17f,42f,37f,
            Color.argb((10+flicker*2).toInt().coerceIn(7,13),
                color[0],color[1],color[2]))
        rect(c,x-13f,y-13f,26f,27f,
            Color.argb((13+flicker*3).toInt().coerceIn(9,17),
                color[0],color[1],color[2]))
        rect(c,x-7f,y-9f,14f,17f,
            Color.argb(11,color[0],color[1],color[2]))
        if(walking){
            val phase=(now/170)%4
            val dx=if(phase<2)-7f else 5f
            rect(c,x+dx,y+15f,5f,2f,0xA091939A.toInt())
            rect(c,x+dx+2f,y+13f,2f,2f,0x74969FA1.toInt())
        }
        c.restore()
    }

    fun heroShadow(c:Canvas,x:Float,y:Float,now:Long,walking:Boolean){
        val cycle=if(walking)sin(now/155.0).toFloat() else 0f
        rect(c,x-11f+cycle, y+13f,22f,3f,0x75080F1A.toInt())
    }
}
