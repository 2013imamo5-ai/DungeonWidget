package com.dungeon.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

object DungeonScene {
    private const val W = 320
    private const val H = 168
    fun render(h: HeroState,frame: Int=0): Bitmap {
        val bmp = Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias=false }
        fun rect(x:Float,y:Float,w:Float,hh:Float,color:Int) {
            p.color=color;c.drawRect(x,y,x+w,y+hh,p)
        }
        fun rgb(code:Long)=code.toInt()
        c.drawColor(Color.rgb(17,23,32))
        for(y in 0 until 11) for(x in 0 until 20) {
            val n=(x*17+y*23+h.floor*7)%5
            rect(x*16f,y*16f,15f,15f,if(n<2) Color.rgb(41,48,59) else Color.rgb(47,54,65))
            if(n==0) rect(x*16f+3,y*16f+4,6f,2f,Color.rgb(55,62,74))
        }
        rect(0f,0f,320f,8f,Color.rgb(91,91,96))
        rect(0f,160f,320f,8f,Color.rgb(91,91,96))
        rect(0f,0f,8f,168f,Color.rgb(91,91,96))
        rect(312f,0f,8f,168f,Color.rgb(91,91,96))
        for(x in listOf(36,280)) {
            rect(x-6f,19f,12f,25f,Color.rgb(69,67,65))
            rect(x-8f,13f,16f,14f,Color.rgb(247,128,42))
            rect(x-4f,9f,8f,15f,Color.rgb(255,202,81))
            rect(x-2f,15f,4f,8f,Color.rgb(255,244,170))
        }
        val bob=if(frame%2==0) 0f else 3f
        // Knight: chunky hand-painted pixel blocks
        val x=95f+((frame%4)*2)
        val y=86f+bob
        rect(x-16,y+37,14f,15f,Color.rgb(56,61,76))
        rect(x+4,y+37,14f,15f,Color.rgb(56,61,76))
        rect(x-20,y+2,40f,40f,Color.rgb(108,124,143))
        rect(x-13,y+10,26f,27f,Color.rgb(166,180,188))
        rect(x-17,y-20,34f,27f,Color.rgb(114,130,146))
        rect(x-12,y-14,24f,15f,Color.rgb(201,207,206))
        rect(x-10,y-5,20f,6f,Color.rgb(28,34,43))
        rect(x-4,y-5,8f,5f,Color.rgb(245,193,93))
        rect(x+21,y+3,5f,42f,Color.rgb(197,201,213))
        rect(x+17,y+31,15f,4f,Color.rgb(214,157,70))
        rect(x-29,y+11,15f,27f,Color.rgb(115,82,58))
        rect(x-25,y+16,7f,12f,Color.rgb(182,150,99))
        // Skeleton
        val ex=225f
        rect(ex-15,89f,30f,26f,Color.rgb(194,188,168))
        rect(ex-17,75f,34f,27f,Color.rgb(214,211,192))
        rect(ex-11,87f,7f,7f,Color.rgb(33,28,31))
        rect(ex+4,87f,7f,7f,Color.rgb(33,28,31))
        rect(ex-5,96f,10f,4f,Color.rgb(66,62,63))
        rect(ex-12,111f,8f,25f,Color.rgb(176,167,146))
        rect(ex+4,111f,8f,25f,Color.rgb(176,167,146))
        rect(ex-20,105f,40f,5f,Color.rgb(153,148,139))
        // Chest
        rect(264f,125f,32f,23f,Color.rgb(94,54,32))
        rect(264f,123f,32f,9f,Color.rgb(161,105,48))
        rect(277f,130f,7f,12f,Color.rgb(233,183,77))
        return bmp
    }
}
