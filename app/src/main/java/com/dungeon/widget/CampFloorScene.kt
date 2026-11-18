package com.dungeon.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

/** Persistent safe floor visually separate from the procedurally generated rooms. */
object CampFloorScene {
    private const val W=320
    private const val H=208
    private var exitStartedAt:Long=0L
    fun beginExit(){ exitStartedAt=System.currentTimeMillis() }
    fun cancelExit(){ exitStartedAt=0L }
    private val p=Paint().apply { isAntiAlias=false;isFilterBitmap=false }
    private fun rect(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){
        p.color=color;c.drawRect(x,y,x+w,y+h,p)
    }
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int){
        p.color=color;p.textSize=size;p.typeface=Typeface.create("monospace",Typeface.BOLD)
        c.drawText(s,x,y,p)
    }
    private fun light(c:Canvas,x:Float,y:Float,w:Float,color:Int){
        rect(c,x-w/2,y-3f,w,6f,color)
    }
    private fun worker(c:Canvas,x:Float,y:Float,skin:Int,clothes:Int,
        tick:Int,blacksmith:Boolean){
        val sway=if(tick==0)0f else sin(tick*0.065).toFloat()
        rect(c,x-7f,y+3f,14f,3f,0xFF252B2E.toInt())
        rect(c,x-5f,y-10f,10f,12f,clothes)
        rect(c,x-4f,y-16f,8f,7f,skin)
        rect(c,x-5f,y-19f,10f,3f,0xFF4D4136.toInt())
        if(blacksmith){
            val lift=if(tick%40<19)9f else 2f
            rect(c,x-10f,y-17f-lift,3f,13f,0xFF9C9A8D.toInt())
            rect(c,x-13f,y-18f-lift,9f,4f,0xFFBFC9C8.toInt())
        }else rect(c,x+4f,y-8f+sway,3f,7f,skin)
        rect(c,x-4f,y+1f,3f,5f,0xFF252F3B.toInt())
        rect(c,x+2f,y+1f,3f,5f,0xFF252F3B.toInt())
    }
    fun render(h:HeroState,frame:Int,live:Boolean):Bitmap{
        val b=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888)
        val c=Canvas(b)
        c.drawColor(0xFF101922.toInt())
        // The sanctuary is a subterranean hall, not an outdoor menu.
        for(row in 0..12)for(col in 0..20){
            val x=col*16f+(if(row%2==0)0f else -8f)
            val y=row*16f
            val edge=(row*7+col*13)%5
            rect(c,x,y,15f,15f,if(edge==0)0xFF28313B.toInt() else 0xFF212A34.toInt())
            rect(c,x,y,14f,1f,0xFF35404A.toInt())
        }
        // Central walkable stone floor and decorated perimeter.
        rect(c,17f,27f,286f,161f,0xFF3E4141.toInt())
        for(row in 0..9)for(col in 0..17){
            val x=18f+col*16f;val y=28f+row*16f
            rect(c,x,y,15f,15f,if((row+col)%3==0)0xFF454948.toInt()
                else 0xFF3B403E.toInt())
            rect(c,x,y,14f,1f,0xFF4F514B.toInt())
        }
        // Warm path links the four functional stations.
        rect(c,149f,30f,22f,146f,0xFF655949.toInt())
        rect(c,35f,100f,249f,13f,0xFF665A49.toInt())
        for(i in 0..8){
            rect(c,153f,35f+i*16f,12f,2f,0xFF877258.toInt())
        }
        // Forge (top left): grows chimney, glowing anvil and tools.
        rect(c,37f,46f,56f,37f,0xFF252D36.toInt())
        rect(c,40f,42f,50f,7f,0xFF777B78.toInt())
        rect(c,44f,36f,14f,12f,0xFF59606A.toInt())
        if(h.forge>=2)rect(c,48f,23f,8f,18f,0xFF626B70.toInt())
        rect(c,55f,58f,23f,15f,0xFF8B5441.toInt())
        val hammerDown=if(live && frame%40<15) 1f else 0f
        rect(c,60f,62f,12f,7f,if(hammerDown>0f)0xFFFFC46A.toInt()
            else 0xFFF0A058.toInt())
        if(live && frame%40<11){
            rect(c,65f,52f,2f,4f,0xFFFFD58A.toInt())
            rect(c,73f,51f,3f,3f,0xFFFFAD54.toInt())
            rect(c,62f,48f,2f,2f,0xFFFFE5AB.toInt())
        }
        rect(c,57f,78f,21f,3f,0xFF9EA9AE.toInt())
        if(h.forge>=4)rect(c,76f,36f,5f,16f,0xFFDBBD81.toInt())
        text(c,"FORGE "+h.forge,35f,94f,9f,0xFFEAC48D.toInt())
        // Medic station (top right), increasingly equipped.
        rect(c,229f,42f,53f,43f,0xFF586452.toInt())
        rect(c,225f,39f,61f,8f,0xFF8A9980.toInt())
        rect(c,237f,55f,15f,22f,0xFFBBB6A2.toInt())
        rect(c,242f,59f,5f,14f,0xFFBD5056.toInt())
        rect(c,238f,64f,13f,4f,0xFFBD5056.toInt())
        if(h.healer>=2){
            rect(c,260f,53f,14f,15f,0xFF7CC3A9.toInt())
            rect(c,264f,48f,6f,6f,0xFFB2E5CD.toInt())
        }
        if(h.healer>=5)rect(c,258f,72f,19f,5f,0xFFD5E2BD.toInt())
        text(c,"HEAL "+h.healer,231f,95f,9f,0xFFBDE2B9.toInt())
        // Scout station (bottom right) with map, lantern and increasing supplies.
        rect(c,225f,131f,61f,37f,0xFF564A3C.toInt())
        rect(c,230f,126f,50f,10f,0xFF8B7656.toInt())
        rect(c,237f,140f,32f,20f,0xFFC7AF7A.toInt())
        rect(c,242f,144f,20f,2f,0xFF6D684C.toInt())
        rect(c,247f,148f,2f,10f,0xFF7B7959.toInt())
        if(h.scouts>=2)rect(c,274f,137f,8f,14f,0xFFDCC477.toInt())
        if(h.scouts>=4)rect(c,231f,157f,12f,9f,0xFF71895E.toInt())
        text(c,"SCOUT "+h.scouts,224f,181f,9f,0xFFE0CE94.toInt())
        // Lantern and small map marker react gently to activity.
        if(live && frame%50<25){
            rect(c,274f,132f,5f,5f,0xFFFFCF75.toInt())
            rect(c,252f,149f,3f,3f,0xFFB4CFA7.toInt())
        }
        // Camp quarters (bottom left).
        rect(c,34f,136f,69f,32f,0xFF40545D.toInt())
        rect(c,37f,129f,63f,8f,0xFF9B875E.toInt())
        rect(c,44f,143f,48f,15f,0xFF697C78.toInt())
        if(h.camp>=2)rect(c,40f,126f,8f,11f,0xFFBE9B66.toInt())
        if(h.camp>=5)rect(c,94f,143f,6f,18f,0xFFCDAC67.toInt())
        text(c,"CAMP "+h.camp,39f,181f,9f,0xFFE3CCA3.toInt())
        // Central campfire. Stable flame with restrained pixel animation.
        rect(c,129f,100f,62f,8f,0xFF21262B.toInt())
        for(i in 0..5){
            rect(c,135f+i*9f,101f,7f,5f,0xFF847F75.toInt())
        }
        rect(c,146f,90f,28f,11f,0xFF78442D.toInt())
        val flicker=if(live)(sin(frame*0.12)*2).toFloat() else 0f
        rect(c,150f,77f+flicker,20f,16f,0xFFF2943C.toInt())
        rect(c,155f,71f-flicker,10f,17f,0xFFFFCB70.toInt())
        rect(c,158f,80f,5f,11f,0xFFFFE5A0.toInt())
        light(c,160f,109f,68f,0x445C4228)
        // Embers drift upwards deterministically, without random flicker.
        if(live)for(i in 0..7){
            val phase=(frame*0.75f+i*16f)%85f
            val ex=155f+sin(i*1.83+frame*0.037).toFloat()*13f
            val ey=84f-phase*0.72f
            if(ey>30f)rect(c,ex,ey,if(i%3==0)3f else 2f,2f,
                if(i%2==0)0xFFFFC76A.toInt() else 0xFFCE7045.toInt())
        }
        // Four permanent workers make the individual buildings feel occupied.
        worker(c,91f,82f,0xFFB47F63.toInt(),0xFF4A5662.toInt(),
            if(live)frame else 0,true)
        worker(c,220f,82f,0xFFE3C5A0.toInt(),0xFF6D876E.toInt(),
            if(live)frame+18 else 0,false)
        worker(c,208f,152f,0xFFD3B18A.toInt(),0xFF766D49.toInt(),
            if(live)frame+27 else 0,false)
        worker(c,111f,157f,0xFFD9BC93.toInt(),0xFF52667B.toInt(),
            if(live)frame+9 else 0,false)
        // The hero visits stations on a looping route, and runs toward the portal
        // when the player leaves. This is visual only: dungeon coordinates are untouched.
        val route=arrayOf(
            floatArrayOf(160f,142f),floatArrayOf(119f,109f),
            floatArrayOf(102f,91f),floatArrayOf(160f,116f),
            floatArrayOf(217f,106f),floatArrayOf(212f,91f),
            floatArrayOf(160f,121f),floatArrayOf(203f,157f),
            floatArrayOf(160f,143f),floatArrayOf(110f,158f))
        val outgoing=exitStartedAt>0L &&
            System.currentTimeMillis()-exitStartedAt in 0L..2200L
        val px:Float
        val py:Float
        if(outgoing){
            val t=((System.currentTimeMillis()-exitStartedAt)/1000f).coerceIn(0f,1f)
            px=160f;py=142f+(39f-142f)*t
        }else if(live){
            val phase=(frame/65f)%route.size
            val from=phase.toInt()
            val to=(from+1)%route.size
            val t=phase-from
            // Smoothstep avoids abrupt corner snaps.
            val eased=t*t*(3f-2f*t)
            px=route[from][0]+(route[to][0]-route[from][0])*eased
            py=route[from][1]+(route[to][1]-route[from][1])*eased
        }else{px=160f;py=142f}
        val moving=outgoing || (live && (frame%65)>4 && (frame%65)<61)
        val stride=if(moving)sin(frame*0.32).toFloat()*2.2f else 0f
        val breathe=if(live)sin(frame*0.07).toFloat()*0.7f else 0f
        rect(c,px-9f,py-2f,18f,4f,0xFF242A2C.toInt())
        rect(c,px-6f,py-16f+breathe,12f,14f,0xFF4A6C89.toInt())
        rect(c,px-6f,py-23f+breathe,12f,9f,0xFFD9B992.toInt())
        rect(c,px-8f,py-25f+breathe,16f,4f,0xFF6D5A43.toInt())
        rect(c,px-7f,py-4f+stride,5f,7f,0xFF273547.toInt())
        rect(c,px+2f,py-4f-stride,5f,7f,0xFF273547.toInt())
        if(outgoing && live){
            rect(c,px-10f,py+4f,20f,2f,0x7765B8D3)
        }
        // Portal marks the return point, without modifying the active dungeon.
        rect(c,149f,22f,22f,12f,0xFF344C64.toInt())
        rect(c,153f,24f,14f,8f,0xFF6096B1.toInt())
        rect(c,156f,25f,8f,6f,0xFF9DD6D7.toInt())
        if(live){
            val pulse=(sin(frame*0.095)+1.0).toFloat()
            rect(c,153f-pulse,23f,2f,10f,0xFF72B6D2.toInt())
            rect(c,168f+pulse,23f,2f,10f,0xFF72B6D2.toInt())
            if(frame%30<12)rect(c,158f,21f,4f,3f,0xFFC4F3EF.toInt())
        }
        // Minimal bottom overlay, not a second viewport or tab-specific height.
        rect(c,0f,190f,320f,18f,0xE0131E27.toInt())
        text(c,"FLOOR 0  /  SANCTUARY",10f,202f,10f,0xFFE9CEA0.toInt())
        text(c,"SAFE",272f,202f,9f,0xFF9BD5AD.toInt())
        return b
    }
}
