package com.dungeon.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object DungeonScene {
    private const val W=320
    private const val H=208
    private val p=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private fun color(hex:Long)=hex.toInt()
    private val floors=intArrayOf(0xFF343949.toInt(),0xFF283E36.toInt(),0xFF42302E.toInt(),0xFF2D4051.toInt())
    private val walls=intArrayOf(0xFF171E2B.toInt(),0xFF172921.toInt(),0xFF2B1D1C.toInt(),0xFF152333.toInt())
    private val accents=intArrayOf(0xFFF0AF57.toInt(),0xFF89D49B.toInt(),0xFFF08455.toInt(),0xFF83D8F0.toInt())
    private fun cellOpen(x:Int,y:Int):Boolean{
        if(x !in 1..18||y !in 1..9)return false
        return (y in 4..6 && x in 1..18) ||
            (x in 3..5 && y in 2..8) ||
            (x in 10..12 && y in 2..8) ||
            (x in 15..17 && y in 2..9) ||
            (y in 2..3 && x in 3..17) ||
            (y in 7..8 && x in 3..17)
    }
    private fun block(c:Canvas,x:Float,y:Float,w:Float,h:Float,col:Int){
        p.color=col;p.style=Paint.Style.FILL;c.drawRect(x,y,x+w,y+h,p)
    }
    private fun dot(c:Canvas,x:Float,y:Float,col:Int,size:Float=2f)=block(c,x,y,size,size,col)
    private fun txt(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int){
        p.color=col;p.textSize=size;p.typeface=Typeface.create("monospace",Typeface.BOLD)
        p.isAntiAlias=false;c.drawText(s,x,y,p)
    }
    fun render(h:HeroState,frame:Int=0,live:Boolean=false):Bitmap{
        val b=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888)
        val c=Canvas(b)
        val biome=h.biomeId
        val floorColor=floors[biome];val wallColor=walls[biome]
        val accent=accents[biome]
        val dark=if(h.theme==1)0xFF0B121C.toInt() else if(h.theme==2)0xFF182A2C.toInt() else 0xFF10141D.toInt()
        c.drawColor(dark)
        block(c,0f,0f,320f,17f,0xFF0A1119.toInt())
        txt(c,"F"+h.floor+"  "+h.biome.uppercase().take(20),9f,12f,10f,accent)
        txt(c,"R"+h.room+"/8",273f,12f,10f,0xFFE1DCC9.toInt())
        val roomSeed=(h.floor*71+h.room*29)
        for(y in 0 until 11)for(x in 0 until 20){
            val xx=x*16f;val yy=y*16f+17
            val open=cellOpen(x,y)
            val noise=(x*37+y*79+roomSeed)%11
            if(open){
                block(c,xx,yy,16f,16f,if(noise>6)bright(floorColor,9)else floorColor)
                block(c,xx,yy,15f,1f,0x44000000)
                block(c,xx,yy,1f,16f,0x33000000)
                if(noise==3)dot(c,xx+5,yy+9,bright(floorColor,24),2f)
                if(noise==6){dot(c,xx+2,yy+3,bright(floorColor,16));dot(c,xx+11,yy+12,0xFF202630.toInt())}
            }else{
                block(c,xx,yy,16f,16f,wallColor)
                if(x in 1..18 && y in 1..9){
                    block(c,xx+1f,yy+1f,14f,4f,bright(wallColor,25))
                    block(c,xx+1f,yy+6f,14f,9f,bright(wallColor,9))
                    if(noise%3==0)block(c,xx+8f,yy+7f,1f,7f,wallColor)
                }
            }
        }
        // Doors are part of the traversable route.
        block(c,16f,97f,5f,29f,0xFF9B714C.toInt())
        block(c,18f,102f,3f,18f,0xFFD5A669.toInt())
        block(c,291f,136f,12f,23f,0xFF8B654D.toInt())
        block(c,294f,139f,6f,16f,0xFFD9B377.toInt())
        // Decorations vary with the biome.
        for(i in 0..3){
            val x=52f+i*64
            val y=34f+(i%2)*92
            when(biome){
                0->{
                    block(c,x-3,y,6f,13f,0xFF857E8D.toInt())
                    block(c,x-6,y-5,12f,7f,0xFFFFAF43.toInt())
                    block(c,x-2,y-8,4f,11f,0xFFFFE39A.toInt())
                }
                1->{
                    block(c,x-2,y+4,4f,12f,0xFFB9A79A.toInt())
                    block(c,x-9,y-3,18f,10f,0xFFB66694.toInt())
                    dot(c,x-4,y-1,0xFFFFD8EA.toInt())
                    dot(c,x+4,y+1,0xFFFFD8EA.toInt())
                }
                2->{
                    block(c,x-4,y,8f,18f,0xFF4D292A.toInt())
                    block(c,x-7,y-6,14f,10f,0xFFEF713B.toInt())
                    block(c,x-3,y-10,6f,8f,0xFFFFB45E.toInt())
                }
                else->{
                    block(c,x-5,y-6,10f,24f,0xFF4D91B6.toInt())
                    block(c,x-2,y-11,5f,6f,0xFFB2EFFF.toInt())
                    block(c,x+4,y+6,3f,6f,0xFF9ED8F7.toInt())
                }
            }
        }
        // Side room furniture, rugs, and banners.
        block(c,104f,57f,30f,15f,if(biome==2)0xFF8C4B40.toInt() else 0xFF5C455F.toInt())
        block(c,109f,57f,21f,15f,if(biome==2)0xFFC47A5A.toInt() else 0xFF866A83.toInt())
        block(c,39f,57f,10f,16f,0xFF514A4D.toInt())
        block(c,40f,53f,8f,5f,0xFF8E8679.toInt())
        // A map of eight chambers.
        block(c,226f,19f,86f,25f,0xD00B121C.toInt())
        for(i in 0 until 8)block(c,234f+i*9f,26f,6f,8f,
            if(i<h.room-1)0xFF6EB38C.toInt() else if(i==h.room-1)accent else 0xFF3C4655.toInt())
        // Ground shadow of interactive object.
        val targetX=221f;val targetY=103f
        block(c,targetX-15,targetY+29,33f,4f,0x8A0B0E14.toInt())
        when(h.roomKind){
            "boss","battle"->enemy(c,targetX,targetY,h,frame,accent)
            "chest"->chest(c,targetX,targetY,frame)
            "shrine"->shrine(c,targetX,targetY,frame)
            "potion"->potion(c,targetX,targetY,frame)
            "relic"->shrine(c,targetX,targetY,frame)
            "merchant"->merchant(c,targetX,targetY,frame)
            "trap"->trap(c,targetX,targetY)
            else->enemy(c,targetX,targetY,h,frame,accent)
        }
        // The hero follows an actual corridor, then turns towards the exit.
        val ratio=if(live&&!h.paused) ((System.currentTimeMillis()-h.lastTick).coerceIn(0,DungeonGame.stepMillis()).toFloat()/DungeonGame.stepMillis()) else 0f
        val step=h.pathStep.toFloat()+ratio
        val tx=if(step<15f)2f+step else 17f
        val ty=if(step<15f)5f else 5f+(step-15f).coerceAtMost(3f)
        val px=tx*16f+8f
        val py=17f+ty*16f+5f
        val walking=live&&!h.paused && h.recovery==0
        hero(c,px,py,walking,frame,h.heroClass,accent,h.event=="camp")
        if(walking&&h.pathStep in 10..13&&h.roomKind in listOf("battle","boss")){
            val blade=if(frame%4<2)0xFFFBE1AC.toInt() else 0xFFE8F8FF.toInt()
            block(c,px+11f,py-17f,17f,3f,blade)
            block(c,px+26f,py-20f,3f,12f,blade)
            dot(c,px+28f,py-23f,accent,3f)
        }
        if(h.event=="level"||h.event=="relic"){
            val sparkleX=px+((frame%5)*3)-6
            dot(c,sparkleX,py-30f-(frame%3),accent,3f)
        }
        // Tiny ground HP meter, visible within a widget.
        block(c,px-13f,py+11f,26f,3f,0xFF371C25.toInt())
        block(c,px-13f,py+11f,26f*h.hp.coerceAtLeast(0)/h.maxHp.coerceAtLeast(1),3f,0xFF66CB84.toInt())
        block(c,0f,193f,320f,15f,0xFF0B111A.toInt())
        txt(c,"HP "+h.hp+"/"+h.maxHp+"   LV "+h.level+"   GOLD "+h.gold,7f,204f,9f,0xFFE6DCC8.toInt())
        return b
    }
    private fun bright(c:Int,a:Int)=Color.rgb(
        min(255,Color.red(c)+a),min(255,Color.green(c)+a),min(255,Color.blue(c)+a))
    private fun hero(c:Canvas,x:Float,y:Float,walk:Boolean,frame:Int,cl:Int,accent:Int,camp:Boolean){
        val bob=if(walk&&frame%6<3)1f else 0f
        val leg=if(walk&&frame%8<4)3f else -2f
        block(c,x-13f,y+8f,26f,4f,0xAA0B0C10.toInt())
        val legC=if(cl==1)0xFF4A6756.toInt() else 0xFF414C62.toInt()
        block(c,x-10f,y+2f,8f,10f+leg,legC)
        block(c,x+3f,y+2f,8f,10f-leg,legC)
        block(c,x-12f,y-17f+bob,24f,23f,when(cl){
            1->0xFF587B63.toInt();2->0xFF6B567E.toInt();else->0xFF748DA1.toInt()})
        block(c,x-7f,y-13f+bob,15f,17f,when(cl){
            1->0xFF90AF88.toInt();2->0xFFA190C0.toInt();else->0xFFBAC9CB.toInt()})
        block(c,x-10f,y-31f+bob,20f,17f,when(cl){
            1->0xFF4B6655.toInt();2->0xFF5F5579.toInt();else->0xFF8E9EAC.toInt()})
        block(c,x-8f,y-27f+bob,16f,9f,0xFFDDE3DE.toInt())
        block(c,x-7f,y-21f+bob,14f,4f,0xFF1B2636.toInt())
        dot(c,x-2f,y-21f+bob,accent,3f)
        block(c,x-18f,y-10f+bob,7f,20f,0xFF6A544A.toInt())
        block(c,x-16f,y-8f+bob,6f,14f,0xFFBDA16C.toInt())
        if(cl==1){
            block(c,x+14f,y-12f,2f,25f,0xFFBCA578.toInt())
            block(c,x+15f,y-10f,6f,18f,accent)
        }else if(cl==2){
            block(c,x+12f,y-19f,3f,30f,0xFFB7A076.toInt())
            block(c,x+9f,y-24f,9f,8f,accent)
        }else{
            block(c,x+13f,y-14f+bob,3f,28f,0xFFE3E8E7.toInt())
            block(c,x+10f,y+6f+bob,10f,3f,0xFFCEAC73.toInt())
        }
        if(camp)block(c,x-13f,y-34f,26f,3f,0xFF81BF9C.toInt())
    }
    private fun enemy(c:Canvas,x:Float,y:Float,h:HeroState,frame:Int,accent:Int){
        val boss=h.roomKind=="boss"
        val bigger=if(boss)5f else 0f
        val wob=if(frame%12<6)0f else 2f
        val body=when(h.biomeId){
            0->0xFFD9D4BB.toInt();1->0xFF84BF98.toInt()
            2->0xFFCB6D63.toInt();else->0xFFA6D7EB.toInt()
        }
        block(c,x-11f-bigger,y+8f,7f+bigger,15f,body)
        block(c,x+4f,y+8f,7f+bigger,15f,body)
        block(c,x-14f-bigger,y-10f+wob,28f+bigger*2,20f,body)
        block(c,x-14f-bigger,y-26f+wob,28f+bigger*2,17f,if(boss)0xFF75435A.toInt() else bright(body,20))
        block(c,x-9f,y-19f+wob,5f,5f,0xFF1D2931.toInt())
        block(c,x+4f,y-19f+wob,5f,5f,0xFF1D2931.toInt())
        block(c,x-4f,y-11f+wob,8f,3f,0xFF37313B.toInt())
        if(boss){
            block(c,x-22f,y-29f,9f,10f,accent)
            block(c,x+13f,y-29f,9f,10f,accent)
            block(c,x-17f,y-39f,35f,6f,0xFF5D2337.toInt())
            block(c,x-16f,y-38f,25f,3f,0xFFCB6B63.toInt())
        }
    }
    private fun chest(c:Canvas,x:Float,y:Float,frame:Int){
        block(c,x-17f,y-7f,34f,24f,0xFF70452E.toInt())
        block(c,x-17f,y-12f,34f,11f,0xFFAE753F.toInt())
        block(c,x-13f,y-9f,26f,6f,0xFFE0AC54.toInt())
        block(c,x-4f,y-1f,8f,12f,0xFFE7CC7E.toInt())
        if(frame%12<6)dot(c,x+19f,y-12f,0xFFFFD478.toInt(),3f)
    }
    private fun shrine(c:Canvas,x:Float,y:Float,frame:Int){
        block(c,x-14f,y+1f,28f,18f,0xFF626B7C.toInt())
        block(c,x-9f,y-8f,18f,13f,0xFF8797B1.toInt())
        block(c,x-4f,y-27f+(if(frame%8<4)1f else 0f),9f,20f,0xFF6EE1CF.toInt())
        block(c,x-2f,y-32f,5f,6f,0xFFD2FFF3.toInt())
    }
    private fun potion(c:Canvas,x:Float,y:Float,frame:Int){
        block(c,x-9f,y-16f,18f,29f,0xFF9FBCCD.toInt())
        block(c,x-7f,y-6f,14f,17f,0xFF5DD5AC.toInt())
        block(c,x-5f,y-21f,10f,6f,0xFFD9D7A9.toInt())
    }
    private fun merchant(c:Canvas,x:Float,y:Float,frame:Int){
        block(c,x-16f,y-14f,32f,30f,0xFF6F4F6D.toInt())
        block(c,x-13f,y-30f,26f,19f,0xFF9D7195.toInt())
        block(c,x-8f,y-23f,16f,10f,0xFFCEB99B.toInt())
        block(c,x-6f,y-20f,4f,4f,0xFF2A2732.toInt())
        block(c,x+3f,y-20f,4f,4f,0xFF2A2732.toInt())
        block(c,x+12f,y+10f,15f,9f,0xFFD9B866.toInt())
    }
    private fun trap(c:Canvas,x:Float,y:Float){
        block(c,x-20f,y+3f,40f,13f,0xFF513A40.toInt())
        for(i in 0..3)block(c,x-16f+i*10f,y-7f,6f,12f,0xFFACADB9.toInt())
    }
}
