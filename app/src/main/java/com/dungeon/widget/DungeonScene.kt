package com.dungeon.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Rect
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
    fun render(h:HeroState,frame:Int=0,live:Boolean=false,zoom:Float=1f,transitionAge:Int=1000):Bitmap{
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
        val layout=DungeonMap.get(h.floor,h.room)
        val roomSeed=layout.seed
        for(y in 0 until 11)for(x in 0 until 20){
            val xx=x*16f;val yy=y*16f+17
            val open=layout.isFloor(x,y)
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
        // A hidden side-chamber appears in some rooms.
        layout.secret?.let{secret->
            val sx=secret.x*16f+5f;val sy=secret.y*16f+19f
            block(c,sx,sy,8f,7f,0xFF9C7F45.toInt())
            dot(c,sx+2f,sy+2f,0xFFF5D58A.toInt())
        }
        // A map of eight chambers.
        block(c,226f,19f,86f,25f,0xD00B121C.toInt())
        for(i in 0 until 8)block(c,234f+i*9f,26f,6f,8f,
            if(i<h.room-1)0xFF6EB38C.toInt() else if(i==h.room-1)accent else 0xFF3C4655.toInt())
        // Ground shadow of interactive object.
        val secretIndex=layout.secret?.let{layout.path.indexOf(it)}?:-1
        val encounter=if(h.roomKind=="secret"&&secretIndex>0)secretIndex
            else (layout.path.size*2/3).coerceAtLeast(8).coerceAtMost(layout.path.size-2)
        val target=layout.path[encounter]
        val targetX=target.x*16f+8f
        val targetY=17f+target.y*16f+5f
        val eventAge=if(live) (System.currentTimeMillis()-h.lastTick).coerceIn(0,1050).toInt() else 1000
        val combatRoom=h.roomKind=="battle"||h.roomKind=="boss"
        val battleFinished=h.pathStep>=encounter && h.event in listOf("battle","boss")
        val impact=battleFinished && h.pathStep==encounter && live && eventAge<930
        val showEnemy=!combatRoom || !battleFinished || (impact && eventAge<400)
        block(c,targetX-15,targetY+29,33f,4f,0x8A0B0E14.toInt())
        when(h.roomKind){
            "boss","battle"->if(showEnemy){
                val knock=if(impact && eventAge>140) (if(frame%4<2) 4f else -4f) else 0f
                enemy(c,targetX+knock,targetY,h,frame,accent)
                if(impact && eventAge<270)block(c,targetX-20,targetY-36,40f,56f,0x66FFFFFF)
            }
            "chest"->chest(c,targetX,targetY,frame,h.pathStep>=encounter)
            "shrine"->shrine(c,targetX,targetY,frame)
            "potion"->if(h.pathStep<encounter)potion(c,targetX,targetY,frame)
            "relic","secret"->shrine(c,targetX,targetY,frame)
            "merchant"->merchant(c,targetX,targetY,frame)
            "trap"->trap(c,targetX,targetY)
            else->enemy(c,targetX,targetY,h,frame,accent)
        }
        if(combatRoom && h.pathStep in (encounter-4)..(encounter-1)){
            if(h.roomKind=="boss"){
                block(c,targetX-22f,targetY-39f,44f,4f,0xFF321820.toInt())
                block(c,targetX-22f,targetY-39f,44f,4f,0xFFCE6B62.toInt())
            }
        }
        if(h.roomKind=="boss"&&h.pathStep in (encounter-5)..(encounter-1)){
            txt(c,"!",targetX-3f,targetY-48f,15f,0xFFFF7F72.toInt())
            block(c,targetX-16f,targetY+32f,32f,3f,0xFFC44E44.toInt())
        }
        // The hero moves along a BFS-computed route, including turns and detours.
        val ratio=if(live&&!h.paused) ((System.currentTimeMillis()-h.lastTick).coerceIn(0,DungeonGame.stepMillis()).toFloat()/DungeonGame.stepMillis()) else 0f
        val index=h.pathStep.coerceIn(0,layout.path.lastIndex)
        val current=layout.path[index]
        val next=layout.path[(index+1).coerceAtMost(layout.path.lastIndex)]
        val px=(current.x+(next.x-current.x)*ratio)*16f+8f
        val py=17f+(current.y+(next.y-current.y)*ratio)*16f+5f
        val walking=live&&!h.paused && h.recovery==0
        if(walking&&frame%4==0){
            dot(c,px-12f,py+14f,0xFF8D9297.toInt(),3f)
            dot(c,px+10f,py+12f,0xFF5C6A75.toInt(),2f)
        }
        hero(c,px,py,walking,frame,h.heroClass,accent,h.event=="camp")
        if(walking && h.pathStep in (encounter-2)..encounter && combatRoom){
            val blade=if(frame%4<2)0xFFFBE1AC.toInt() else 0xFFE8F8FF.toInt()
            block(c,px+11f,py-17f,17f,3f,blade)
            block(c,px+26f,py-20f,3f,12f,blade)
            dot(c,px+28f,py-23f,accent,3f)
        }
        if(impact){
            val elapsed=eventAge.coerceAtLeast(1)
            val raise=(elapsed/18f).coerceAtMost(30f)
            if(elapsed<780){
                val number=if("КРИТ" in h.log) "КРИТ -"+h.lastDamage else "-"+h.lastDamage
                txt(c,number,targetX-24f,targetY-44f-raise,if("КРИТ" in h.log)13f else 12f,
                    if("КРИТ" in h.log)0xFFFFCD61.toInt() else 0xFFFF8B76.toInt())
            }
            for(i in 0 until 12){
                val drift=(elapsed/65f).coerceAtMost(14f)
                val dx=((i*17)%13-6)*drift/6f
                val dy=((i*11)%11-5)*drift/7f
                val col=if(i%3==0)0xFFFFE5AF.toInt() else if(i%3==1)0xFFDC7773.toInt() else accent
                dot(c,targetX+dx,targetY-17f+dy,col,if(i%2==0)3f else 2f)
            }
            if(elapsed in 30..300){
                val length=28f+(elapsed/20f)
                block(c,targetX-23f,targetY-29f,length,3f,0xFFF3E8D9.toInt())
                block(c,targetX-18f,targetY-37f,3f,30f,0xFFFCE2AB.toInt())
            }
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
        if(zoom<=1.01f && transitionAge>=480)return b
        // Crop only the world. The floor and status information stay legible.
        val result=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888)
        val out=Canvas(result)
        val nearest=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
        val ratio=zoom.coerceIn(1f,1.75f)
        val sourceWidth=(W/ratio).toInt().coerceAtLeast(180)
        val sourceHeight=(176f/ratio).toInt().coerceAtLeast(95)
        val left=(px.toInt()-sourceWidth/2).coerceIn(0,W-sourceWidth)
        val top=(py.toInt()-sourceHeight/2).coerceIn(17,193-sourceHeight)
        out.drawBitmap(b,Rect(0,0,W,17),Rect(0,0,W,17),nearest)
        out.drawBitmap(b,Rect(left,top,left+sourceWidth,top+sourceHeight),
            Rect(0,17,W,193),nearest)
        out.drawBitmap(b,Rect(0,193,W,H),Rect(0,193,W,H),nearest)
        if(transitionAge<480){
            val remaining=(1f-transitionAge/480f).coerceIn(0f,1f)
            block(out,0f,17f,W.toFloat(),176f,Color.argb((remaining*220).toInt(),8,12,19))
            if(remaining>0.28f){
                txt(out,"КОМНАТА "+h.room,100f,103f,13f,accent)
            }
        }
        b.recycle()
        return result
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
        val species=h.enemy.lowercase()
        if(!boss&&("паук" in species||"волк" in species||"крыса" in species)){
            val beast=if("паук" in species)0xFF8F667F.toInt() else 0xFFA49A8F.toInt()
            block(c,x-16f,y-12f,32f,18f,beast)
            block(c,x-19f,y-17f,20f,12f,bright(beast,19))
            for(i in 0..3)block(c,x-13f+i*9f,y+5f,5f,13f,beast)
            dot(c,x-11f,y-13f,0xFFEE7373.toInt(),4f)
            dot(c,x-3f,y-13f,0xFFEE7373.toInt(),4f)
            if("паук" in species)for(i in 0..2){
                block(c,x-25f,y-8f+i*7f,10f,3f,beast)
                block(c,x+15f,y-8f+i*7f,10f,3f,beast)
            }
            return
        }
        if(!boss&&("призрак" in species||"дух" in species)){
            block(c,x-14f,y-25f,28f,34f,0xFF9FBBC8.toInt())
            block(c,x-10f,y-29f,20f,11f,0xFFD4ECF3.toInt())
            block(c,x-10f,y+9f,7f,9f,0xFF9FBBC8.toInt())
            block(c,x+4f,y+8f,7f,10f,0xFF9FBBC8.toInt())
            block(c,x-8f,y-17f,5f,7f,0xFF3F5177.toInt())
            block(c,x+4f,y-17f,5f,7f,0xFF3F5177.toInt())
            return
        }
        if(!boss&&("культист" in species||"лич" in species||"демон" in species)){
            block(c,x-14f,y-20f,28f,40f,0xFF6B516C.toInt())
            block(c,x-10f,y-30f,20f,18f,0xFF4A405C.toInt())
            block(c,x-8f,y-18f,16f,8f,0xFFC9A5A3.toInt())
            dot(c,x-5f,y-16f,0xFFDA7B63.toInt(),3f)
            dot(c,x+4f,y-16f,0xFFDA7B63.toInt(),3f)
            block(c,x+17f,y-24f,3f,40f,0xFFB3A17C.toInt())
            block(c,x+14f,y-29f,9f,9f,0xFFD97C72.toInt())
            return
        }
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
    private fun chest(c:Canvas,x:Float,y:Float,frame:Int,opened:Boolean){
        block(c,x-17f,y-7f,34f,24f,0xFF70452E.toInt())
        if(opened){
            block(c,x-17f,y-26f,34f,10f,0xFFAE753F.toInt())
            block(c,x-13f,y-24f,26f,6f,0xFFE0AC54.toInt())
            block(c,x-13f,y-8f,26f,8f,0xFFFADE8B.toInt())
            for(i in 0 until 5){
                val spread=(i-2)*9f
                dot(c,x+spread,y-35f+(if(frame%6<3)0f else 3f),0xFFFFE8A0.toInt(),3f)
            }
        }else{
            block(c,x-17f,y-12f,34f,11f,0xFFAE753F.toInt())
            block(c,x-13f,y-9f,26f,6f,0xFFE0AC54.toInt())
        }
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
