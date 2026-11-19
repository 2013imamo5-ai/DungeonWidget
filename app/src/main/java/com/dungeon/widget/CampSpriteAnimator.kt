package com.dungeon.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated layers cut from the actual generated camp pixel art.
 * The stone architecture stays still; the portal spiral and two fires
 * are extracted once from the backdrop and animated independently.
 * No extra bitmap decoding, allocations or saved-state mutations per frame.
 */
internal object CampSpriteAnimator {
    private const val WIDTH=320
    private const val HEIGHT=208
    private val nearest=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private val pixels=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private val full=Rect(0,0,WIDTH,HEIGHT)
    private val portalClip=Path().apply{
        addCircle(160f,30f,17.5f,Path.Direction.CW)
    }
    private data class Layers(
        val original:Bitmap,
        val floor:Bitmap,
        val portal:Bitmap,
        val campFlame:Bitmap,
        val forgeFlame:Bitmap
    )
    @Volatile private var prepared:Layers?=null

    @Synchronized
    private fun layers(source:Bitmap):Layers{
        val cached=prepared
        if(cached!=null && cached.original===source)return cached
        if(source.width!=WIDTH || source.height!=HEIGHT){
            val result=Layers(source,source,source,source,source)
            prepared=result
            return result
        }
        val original=IntArray(WIDTH*HEIGHT)
        source.getPixels(original,0,WIDTH,0,0,WIDTH,HEIGHT)
        val base=original.copyOf()
        val portal=IntArray(original.size)
        val fire=IntArray(original.size)
        val forge=IntArray(original.size)
        for(y in 0 until HEIGHT)for(x in 0 until WIDTH){
            val index=y*WIDTH+x
            val argb=original[index]
            val red=Color.red(argb)
            val green=Color.green(argb)
            val blue=Color.blue(argb)
            val dx=(x-160f)/17.5f
            val dy=(y-30f)/17.5f
            if(dx*dx+dy*dy<=1f){
                // Rotate this exact vortex sprite without turning its stone arch.
                portal[index]=argb
                base[index]=Color.rgb(12,31,71)
            }
            val hot=red>170 && green>70 &&
                red>green*1.07 && green>blue*1.42
            val camp=x in 146..175 && y in 108..138
            val smith=x in 33..64 && y in 75..101
            if(hot && (camp||smith)){
                if(camp)fire[index]=argb
                if(smith)forge[index]=argb
                // The removed glow becomes the dark hearth underneath
                // the moving flame sprite, so it doesn't ghost in place.
                base[index]=Color.rgb(
                    (red*.35f).toInt().coerceIn(0,255),
                    (green*.27f).toInt().coerceIn(0,255),
                    (blue*.4f).toInt().coerceIn(0,255))
            }
        }
        fun bitmap(data:IntArray)=Bitmap.createBitmap(
            data,WIDTH,HEIGHT,Bitmap.Config.ARGB_8888)
        val result=Layers(source,bitmap(base),bitmap(portal),
            bitmap(fire),bitmap(forge))
        prepared=result
        return result
    }

    private fun px(c:Canvas,x:Float,y:Float,size:Float,color:Int){
        pixels.color=color
        c.drawRect(x,y,x+size,y+size,pixels)
    }
    private fun light(c:Canvas,x:Float,y:Float,radius:Float,
        red:Int,green:Int,blue:Int,phase:Float){
        val drift=sin(phase.toDouble()).toFloat()*.24f
        for(i in 3 downTo 1){
            val radiusScale=radius*(.40f+i*.22f)
            pixels.color=Color.argb(
                (8+drift*13f+((4-i)*3)).toInt().coerceIn(2,38),
                red,green,blue)
            c.drawCircle(x,y,radiusScale,pixels)
        }
    }
    private fun bobFlame(c:Canvas,bmp:Bitmap,x:Float,bottom:Float,
        phase:Float,small:Boolean){
        c.save()
        val sway=sin(phase.toDouble()).toFloat()
        c.translate(sway*(if(small)1.0f else 1.6f),0f)
        val width=1f+sin((phase*1.63f).toDouble()).toFloat()*
            (if(small).075f else .11f)
        val height=1.05f+sin((phase*1.22f+1.0f).toDouble()).toFloat()*
            (if(small).08f else .15f)
        c.scale(width,height,x,bottom)
        c.drawBitmap(bmp,0f,0f,nearest)
        c.restore()
    }
    private fun renderPortal(c:Canvas,bitmap:Bitmap,time:Long){
        val spin=(time%8500L).toFloat()/8500f*360f
        c.save()
        c.clipPath(portalClip)
        c.rotate(spin,160f,30f)
        c.drawBitmap(bitmap,0f,0f,nearest)
        c.restore()
        val wave=(time%2400L).toFloat()/2400f
        light(c,160f,30f,19f,66,170,255,wave*6.28318f)
        // Travelling arcane motes, not randomly appearing still pixels.
        for(i in 0..4){
            val angle=(time%5200L).toDouble()/5200.0*6.28318+
                i*1.256636
            val x=160f+cos(angle).toFloat()*19f
            val y=30f+sin(angle).toFloat()*18f
            px(c,x,y,if(i%2==0)2f else 1f,
                if(i%2==0)0xFFC6F5FF.toInt() else 0xFF73D1FA.toInt())
        }
    }
    private fun embers(c:Canvas,time:Long,
        x:Float,y:Float,count:Int,spread:Float,travel:Float,seed:Int){
        for(i in 0 until count){
            val elapsed=((time/34L+i*17L+seed*11L)%83L).toFloat()/83f
            val jitter=sin((time%19000L).toDouble()/285.0+
                i*1.83+seed).toFloat()
            val xx=x+jitter*spread
            val yy=y-elapsed*travel
            px(c,xx,yy,if(i%4==0)2f else 1f,
                if(i%3==0)0xFFFFE9A6.toInt()
                else if(i%3==1)0xFFFFB65B.toInt()
                else 0xFFF5793C.toInt())
        }
    }

    /** Draw all sprite-based effects behind the moving inhabitants. */
    fun draw(c:Canvas,source:Bitmap,now:Long){
        val sprite=layers(source)
        if(source.width!=WIDTH || source.height!=HEIGHT){
            c.drawBitmap(source,null,full,nearest)
            return
        }
        c.drawBitmap(sprite.floor,null,full,nearest)
        // Station illumination is layered over the crisp sprites.
        val t=(now%24000L).toFloat()/230f
        light(c,160f,136f,25f,255,132,48,t)
        light(c,48f,89f,17f,255,105,51,t+1.4f)
        light(c,274f,78f,12f,105,235,136,t*.77f)
        light(c,51f,167f,12f,255,194,101,t*1.3f)
        light(c,240f,166f,15f,255,164,91,t*1.05f)
        light(c,16f,43f,9f,255,141,49,t*.93f)
        light(c,304f,43f,9f,255,141,49,t*1.07f)
        light(c,12f,190f,12f,134,124,255,t*1.26f)
        light(c,305f,192f,12f,134,124,255,t*1.17f)
        renderPortal(c,sprite.portal,now)
        bobFlame(c,sprite.forgeFlame,49f,100f,t*1.62f,true)
        bobFlame(c,sprite.campFlame,160f,139f,t,true.not())
        embers(c,now,159f,123f,11,10f,28f,5)
        embers(c,now,52f,85f,6,7f,15f,13)
        // Animated potion bubbles and tiny scout-map markers.
        val rise=((now/120L)%19L).toFloat()
        px(c,279f,87f-rise*.27f,2f,0xFF94F6B8.toInt())
        px(c,283f,90f-(rise+7f)%19f*.28f,1f,0xFFDAFFBF.toInt())
        val scout=(now/370L)%12L
        px(c,if(scout<6)50f+scout else 56f-(scout-6f),
            163f,2f,0xFFFFC96A.toInt())
        // Occasional lantern highlight in the sleeping quarters.
        if((now/400L)%3L!=0L){
            px(c,246f,166f,3f,0xFFFFD883.toInt())
            px(c,240f,170f,1f,0xFFFFF2BA.toInt())
        }
    }
}
