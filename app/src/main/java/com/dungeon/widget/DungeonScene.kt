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
        val fighting=combatRoom&&h.enemyMaxHp>0&&h.pathStep>=encounter
        val victory=fighting&&h.enemyHp<=0
        val impact=fighting&&live&&h.lastDamage>0&&
            h.pathStep in encounter..(encounter+1)
        val hitAge=if(h.pathStep==encounter)eventAge else 1050+eventAge
        val showEnemy=!combatRoom||h.enemyHp>0||(victory&&impact&&hitAge<420)
        block(c,targetX-15,targetY+29,33f,4f,0x8A0B0E14.toInt())
        when(h.roomKind){
            "boss","battle"->if(showEnemy){
                val knock=if(impact && hitAge>140) (if(frame%4<2) 4f else -4f) else 0f
                if(h.elite){
                    block(c,targetX-22f,targetY-34f,44f,53f,0x33FFD46D)
                    for(i in 0..7){
                        val a=(i*19+frame%11)%31
                        dot(c,targetX-20f+i*5f,targetY-37f+a,
                            if(i%2==0)0xFFFFD875.toInt() else 0xFFA5F5FF.toInt(),3f)
                    }
                }
                enemy(c,targetX+knock,targetY,h,frame,accent)
                if(h.elite)txt(c,"ELITE",targetX-20f,targetY-58f,10f,
                    0xFFF6CD6D.toInt())
                if(h.roomKind=="boss"&&h.bossPhase>0){
                    block(c,targetX-26f,targetY-45f,52f,3f,0xFFF55C5A.toInt())
                    txt(c,"ФАЗА II",targetX-22f,targetY-63f,10f,0xFFFF7D72.toInt())
                }
                if(impact && hitAge<350)block(c,targetX-20,targetY-36,40f,56f,0x66FFFFFF)
                if(h.enemyMaxHp>0){
                    block(c,targetX-26f,targetY-48f,52f,5f,0xFF431E30.toInt())
                    block(c,targetX-25f,targetY-47f,
                        50f*h.enemyHp.coerceAtLeast(0)/h.enemyMaxHp.coerceAtLeast(1),3f,
                        if(h.roomKind=="boss")0xFFF1A15D.toInt() else 0xFFB4D984.toInt())
                }
                if(h.enemyWarning){
                    for(i in 0..3){
                        val dx=if(i%2==0)-21f else 18f
                        val dy=-28f+i*11f
                        dot(c,targetX+dx,targetY+dy,0xFFFF594A.toInt(),5f)
                    }
                    txt(c,"!",targetX-4f,targetY-56f,16f,0xFFFF624E.toInt())
                }
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
        hero(c,px,py,walking,frame,h,accent,h.event=="camp")
        if(walking && h.pathStep in (encounter-2)..encounter && combatRoom){
            if(impact)weaponFx(c,px,py,targetX,targetY,h,frame,accent)
            else{
                dot(c,px+18f,py-16f,accent,3f)
                dot(c,targetX-18f,targetY-23f,accent,2f)
            }
        }
        if(impact && h.lastAbility.isNotEmpty() && hitAge<1050){
            val skillColor=when(h.heroClass){
                0->0xFFB3E0EF.toInt()
                1->0xFFB8E79B.toInt()
                else->0xFFD4A9FA.toInt()
            }
            txt(c,"УМЕНИЕ!",px-25f,py-49f-hitAge/80f,12f,skillColor)
            for(i in 0..11){
                val dx=((i*17)%29-14).toFloat()
                val dy=((i*13)%27-13).toFloat()
                dot(c,px+dx,py-15f+dy,skillColor,if(i%3==0)5f else 3f)
            }
            if(h.heroClass==0){
                block(c,px-24f,py-27f,13f,31f,0xFFACC4E6.toInt())
                block(c,px-20f,py-23f,6f,23f,0xFFEAF2FA.toInt())
            }else if(h.heroClass==1){
                for(i in 0..2){
                    val progress=(frame%9).toFloat()/9f
                    block(c,px+(targetX-px)*progress,
                        py-18f+i*7f+(targetY-py)*progress,12f,2f,skillColor)
                }
            }else{
                block(c,targetX-19f,targetY-28f,38f,3f,skillColor)
                block(c,targetX-3f,targetY-44f,6f,33f,skillColor)
            }
        }
        if(impact && h.incomingDamage>0 && hitAge<750){
            txt(c,"-"+h.incomingDamage,px-15f,py-43f-hitAge/35f,13f,
                0xFFFF7380.toInt())
        }
        if(impact && h.lastDodge && hitAge<730){
            txt(c,"MISS",px-20f,py-43f-hitAge/33f,12f,0xFF9FE6E2.toInt())
        }
        if(impact){
            val elapsed=hitAge.coerceAtLeast(1)
            val raise=(elapsed/30f).coerceAtMost(30f)
            if(elapsed<1650){
                val critical=h.lastCritical
                val number=if(critical)"КРИТ -"+h.lastDamage else "-"+h.lastDamage
                val textX=targetX-(if(critical)33f else 19f)
                txt(c,number,textX+1f,targetY-39f-raise+1f,if(critical)14f else 15f,
                    0xFF131B26.toInt())
                txt(c,number,textX,targetY-39f-raise,if(critical)14f else 15f,
                    if(critical)0xFFFFCF69.toInt() else 0xFFFF8D79.toInt())
            }
            for(i in 0 until 12){
                val drift=(elapsed/65f).coerceAtMost(14f)
                val dx=((i*17)%13-6)*drift/6f
                val dy=((i*11)%11-5)*drift/7f
                val col=if(i%3==0)0xFFFFE5AF.toInt() else if(i%3==1)0xFFDC7773.toInt() else accent
                dot(c,targetX+dx,targetY-17f+dy,col,if(i%2==0)3f else 2f)
            }
            if(elapsed in 30..430){
                val length=28f+(elapsed/20f)
                block(c,targetX-23f,targetY-29f,length,3f,0xFFF3E8D9.toInt())
                block(c,targetX-18f,targetY-37f,3f,30f,0xFFFCE2AB.toInt())
            }
        }
        if(h.effectTicks>0 && live){
            val color=when(h.effectType){
                "poison"->0xFF78D97D.toInt()
                "burn"->0xFFFFA45A.toInt()
                "bleed"->0xFFF08B8E.toInt()
                else->0xFFA6B8EE.toInt()
            }
            dot(c,px-9f,py-35f-(frame%3)*3f,color,4f)
            dot(c,px+7f,py-32f-(frame%4)*2f,color,3f)
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
        val cameraZoom=zoom.coerceIn(1f,1.75f)
        val sourceWidth=(W/cameraZoom).toInt().coerceAtLeast(180)
        val sourceHeight=(176f/cameraZoom).toInt().coerceAtLeast(95)
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
        if(live)drawHud(out,layout,h,accent)
        b.recycle()
        return result
    }
    // Unobtrusive skill charge and actual explored map, drawn AFTER camera cropping.
    // Do not place it in the home-screen widget, where details would be too small.
    private fun drawHud(c:Canvas,room:RoomLayout,h:HeroState,accent:Int){
        block(c,6f,20f,88f,17f,0xD4141D28.toInt())
        txt(c,"НАВЫК",11f,31f,9f,0xFFF0D2A2.toInt())
        for(i in 0..3){
            block(c,45f+i*11f,26f,8f,5f,
                if(i<h.abilityMeter)accent else 0xFF49586B.toInt())
        }
        val x0=263f
        val y0=158f
        block(c,x0-3,y0-3,52f,31f,0xD2131D2A.toInt())
        for(y in 0..10)for(x in 0..19){
            if(room.isFloor(x,y)){
                block(c,x0+x*2.2f,y0+y*2.0f,2f,2f,0xFF697788.toInt())
            }
        }
        val path=room.path
        val where=path[h.pathStep.coerceIn(0,path.lastIndex)]
        block(c,x0+where.x*2.2f-1f,y0+where.y*2.0f-1f,4f,4f,accent)
        block(c,x0+18*2.2f,y0+8*2f,3f,3f,0xFF9BE6A3.toInt())
    }
    private fun bright(c:Int,a:Int)=Color.rgb(
        min(255,Color.red(c)+a),min(255,Color.green(c)+a),min(255,Color.blue(c)+a))
    private fun gearColor(rarity:Int,base:Int):Int{
        val palette=intArrayOf(
            0xFF9AA9B8.toInt(),0xFF6FAB9A.toInt(),0xFF669CCF.toInt(),
            0xFFAF79D9.toInt(),0xFFE4B65F.toInt()
        )
        return if(rarity>=0)palette[rarity.coerceIn(0,4)] else base
    }
    // Equipment-driven sprite: helm, chest, sleeves, gloves, boots and weapon
    // change in real-time when inventory equipment changes.
    private fun hero(c:Canvas,x:Float,y:Float,walk:Boolean,frame:Int,
        h:HeroState,accent:Int,camp:Boolean){
        val cl=h.heroClass
        val helmet=h.gear.firstOrNull{it.id==h.equipment["helmet"]}
        val chest=h.armorItem
        val gloves=h.gear.firstOrNull{it.id==h.equipment["gloves"]}
        val boots=h.gear.firstOrNull{it.id==h.equipment["boots"]}
        val amulet=h.gear.firstOrNull{it.id==h.equipment["amulet"]}
        val ring=h.gear.firstOrNull{it.id==h.equipment["ring"]||
            it.id==h.equipment["ring2"]}
        val chestCol=gearColor(chest?.rarity?:-1,when(cl){
            1->0xFF587B63.toInt();2->0xFF6B567E.toInt();else->0xFF748DA1.toInt()})
        val helmCol=gearColor(helmet?.rarity?:-1,when(cl){
            1->0xFF4B6655.toInt();2->0xFF5F5579.toInt();else->0xFF8E9EAC.toInt()})
        val gloveCol=gearColor(gloves?.rarity?:-1,0xFF8F775F.toInt())
        val bootCol=gearColor(boots?.rarity?:-1,
            if(cl==1)0xFF4A6756.toInt() else 0xFF414C62.toInt())
        val bob=if(walk&&frame%6<3)1f else 0f
        val leg=if(walk&&frame%8<4)3f else -2f
        block(c,x-14f,y+10f,28f,4f,0xAA0B0C10.toInt())
        if(chest!=null){
            block(c,x-13f,y-17f+bob,26f,27f,0xFF302837.toInt())
        }
        block(c,x-11f,y+2f,9f,11f+leg,bootCol)
        block(c,x+3f,y+2f,9f,11f-leg,bootCol)
        block(c,x-12f,y-17f+bob,24f,24f,chestCol)
        block(c,x-8f,y-14f+bob,16f,17f,
            if(chest==null)0xFFC0C9C7.toInt() else bright(chestCol,22))
        if(chest!=null){
            block(c,x-15f,y-19f+bob,8f,9f,chestCol)
            block(c,x+8f,y-19f+bob,8f,9f,chestCol)
            block(c,x-4f,y-10f+bob,8f,6f,0xFFE4D29B.toInt())
        }
        // Helmet silhouette changes from leather hood to metal crown and visor.
        block(c,x-10f,y-31f+bob,20f,17f,helmCol)
        if(helmet!=null){
            block(c,x-13f,y-34f+bob,26f,6f,helmCol)
            block(c,x-10f,y-37f+bob,20f,5f,
                if(helmet.rarity>=3)0xFFFFD17B.toInt() else helmCol)
            if("корона" in helmet.name.lowercase()||helmet.rarity>=4){
                for(i in -1..1)block(c,x+i*7f-2f,y-42f+bob,4f,8f,helmCol)
            }
        }
        block(c,x-8f,y-26f+bob,16f,8f,0xFFDDE3DE.toInt())
        block(c,x-7f,y-21f+bob,14f,4f,0xFF1B2636.toInt())
        dot(c,x-2f,y-21f+bob,accent,3f)
        block(c,x-18f,y-10f+bob,7f,19f,gloveCol)
        block(c,x-16f,y-7f+bob,6f,12f,bright(gloveCol,22))
        if(gloves!=null)block(c,x-19f,y+3f+bob,9f,6f,gloveCol)
        if(amulet!=null){
            block(c,x-2f,y-12f+bob,5f,7f,
                gearColor(amulet.rarity,0xFFE2B979.toInt()))
            dot(c,x,y-10f+bob,0xFFFFFFFF.toInt(),2f)
        }
        if(ring!=null&&ring.rarity>=3){
            dot(c,x-22f,y+3f+bob,0xFFE9C67F.toInt(),4f)
            dot(c,x-22f,y+3f+bob,0xFFFFFFFF.toInt(),2f)
        }
        // Base held weapon respects weapon type, not just class.
        val weaponName=(h.weaponItem?.name?:h.weaponName).lowercase()
        val style=when{
            "топор" in weaponName->"smash"
            "лук" in weaponName->"arrow"
            "посох" in weaponName||"жезл" in weaponName->"magic"
            else->if(cl==1)"arrow" else if(cl==2)"magic" else "slash"
        }
        val weaponColor=gearColor(h.weaponItem?.rarity?:-1,0xFFCED4D6.toInt())
        when(style){
            "arrow"->{
                block(c,x+15f,y-18f+bob,3f,30f,0xFFD3B58A.toInt())
                block(c,x+19f,y-17f+bob,2f,29f,weaponColor)
                block(c,x+12f,y-4f+bob,13f,2f,0xFFE3E3CC.toInt())
            }
            "magic"->{
                block(c,x+14f,y-19f+bob,4f,34f,0xFFA88B69.toInt())
                block(c,x+10f,y-25f+bob,12f,9f,weaponColor)
                dot(c,x+14f,y-23f+bob,0xFFEBF8FC.toInt(),4f)
            }
            "smash"->{
                block(c,x+15f,y-14f+bob,3f,29f,0xFFC2A27D.toInt())
                block(c,x+10f,y-19f+bob,14f,12f,weaponColor)
                block(c,x+10f,y-18f+bob,14f,3f,0xFFE3DBB8.toInt())
            }
            else->{
                block(c,x+14f,y-15f+bob,4f,31f,weaponColor)
                block(c,x+13f,y-19f+bob,6f,8f,0xFFE2EAF1.toInt())
                block(c,x+11f,y+7f+bob,10f,3f,0xFFCEAC73.toInt())
            }
        }
        if(camp)block(c,x-13f,y-41f,26f,3f,0xFF81BF9C.toInt())
    }
    private fun weaponFx(c:Canvas,x:Float,y:Float,tx:Float,ty:Float,
        h:HeroState,frame:Int,accent:Int){
        val color=if(h.lastCritical)0xFFFFD56D.toInt() else accent
        when(h.attackVisual){
            "smash"->{
                for(i in 0..3){
                    block(c,tx-19f+i*12f,ty+7f-(i%2)*7f,10f,4f,color)
                    block(c,tx-17f+i*12f,ty+8f,3f,10f,0xFF8D7656.toInt())
                }
                txt(c,"!",tx-4f,ty-38f,13f,color)
            }
            "arrow"->{
                val progress=(frame%8)/7f
                val bx=x+(tx-x)*progress
                val by=y-10f+(ty-y)*progress
                block(c,bx,by,17f,3f,0xFFE7EBE1.toInt())
                block(c,bx+14f,by-3f,5f,9f,color)
            }
            "magic"->{
                for(i in 0..6){
                    val progress=(i+(frame%6)).toFloat()/12f
                    val bx=x+(tx-x)*progress
                    val by=y-18f+(ty-y)*progress
                    dot(c,bx,by,if(i%2==0)color else 0xFFB8F5FF.toInt(),5f)
                }
                for(i in 0..3){
                    dot(c,tx-16f+i*10f,ty-30f+(i%2)*10f,color,4f)
                }
            }
            else->{
                for(i in 0..7){
                    val xi=tx-25f+i*7f
                    val yi=ty-39f+i*4f
                    block(c,xi,yi,8f,4f,if(i%2==0)color else 0xFFF3EEE2.toInt())
                }
            }
        }
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
