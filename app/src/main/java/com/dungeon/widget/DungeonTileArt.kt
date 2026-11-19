package com.dungeon.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * High-contrast pixel art replacement for the old flat checkerboard.
 * Uses the existing DungeonMap walkability and never changes gameplay.
 * Nothing is loaded from the network or from individual saved rooms.
 */
object DungeonTileArt {
    private val p=Paint().apply { isAntiAlias=false;isFilterBitmap=false }
    private val floorBase=intArrayOf(
        0xFF62515B.toInt(),0xFF405A4A.toInt(),
        0xFF69453E.toInt(),0xFF485C70.toInt())
    private val wallBase=intArrayOf(
        0xFF343044.toInt(),0xFF273D37.toInt(),
        0xFF423037.toInt(),0xFF293F52.toInt())
    private val lightStone=intArrayOf(
        0xFFA9989A.toInt(),0xFF90A994.toInt(),
        0xFFBD8E76.toInt(),0xFF9BBAC8.toInt())
    private fun r(c:Canvas,x:Float,y:Float,w:Float,h:Float,color:Int){
        p.color=color
        c.drawRect(x,y,x+w,y+h,p)
    }
    private fun tone(color:Int,offset:Int)=Color.rgb(
        (Color.red(color)+offset).coerceIn(0,255),
        (Color.green(color)+offset).coerceIn(0,255),
        (Color.blue(color)+offset).coerceIn(0,255))
    private fun hash(seed:Int,x:Int,y:Int):Int{
        var v=seed xor (x*73856093) xor (y*19349663)
        v=v xor (v ushr 16)
        v*=0x7feb352d
        return (v xor (v ushr 15)) and Int.MAX_VALUE
    }
    private fun floor(c:Canvas,x:Float,y:Float,base:Int,seed:Int,biome:Int){
        val main=tone(base,(seed%7)-3)
        val light=lightStone[biome]
        // Large individual flagstones are separated by real shadowed grout.
        r(c,x,y,16f,16f,0xFF282633.toInt())
        val offset=if(seed%2==0)0f else 4f
        r(c,x,y,15f,7f,tone(main,9))
        r(c,x+1f,y+1f,13f,1f,tone(main,30))
        r(c,x,y+8f,15f,8f,tone(main,-6))
        r(c,x+1f,y+9f,13f,1f,tone(main,13))
        r(c,x+offset+3f,y+1f,1f,6f,0xFF33313A.toInt())
        r(c,x+11f-offset,y+9f,1f,6f,0xFF33313A.toInt())
        r(c,x+2f,y+6f,7f,1f,tone(main,-25))
        r(c,x+8f,y+14f,6f,1f,tone(main,-25))
        if(seed%5==0){
            // Hairline fractures form a recognizable aging pattern.
            r(c,x+4f,y+3f,1f,3f,tone(main,-31))
            r(c,x+5f,y+5f,4f,1f,tone(main,-31))
            r(c,x+8f,y+6f,1f,3f,tone(main,-31))
            r(c,x+9f,y+9f,3f,1f,tone(main,-31))
        }
        if(seed%13==4){
            val moss=when(biome){
                1->0xFF6BA477.toInt()
                2->0xFFAD6D4E.toInt()
                3->0xFF92CDE1.toInt()
                else->0xFF6D7B73.toInt()
            }
            r(c,x+2f,y+12f,5f,2f,moss)
            r(c,x+3f,y+11f,2f,2f,tone(moss,28))
        }
        if(seed%11==7){
            r(c,x+10f,y+3f,3f,1f,light)
            r(c,x+12f,y+4f,1f,3f,tone(light,-32))
        }
    }
    private fun wall(c:Canvas,x:Float,y:Float,base:Int,seed:Int,
        above:Boolean,below:Boolean,left:Boolean,right:Boolean,biome:Int){
        val line=lightStone[biome]
        val brick=tone(base,(seed%7)-3)
        // Deep shadow at the rear, three rows of dressed masonry at the front.
        r(c,x,y,16f,16f,0xFF131622.toInt())
        r(c,x,y+1f,16f,14f,tone(brick,-6))
        r(c,x+1f,y+1f,14f,2f,tone(brick,32))
        r(c,x,y+5f,16f,2f,tone(brick,-25))
        r(c,x,y+12f,16f,2f,tone(brick,-25))
        r(c,x+6f+(seed%3).toFloat(),y+2f,1f,3f,tone(brick,-34))
        r(c,x+11f-(seed%5).toFloat(),y+7f,1f,5f,tone(brick,-38))
        r(c,x+3f,y+8f,7f,1f,tone(brick,17))
        r(c,x+8f,y+14f,5f,1f,tone(brick,11))
        if(seed%13==0){
            // Mason's chisel marks make large walls less repetitive.
            r(c,x+4f,y+3f,5f,1f,tone(brick,43))
            r(c,x+6f,y+4f,1f,3f,tone(brick,-30))
        }
        if(below){
            // Vertical 3D wall face, capstones and a deep bottom ledge.
            r(c,x,y+10f,16f,6f,tone(brick,-39))
            r(c,x,y+9f,16f,2f,line)
            r(c,x+1f,y+12f,14f,1f,tone(brick,5))
            r(c,x+1f,y+15f,14f,1f,0xFF171B29.toInt())
            if(seed%7==2){
                r(c,x+2f,y+1f,11f,10f,0xFF151925.toInt())
                r(c,x+3f,y+2f,9f,2f,tone(brick,27))
                r(c,x+3f,y+8f,9f,2f,tone(brick,-11))
                // Tiny sculpted skull ornament on the dungeon masonry.
                r(c,x+5f,y+4f,6f,5f,tone(line,-11))
                r(c,x+6f,y+6f,1f,1f,0xFF33303A.toInt())
                r(c,x+9f,y+6f,1f,1f,0xFF33303A.toInt())
            }
        }
        if(above)r(c,x,y,16f,2f,line)
        if(left){
            r(c,x,y,3f,16f,line)
            r(c,x+3f,y,2f,16f,tone(brick,-32))
        }
        if(right){
            r(c,x+13f,y,3f,16f,line)
            r(c,x+11f,y,2f,16f,tone(brick,-32))
        }
        if(seed%19==6 && (left||right)){
            r(c,x+6f,y+4f,3f,9f,0xFF405C53.toInt())
            r(c,x+8f,y+5f,2f,4f,0xFF6C9775.toInt())
        }
    }
    private fun torch(c:Canvas,x:Float,y:Float,biome:Int,tick:Long){
        val warm=biome!=1&&biome!=3
        val flicker=sin(tick/270.0+x).toFloat()*1.1f
        r(c,x-3f,y-2f,6f,10f,0xFF34303B.toInt())
        r(c,x-2f,y+2f,4f,6f,0xFF8E7B71.toInt())
        r(c,x-5f,y,10f,2f,0xFFBCA081.toInt())
        r(c,x-5f,y-8f+flicker,10f,8f,
            if(warm)0xFFE78E3D.toInt() else 0xFF65BED0.toInt())
        r(c,x-3f,y-11f-flicker,6f,10f,
            if(warm)0xFFFFC86C.toInt() else 0xFF9CEBF6.toInt())
        r(c,x-1f,y-8f,2f,6f,0xFFFFEDB4.toInt())
    }
    fun draw(c:Canvas,room:RoomLayout,biome:Int,now:Long,animate:Boolean){
        val type=biome.coerceIn(0,3)
        val floor=floorBase[type]
        val wall=wallBase[type]
        c.save()
        c.clipRect(0f,17f,320f,193f)
        for(y in 0 until 11)for(x in 0 until 20){
            val px=x*16f
            val py=17f+y*16f
            val seed=hash(room.seed,x,y)
            val up=room.isFloor(x,y-1)
            val down=room.isFloor(x,y+1)
            val left=room.isFloor(x-1,y)
            val right=room.isFloor(x+1,y)
            if(room.isFloor(x,y)){
                floor(c,px,py,floor,seed,type)
                // Dark edge on walkable side of the raised walls.
                if(!up)r(c,px,py,16f,2f,0xAA141724.toInt())
                if(!down)r(c,px,py+14f,16f,2f,0x66141724.toInt())
                if(!left)r(c,px,py,2f,16f,0xAA141724.toInt())
                if(!right)r(c,px+14f,py,2f,16f,0xAA141724.toInt())
            }else{
                wall(c,px,py,wall,seed,up,down,left,right,type)
                // Same deterministic wall torch positions as the light pass.
                if(down&&(room.seed+x*23+y*29)%19==3){
                    torch(c,px+8f,py+5f,type,
                        if(animate)now else room.seed.toLong()*107L)
                }
            }
        }
        c.restore()
    }
}
