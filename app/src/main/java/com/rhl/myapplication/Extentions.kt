package com.rhl.myapplication

import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


fun RecyclerView.LayoutManager.layoutDecoratedWithMargins(
    view: View,
    positionData: PositionData
) = with(positionData) { layoutDecoratedWithMargins(view, left, top, right, bottom) }

fun View.updatePosition(
    positionData: PositionData,
    calculatedRatio: Float
) {
//    val scale = Math.round(calculatedRatio * 1000.0) / 1000.0
    scaleX = calculatedRatio
    scaleY = calculatedRatio
    left = positionData.left
    top = positionData.top
    right = positionData.right
    bottom = positionData.bottom


}

fun scaleView(v: View, startScale: Float, endScale: Float) {
    val anim: Animation = ScaleAnimation(
        1f, 1f,  // Start and end values for the X axis scaling
        startScale, endScale,  // Start and end values for the Y axis scaling
        Animation.RELATIVE_TO_SELF, 0f,  // Pivot point of X scaling
        Animation.RELATIVE_TO_SELF, 1f
    ) // Pivot point of Y scaling
    anim.fillAfter = true // Needed to keep the result of the animation
    anim.duration = 1000
    v.startAnimation(anim)
}

fun View.updateViewSize(scale: Float) {
//    alpha = scale.toFloat()
    scaleX = if (scale > 1) 1f else scale.toFloat()
    scaleY = if (scale > 1) 1f else scale.toFloat()
}

fun Int.isDivideByTwo() = this % 2 == 0

fun cosAngle(angle: Double): Double {
    return cos(angleToRadian(angle))
}

fun sinAngle(angle: Double): Double {
    return sin(angleToRadian(angle))
}

fun angleToRadian(angle: Double) = angle * PI / 180
