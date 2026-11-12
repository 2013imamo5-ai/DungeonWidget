package com.dungeon.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Pixel art item silhouettes drawn natively without external image assets. */
object PixelGearIcon {
    private val rarityColors=intArrayOf(
        0xFF98A0AD.toInt(),0xFF75C89A.toInt(),0xFF76A9ED.toInt(),
        0xFFBF84F0.toInt(),0xFFF2C36F.toInt())
    fun rarityColor(i:Int)=rarityColors[i.coerceIn(0,4)]
    fun create(g:Gear,size:Int=64):Bitmap{
        val bitmap=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val paint=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
        fun rect(x:Int,y:Int,w:Int,h:Int,c:Int){
            paint.color=c;canvas.drawRect(x.toFloat(),y.toFloat(),
                (x+w).toFloat(),(y+h).toFloat(),paint)
        }
        val bright=rarityColor(g.rarity)
        val edge=0xFF17212E.toInt()
        val metal=0xFFC8D7E3.toInt()
        val metalShadow=0xFF647F98.toInt()
        val wood=0xFF987453.toInt()
        val glow=0xFFFFE8B5.toInt()
        canvas.drawColor(Color.TRANSPARENT)
        rect(4,4,40,40,0xD01C2837.toInt())
        rect(7,7,34,34,0xFF29384B.toInt())
        rect(10,10,28,2,0xFF3A4B61.toInt())
        when(g.slot){
            "weapon"->{
                rect(23,5,5,29,edge)
                rect(24,5,3,24,metal)
                rect(25,7,1,19,glow)
                rect(20,28,12,4,edge)
                rect(21,29,10,2,bright)
                rect(24,32,3,10,wood)
                rect(23,40,5,3,bright)
            }
            "armor"->{
                rect(13,13,22,25,edge)
                rect(16,12,16,5,bright)
                rect(12,15,8,18,metalShadow)
                rect(29,15,8,18,metalShadow)
                rect(17,17,15,21,metal)
                rect(20,20,9,12,bright)
                rect(23,19,3,16,glow)
                rect(16,36,17,4,metalShadow)
            }
            "helmet"->{
                rect(14,11,21,20,edge)
                rect(16,10,17,19,metalShadow)
                rect(18,12,13,12,metal)
                rect(22,8,5,6,bright)
                rect(12,24,25,5,bright)
                rect(17,22,15,7,edge)
                rect(19,24,11,3,glow)
                rect(16,29,5,6,metalShadow)
                rect(28,29,5,6,metalShadow)
            }
            "gloves"->{
                rect(13,11,22,18,metalShadow)
                for(i in 0..3)rect(12+i*6,8,5,13,bright)
                rect(16,20,19,14,metal)
                rect(17,29,17,8,wood)
                rect(17,23,5,11,bright)
                rect(24,23,3,7,glow)
            }
            "boots"->{
                rect(13,10,9,22,metal)
                rect(26,10,9,22,metal)
                rect(12,15,11,4,bright)
                rect(25,15,11,4,bright)
                rect(10,29,14,9,edge)
                rect(25,29,15,9,edge)
                rect(10,32,14,5,wood)
                rect(25,32,15,5,wood)
                rect(9,37,17,3,bright)
                rect(24,37,17,3,bright)
            }
            "ring"->{
                rect(14,13,20,6,bright)
                rect(12,19,7,16,bright)
                rect(29,19,7,16,bright)
                rect(15,33,19,6,bright)
                rect(19,18,11,15,edge)
                rect(22,9,5,9,glow)
                rect(19,12,11,5,bright)
                rect(23,11,4,5,0xFFFFFFFF.toInt())
            }
            else->{
                rect(22,8,5,7,wood)
                rect(17,15,16,4,bright)
                rect(13,19,24,4,bright)
                rect(13,23,7,8,bright)
                rect(30,23,7,8,bright)
                rect(18,30,13,9,bright)
                rect(20,22,9,11,metal)
                rect(23,25,3,5,glow)
                rect(22,38,5,3,wood)
            }
        }
        if(g.rarity>=3){
            rect(38,5,2,7,bright);rect(35,8,8,2,bright)
            rect(6,36,2,7,bright);rect(3,39,8,2,bright)
        }
        val scaled=Bitmap.createScaledBitmap(bitmap,size,size,false)
        if(scaled!==bitmap)bitmap.recycle()
        return scaled
    }
}
