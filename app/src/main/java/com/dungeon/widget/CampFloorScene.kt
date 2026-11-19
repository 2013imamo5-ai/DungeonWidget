package com.dungeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.sin

/**
 * Real generated pixel-art sprites compose the permanent sanctuary backdrop:
 * stone floor and walls, a central campfire, portal arch, forge, healer,
 * scouting desk, quarters, carpets, crystals and furniture. The backdrop is
 * stored as an offline WebP resource. Workers, hero and magical particles
 * are drawn independently over it, without changing expedition state.
 */
object CampFloorScene {
    private const val W=320
    private const val H=208
    private val pixel=Paint().apply { isAntiAlias=false;isFilterBitmap=false }
    private val imagePaint=Paint().apply { isAntiAlias=false;isFilterBitmap=true }
    private var campBackdrop:Bitmap?=null
    private var exitStartedAt:Long=0L

    fun beginExit(){ exitStartedAt=System.currentTimeMillis() }
    fun cancelExit(){ exitStartedAt=0L }

    @Synchronized
    private fun sceneBitmap(ctx:Context):Bitmap?{
        val loaded=campBackdrop
        if(loaded!=null && !loaded.isRecycled)return loaded
        val options=BitmapFactory.Options().apply{
            inScaled=false
            inPreferredConfig=Bitmap.Config.ARGB_8888
        }
        campBackdrop=BitmapFactory.decodeResource(
            ctx.applicationContext.resources,R.drawable.camp_sprite_scene,options)
        return campBackdrop
    }

    private fun rect(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){
        pixel.color=color
        c.drawRect(x,y,x+w,y+h,pixel)
    }

    private fun label(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int){
        pixel.color=color;pixel.textSize=size
        pixel.typeface=Typeface.create("monospace",Typeface.BOLD)
        c.drawText(s,x,y,pixel)
    }

    private fun worker(c:Canvas,x:Float,y:Float,skin:Int,coat:Int,frame:Int,
        isSmith:Boolean=false){
        val sway=sin(frame*.14).toFloat()
        val yBob=if(frame==0)0f else sin(frame*.08).toFloat()*.6f
        rect(c,x-6f,y+2f,12f,3f,0x99111A23.toInt())
        rect(c,x-5f,y-9f+yBob,10f,12f,coat)
        rect(c,x-4f,y-15f+yBob,8f,7f,skin)
        rect(c,x-5f,y-17f+yBob,10f,3f,0xFF513C37.toInt())
        rect(c,x-4f,y+1f,3f,4f,0xFF31394A.toInt())
        rect(c,x+1f,y+1f,3f,4f,0xFF31394A.toInt())
        if(isSmith){
            val raise=if(frame%36<18)8f else 1f
            rect(c,x+6f,y-13f-raise,2f,10f,0xFFB4A8A0.toInt())
            rect(c,x+3f,y-14f-raise,8f,3f,0xFFD9B68A.toInt())
            if(frame%36<7){
                rect(c,x+11f,y-12f,3f,2f,0xFFFFBE66.toInt())
                rect(c,x+14f,y-14f,2f,2f,0xFFFFE4A3.toInt())
            }
        }else{
            rect(c,x-7f,y-7f+sway,3f,7f,skin)
            rect(c,x+4f,y-8f-sway,3f,8f,skin)
        }
    }

    private fun hero(c:Canvas,x:Float,y:Float,frame:Int,moving:Boolean){
        val stride=if(moving)sin(frame*.33).toFloat()*1.5f else 0f
        val bob=if(moving)kotlin.math.abs(stride)*.45f else 0f
        // Sword, cape and face stay legible at 320 x 208 and in the widget.
        rect(c,x-7f,y+3f,16f,3f,0xA0101420.toInt())
        rect(c,x-7f,y-12f+bob,14f,15f,0xFF7B3545.toInt())
        rect(c,x-5f,y-13f+bob,10f,13f,0xFF57748A.toInt())
        rect(c,x-7f,y-9f+bob,4f,10f,0xFFB46355.toInt())
        rect(c,x-5f,y-20f+bob,10f,8f,0xFFD9B18B.toInt())
        rect(c,x-6f,y-22f+bob,12f,5f,0xFF583A30.toInt())
        rect(c,x+2f,y-17f+bob,2f,2f,0xFF2B303A.toInt())
        rect(c,x-5f,y,4f,6f+stride,0xFF364259.toInt())
        rect(c,x+2f,y,4f,6f-stride,0xFF364259.toInt())
        rect(c,x+7f,y-14f,2f,15f,0xFFD7E6E8.toInt())
        rect(c,x+6f,y-17f,4f,5f,0xFFF1F4E9.toInt())
        rect(c,x+4f,y-3f,8f,2f,0xFFE2AC71.toInt())
    }

    fun render(h:HeroState,frame:Int,live:Boolean,context:Context?=null):Bitmap{
        val bmp=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888)
        val c=Canvas(bmp)
        val image=context?.let{runCatching{sceneBitmap(it)}.getOrNull()}
        if(image!=null){
            c.drawBitmap(image,null,Rect(0,0,W,H),imagePaint)
        }else{
            // Compatibility fallback if the art resource cannot be loaded.
            c.drawColor(0xFF292B3A.toInt())
            for(y in 0..12)for(x in 0..19){
                rect(c,x*16f,y*16f,15f,15f,
                    if((x+y)%3==0)0xFF484456.toInt()
                    else 0xFF3B3D4D.toInt())
            }
            rect(c,124f,6f,72f,56f,0xFF31658D.toInt())
            rect(c,139f,22f,42f,30f,0xFF56B0D6.toInt())
            rect(c,139f,112f,42f,31f,0xFFE69A4E.toInt())
            label(c,"ЛАГЕРЬ",14f,14f,12f,0xFFFFD7A0.toInt())
        }

        // Warm embers rise over the static painted campfire.
        if(live){
            for(i in 0..10){
                val t=(frame*1.5f+i*12f)%52f
                val xx=159f+sin(frame*.06+i*1.7).toFloat()*13f
                val yy=123f-t*.60f
                rect(c,xx,yy,if(i%3==0)2f else 1f,2f,
                    if(i%3==0)0xFFFFD18B.toInt()
                    else 0xFFFF975D.toInt())
            }
            // The portal's blue shimmer is intentionally restrained so the
            // original blue vortex sprite remains visible.
            if(frame%24<9){
                rect(c,157f,16f,2f,3f,0xFFB3EDFF.toInt())
                rect(c,170f,29f,2f,2f,0xFF8BE6FD.toInt())
            }
        }

        // Small visual details unlock with camp upgrades. Gameplay values
        // remain the existing HeroState stats and are not touched here.
        if(h.forge>=2)rect(c,96f,79f,4f,3f,0xFFFFCC80.toInt())
        if(h.forge>=5)rect(c,84f,84f,3f,2f,0xFFFFF1BF.toInt())
        if(h.healer>=2)rect(c,260f,81f,4f,3f,0xFF9FE2A2.toInt())
        if(h.scouts>=2)rect(c,91f,166f,4f,3f,0xFFF1CA7D.toInt())
        if(h.camp>=3)rect(c,274f,155f,3f,4f,0xFFFFD48A.toInt())

        worker(c,114f,100f,0xFFD3A986.toInt(),0xFF62616C.toInt(),
            if(live)frame else 0,true)
        worker(c,211f,102f,0xFFE1C4A0.toInt(),0xFF51826D.toInt(),
            if(live)frame+13 else 0)
        worker(c,124f,168f,0xFFD7BC97.toInt(),0xFF7D6D53.toInt(),
            if(live)frame+27 else 0)
        worker(c,211f,172f,0xFFE3C49D.toInt(),0xFF666381.toInt(),
            if(live)frame+4 else 0)

        val route=arrayOf(
            floatArrayOf(160f,179f),floatArrayOf(125f,178f),
            floatArrayOf(118f,125f),floatArrayOf(160f,180f),
            floatArrayOf(209f,122f),floatArrayOf(211f,181f),
            floatArrayOf(159f,178f))
        val now=System.currentTimeMillis()
        val outgoing=exitStartedAt>0L && now-exitStartedAt in 0L..1800L
        val hx:Float
        val hy:Float
        if(outgoing){
            val t=((now-exitStartedAt)/1000f).coerceIn(0f,1f)
            when{
                t<.26f->{
                    val q=t/.26f
                    hx=160f+(210f-160f)*q;hy=179f+(169f-179f)*q
                }
                t<.73f->{
                    val q=(t-.26f)/.47f
                    hx=210f;hy=169f+(68f-169f)*q
                }
                else->{
                    val q=(t-.73f)/.27f
                    hx=210f+(160f-210f)*q;hy=68f
                }
            }
        }else if(live){
            val pos=(frame/66f)%route.size
            val a=pos.toInt()
            val b=(a+1)%route.size
            val t=pos-a
            val eased=t*t*(3f-2f*t)
            hx=route[a][0]+(route[b][0]-route[a][0])*eased
            hy=route[a][1]+(route[b][1]-route[a][1])*eased
        }else{hx=160f;hy=178f}
        hero(c,hx,hy,frame,live||outgoing)

        // Tiny native label indicates floor 0 without obscuring the sprite
        // art or repeating the main screen's large navigation panel.
        rect(c,5f,5f,57f,15f,0xD0141927.toInt())
        label(c,"ЭТАЖ 0",11f,15f,9f,0xFFFFDC9C.toInt())
        return bmp
    }
}
