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
    private var combatInitialized=false
    private var lastCombatRevision=0L
    private var combatStarted=0L

    fun reset(){
        locationKey=Long.MIN_VALUE
        lastTime=0L
        initialized=false
        combatInitialized=false
    }

    // Effects are triggered by a new combat revision, not the repeating
    // game-tick clock. Opening a screen must not replay an old attack.
    fun combatAge(revision:Long,now:Long):Int{
        if(!combatInitialized){
            combatInitialized=true
            lastCombatRevision=revision
            combatStarted=now
            return 10000
        }
        if(revision!=lastCombatRevision){
            lastCombatRevision=revision
            combatStarted=now
        }
        return (now-combatStarted).coerceIn(0L,10000L).toInt()
    }

    fun sample(floor:Int,room:Int,targetX:Float,targetY:Float,now:Long):Pair<Float,Float>{
        val key=(floor.toLong() shl 32) or (room.toLong() and 0xffffffffL)
        if(!initialized||key!=locationKey||now<lastTime||now-lastTime>5000L){
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
            // Never discard continuity during a brief UI stall. Speed is
            // capped to avoid snapping across a whole corridor at once.
            val smoothing=(1.0-exp((-elapsed.toDouble()/125.0))).toFloat()
            val maxStep=(elapsed*0.045f).coerceAtLeast(0f)
            val step=(distance*smoothing).coerceAtMost(maxStep).coerceAtMost(distance)
            if(distance>0f){
                x+=dx/distance*step
                y+=dy/distance*step
            }
        }
        return Pair(x,y)
    }
}
