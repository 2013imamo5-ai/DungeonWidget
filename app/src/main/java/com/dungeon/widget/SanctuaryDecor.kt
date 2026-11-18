package com.dungeon.widget

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.sin

/** Atmospheric furnishings behind the animated floor-zero stations. */
object SanctuaryDecor {
    private val p=Paint().apply{isAntiAlias=false;isFilterBitmap=false}
    private fun r(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){
        p.color=color;c.drawRect(x,y,x+w,y+h,p)
    }
    private fun torch(c:Canvas,x:Float,y:Float,frame:Int,animate:Boolean){
        val flame=if(animate)sin(frame*0.11+x).toFloat()*1.3f else 0f
        r(c,x-8f,y-9f,16f,20f,0x14FFD394)
        r(c,x-5f,y-6f,10f,13f,0x24FFB35B)
        r(c,x-2f,y+4f,4f,8f,0xFF84767A.toInt())
        r(c,x-4f,y+1f,8f,4f,0xFF3F404C.toInt())
        r(c,x-3f,y-8f+flame,6f,9f,0xFFEC8E43.toInt())
        r(c,x-2f,y-12f+flame,4f,9f,0xFFFFC66C.toInt())
        r(c,x-1f,y-8f+flame,2f,5f,0xFFFFF3B6.toInt())
    }
    private fun banner(c:Canvas,x:Float,y:Float,green:Boolean){
        val cloth=if(green)0xFF34634C.toInt() else 0xFF703E4A.toInt()
        r(c,x-10f,y-2f,20f,3f,0xFFBD9870.toInt())
        r(c,x-8f,y+1f,16f,21f,cloth)
        r(c,x-5f,y+3f,10f,2f,0xFF997351.toInt())
        r(c,x-2f,y+6f,4f,11f,0xFFE5D0A1.toInt())
        r(c,x-6f,y+10f,12f,3f,0xFFE5D0A1.toInt())
        r(c,x-6f,y+22f,12f,4f,0xFF2C2939.toInt())
        r(c,x-3f,y+22f,6f,4f,cloth)
    }
    private fun crystal(c:Canvas,x:Float,y:Float){
        r(c,x-12f,y+8f,25f,5f,0xFF252B3C.toInt())
        r(c,x-7f,y-5f,8f,17f,0xFF567AC5.toInt())
        r(c,x-4f,y-11f,4f,21f,0xFF95C9F5.toInt())
        r(c,x-3f,y-7f,2f,11f,0xFFE2FFFF.toInt())
        r(c,x+4f,y+1f,8f,11f,0xFF7E95E4.toInt())
        r(c,x+7f,y-4f,4f,14f,0xFFB6DAF8.toInt())
    }
    private fun rug(c:Canvas,x:Float,y:Float,w:Float,h:Float,shade:Int){
        r(c,x,y,w,h,0xFF262737.toInt())
        r(c,x+2f,y+2f,w-4f,h-4f,shade)
        r(c,x+5f,y+5f,w-10f,h-10f,0xFF59414A.toInt())
        r(c,x+8f,y+8f,w-16f,h-16f,shade)
        for(i in 0 until 6){
            val xx=x+5f+i*(w-10f)/5f
            r(c,xx,y+3f,3f,2f,0xFFCC9A6E.toInt())
            r(c,xx,y+h-5f,3f,2f,0xFFCC9A6E.toInt())
        }
    }
    fun draw(c:Canvas,frame:Int,live:Boolean){
        c.save();c.clipRect(0f,0f,320f,190f)
        // Multi-depth masonry arch surrounding the actual portal.
        r(c,128f,0f,64f,43f,0xFF161E2C.toInt())
        r(c,131f,2f,58f,41f,0xFF756C7E.toInt())
        r(c,137f,5f,46f,38f,0xFF292D43.toInt())
        r(c,142f,8f,36f,33f,0xFF244767.toInt())
        r(c,146f,14f,28f,25f,0xFF3579AB.toInt())
        r(c,151f,16f,18f,21f,0xFF5FB2DA.toInt())
        r(c,154f,20f,12f,15f,0xFF224B88.toInt())
        r(c,160f,19f,3f,13f,0xFFD0F6FF.toInt())
        r(c,128f,31f,14f,18f,0xFF645F73.toInt())
        r(c,178f,31f,14f,18f,0xFF645F73.toInt())
        r(c,126f,34f,19f,5f,0xFFA39A94.toInt())
        r(c,175f,34f,19f,5f,0xFFA39A94.toInt())
        // Banners, braziers and subtle warmth at the sanctuary perimeter.
        banner(c,109f,9f,false)
        banner(c,211f,9f,true)
        torch(c,19f,57f,frame,live)
        torch(c,301f,57f,frame,live)
        torch(c,19f,154f,frame,live)
        torch(c,301f,154f,frame,live)
        // Ornamental rugs remain flat and do not change pathing.
        rug(c,31f,43f,74f,45f,0xFF795646.toInt())
        rug(c,219f,42f,68f,50f,0xFF46674F.toInt())
        rug(c,27f,123f,81f,57f,0xFF58516A.toInt())
        rug(c,220f,121f,74f,56f,0xFF706047.toInt())
        // Arc of chipped granite around the fire, before flames are drawn.
        for(i in 0..17){
            val a=i*Math.PI/9.0
            val x=160f+sin(a).toFloat()*39f
            val y=105f+kotlin.math.cos(a).toFloat()*27f
            r(c,x-4f,y-3f,8f,5f,if(i%2==0)
                0xFF998678.toInt() else 0xFF756873.toInt())
            r(c,x-3f,y-3f,6f,1f,0xFFC1A58C.toInt())
        }
        crystal(c,22f,180f)
        crystal(c,299f,180f)
        // Boxes, herb bundles and scroll racks hint at the other workers.
        for(i in 0..2){
            r(c,23f+i*11f,103f,9f,9f,0xFF6D5140.toInt())
            r(c,23f+i*11f,103f,9f,2f,0xFFAA8659.toInt())
            r(c,280f-i*12f,108f,9f,8f,0xFF6D5140.toInt())
            r(c,280f-i*12f,108f,9f,2f,0xFFAA8659.toInt())
        }
        for(i in 0..3){
            r(c,237f+i*11f,34f,5f,5f,0xFF568962.toInt())
            r(c,239f+i*11f,30f,2f,6f,0xFF84B371.toInt())
        }
        c.restore()
    }
}
