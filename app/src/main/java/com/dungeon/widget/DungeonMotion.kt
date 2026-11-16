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
    private var cameraReady=false
    private var cameraX=0f
    private var cameraY=0f
    private var cameraClock=0L
    private var minimapCorner=0
    private var minimapLastChanged=0L

    fun reset(){
        locationKey=Long.MIN_VALUE
        lastTime=0L
        initialized=false
        combatInitialized=false
        cameraReady=false
        minimapCorner=0
        minimapLastChanged=0L
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

    /**
     * Camera target is the origin of the source viewport. Allow a small
     * overscan around the world so the hero can stay beneath the HUD even
     * while exploring the first row of the map.
     */
    fun camera(targetX:Float,targetY:Float,now:Long):Pair<Float,Float>{
        if(!cameraReady||now<cameraClock||now-cameraClock>5000L){
            cameraX=targetX
            cameraY=targetY
            cameraClock=now
            cameraReady=true
            return Pair(cameraX,cameraY)
        }
        val dt=(now-cameraClock).coerceIn(0L,180L).toFloat()
        cameraClock=now
        val blend=(1.0-exp(-dt.toDouble()/175.0)).toFloat()
        cameraX+=(targetX-cameraX)*blend
        cameraY+=(targetY-cameraY)*blend
        return Pair(cameraX,cameraY)
    }

    /**
     * Candidate positions: bottom-right, bottom-left, right-middle,
     * left-middle. Avoid both the player and the current battle target.
     * Minimum hold time prevents constant corner flicker.
     */
    fun mapCorner(hx:Float,hy:Float,ex:Float,ey:Float,
        fighting:Boolean,now:Long):Int{
        val rects=arrayOf(
            floatArrayOf(225f,133f,316f,193f),
            floatArrayOf(4f,133f,95f,193f),
            floatArrayOf(225f,58f,316f,118f),
            floatArrayOf(4f,58f,95f,118f)
        )
        fun overlap(r:FloatArray,x:Float,y:Float,margin:Float):Float{
            val dx=kotlin.math.max(r[0]-x,0f).coerceAtLeast(x-r[2])
            val dy=kotlin.math.max(r[1]-y,0f).coerceAtLeast(y-r[3])
            val d=kotlin.math.hypot(dx,dy)
            return (margin-d).coerceAtLeast(0f)
        }
        val scores=rects.indices.map{i->
            val r=rects[i]
            val hero=overlap(r,hx,hy-13f,23f)
            val enemy=if(fighting)overlap(r,ex,ey-13f,20f) else 0f
            hero*5f+enemy*4f+i*1.4f
        }
        val best=scores.indices.minByOrNull{scores[it]}?:0
        val current=scores[minimapCorner]
        if(best!=minimapCorner && (now-minimapLastChanged>1200L)
            && (current>scores[best]+8f || current>35f)){
            minimapCorner=best
            minimapLastChanged=now
        }
        return minimapCorner
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
