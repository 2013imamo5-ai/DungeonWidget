package com.dungeon.widget

import kotlin.math.exp
import kotlin.math.hypot

/**
 * Presentation-only movement. Game logic still advances on the same saved ticks.
 * The render coordinates are never serialized, so old game saves are compatible.
 */
class DungeonMotion {
    private var locationKey:Long=Long.MIN_VALUE
    private var lastTime:Long=0L
    private var x=0f
    private var y=0f
    private var initialized=false

    fun reset(){
        locationKey=Long.MIN_VALUE
        lastTime=0L
        initialized=false
    }

    fun sample(floor:Int,room:Int,targetX:Float,targetY:Float,now:Long):Pair<Float,Float>{
        val key=(floor.toLong() shl 32) or (room.toLong() and 0xffffffffL)
        if(!initialized||key!=locationKey||now<lastTime||now-lastTime>1000L){
            locationKey=key
            initialized=true
            x=targetX;y=targetY;lastTime=now
            return Pair(x,y)
        }
        val elapsed=(now-lastTime).coerceIn(0,180).toFloat()
        lastTime=now
        val dx=targetX-x
        val dy=targetY-y
        val distance=hypot(dx,dy)
        if(distance<0.025f){x=targetX;y=targetY}
        else{
            // Low-pass frame jitter but never snap across map tiles.
            val smoothing=(1.0-exp((-elapsed.toDouble()/85.0))).toFloat()
            val maxStep=(elapsed*0.065f).coerceAtLeast(0f)
            val step=(distance*smoothing).coerceAtMost(maxStep).coerceAtMost(distance)
            if(distance>0f){
                x+=dx/distance*step
                y+=dy/distance*step
            }
        }
        return Pair(x,y)
    }
}
