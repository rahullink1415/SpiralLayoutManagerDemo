package com.rhl.myapplication

import android.graphics.PointF
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CircularRecyclerLayoutManager(
    private val canScrollHorizontally: Boolean = true,
    private val canScrollVertically: Boolean = false,
    val itemWidth: Int = 0,
    private val onLayoutDrawn: (center: PointF, secondLastPositionData: PositionData?, posLast: Int, snapPosData: PositionData?) -> Unit
) : RecyclerView.LayoutManager() {
    var secondLastPositionData: PositionData? = null

    private var secondLastVisiblePos: Int = 0
    private var isScrollBackward: Boolean = false
    private var stopBackScroll: Boolean = true
    private var stopForwardScroll: Boolean = false

    private val spiralRatio: Double = 0.65
    private var horizontalScrollOffset = 0
    private var verticalScrollOffset = 0


    override fun generateDefaultLayoutParams() =
        RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )

    override fun canScrollHorizontally() = canScrollHorizontally

    override fun canScrollVertically() = canScrollVertically

    override fun measureChildWithMargins(child: View, widthUsed: Int, heightUsed: Int) {
        child.measure(
            View.MeasureSpec.makeMeasureSpec(widthUsed, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightUsed, View.MeasureSpec.EXACTLY)
        )
    }

    private lateinit var centerPoint: PointF

    private val viewCalculation = SparseArray<ItemData>(itemCount)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (recycler != null) {
            detachAndScrapAttachedViews(recycler)
        };

        centerPoint = PointF(width / 1.85f, height / 2f)
        for (pos in 0..30) {
            val angle = -45.0 * pos
            val circleRadius =
                spiralRatio.times(angle)
            val data = calculatePosition(circleRadius, angle)
            val calculatedRatio =
                circleRadius.div(spiralRatio).div(-45).toInt().div(10f)
            if (isViewVisible(data) && calculatedRatio < 2.0) {
                secondLastVisiblePos = pos - 1
                val angle2 = -45.0 * secondLastVisiblePos
                val circleRadius2 =
                    spiralRatio.times(angle2)
                secondLastPositionData = calculatePosition(circleRadius2, angle2)
            }
        }
        for (position in 0 until itemCount) {
            fillAndLayoutItem(position, recycler)
        }
        onLayoutDrawn.invoke(centerPoint, secondLastPositionData, 0,null)
    }

    private fun fillAndLayoutItem(position: Int, recycler: RecyclerView.Recycler?) {
        val pos = secondLastVisiblePos - position
        var angle = -45.0 * pos
        angle = if (angle < 0) angle else 0.0
        val circleRadius =
            spiralRatio.times(angle)
        val positionData = calculatePosition(circleRadius, angle)

        viewCalculation.put(position, ItemData(circleRadius, circleRadius, angle))
        val calculatedRatio =
            circleRadius.div(spiralRatio).div(-45).toInt().div(10f)
        if (isViewVisible(positionData) && calculatedRatio < 2.0) {
            recycler?.getViewForPosition(position)?.let { viewForPosition ->

                viewForPosition.scaleX = calculatedRatio
                viewForPosition.scaleY = calculatedRatio
                addView(viewForPosition)

                measureChildWithMargins(viewForPosition, itemWidth, itemWidth)
                layoutDecoratedWithMargins(viewForPosition, positionData)

            }
        }

    }


    private fun calculatePosition(radius: Double, angle: Double): PositionData {
        val xCoordinate = (radius * cosAngle(angle)) + centerPoint.x
        val yCoordinate = (radius * sinAngle(angle)) + centerPoint.y
        val top = yCoordinate - itemWidth.div(2)
        val left = xCoordinate - itemWidth.div(2)
        val right = left + itemWidth
        val bottom = top + itemWidth
        return PositionData(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }


    private fun isViewVisible(positionData: PositionData): Boolean {
        return when {
            positionData.left <= (-itemWidth) -> false
            positionData.right >= width + itemWidth / 2 -> false
            positionData.top <= (-itemWidth) -> false
            positionData.bottom >= height + itemWidth -> false
            else -> true
        }
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return false
    }

    private fun updateViews(recycler: RecyclerView.Recycler?) {
        val updatedPositions = mutableListOf<Int>()
        val viewsForDetaching = mutableListOf<View>()

        updateAllChild(viewsForDetaching, updatedPositions)

        for (position in 0 until itemCount) {
            if (updatedPositions.contains(position)) continue
            val data = viewCalculation[position]
            val positionData = calculatePosition(data.currentRadius ?: 0.0, data.angle ?: 0.0)
            layoutItemIfNeeded(positionData, data, recycler, position, viewsForDetaching)
        }

        recycler?.let { viewsForDetaching.forEach { detachAndScrapView(it, recycler) } }
        viewsForDetaching.clear()
    }

    private fun layoutItemIfNeeded(
        positionData: PositionData,
        data: ItemData?,
        recycler: RecyclerView.Recycler?,
        position: Int,
        viewsForDetaching: MutableList<View>
    ) {
        recycler?.getViewForPosition(position)?.let { viewForPosition ->

            val calculatedRatio =
                (data?.currentRadius ?: 0.0).div(spiralRatio).div(-45).toInt().div(10f)
            if (isViewVisible(positionData) && calculatedRatio > 0.0 && calculatedRatio < 2.0) {
                addView(viewForPosition)
                viewForPosition.scaleX = calculatedRatio
                viewForPosition.scaleY = calculatedRatio
                viewsForDetaching.remove(viewForPosition)
                measureChildWithMargins(viewForPosition, itemWidth, itemWidth)
                layoutDecoratedWithMargins(viewForPosition, positionData)

            } else {
                detachView(viewForPosition)
            }
        }
    }

    private fun updateAllChild(
        viewsForDetaching: MutableList<View>,
        updatedPositions: MutableList<Int>
    ) {
        for (position in 0 until childCount) {
            getChildAt(position)?.let { childAt ->
                val childPosition = getPosition(childAt)
                val data = viewCalculation[childPosition]

                val positionData = calculatePosition(data.currentRadius ?: 0.0, data.angle ?: 0.0)
                val calculatedRatio =
                    (data.currentRadius ?: 0.0).div(spiralRatio).div(-45).toInt().div(10f)
                childAt.scaleX = calculatedRatio
                childAt.scaleY = calculatedRatio
                if (isViewVisible(positionData).not() || data.currentRadius == 0.0 || calculatedRatio > 2.0) {
                    viewsForDetaching.add(childAt)
                } else {
                    childAt.updatePosition(positionData)
                }
                updatedPositions.add(childPosition)

            }
        }
    }


    private fun shouldItemMove(index: Int): Boolean {
        val itemCurrentRadius = viewCalculation.get(index)?.currentRadius ?: 0.0
        val calculatedRatio =
            (itemCurrentRadius).div(spiralRatio).div(-45).toInt().div(10f)

//        if (childCount in 1..2 && calculatedRatio >1.7 &&isScrollBackward.not()) {
//            horizontalScrollOffset =0
//                return false
//            }
        return true
    }


    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        var travel = dx
        isScrollBackward = dx >= 0
        travel = if (isScrollBackward) {
            if (dx >= 500)
                500
            else
                dx
        } else {
            if (dx <= -500)
                -500
            else
                dx
        }
        if (itemCount == 1)
            travel = 0
        val lastAngle = -45.0 * secondLastVisiblePos + 1
        val lastRadius = spiralRatio.times(lastAngle)
        when {
            stopForwardScroll.not() && isScrollBackward.not() -> {
                horizontalScrollOffset += travel
                for (position in 0 until viewCalculation.size()) {
                    val pos = secondLastVisiblePos - position
                    var angle = -45.0 * pos + horizontalScrollOffset * 0.1
                    angle = if (angle < 0) angle else 0.0
                    val radius: Double = spiralRatio.times(angle)
                    if (position == viewCalculation.size() - 1 && childCount in 2..4)
                        stopForwardScroll =
                            (radius <= lastRadius + 5 && radius >= lastRadius - 5) || radius <= lastRadius
                    if (position == 0)
                        stopBackScroll =
                            (radius <= lastRadius + 5 && radius >= lastRadius - 5) || radius > lastRadius + 5
                    if (lastAngle >= angle - 25 && lastAngle <= angle + 25) {
                        val positionData = calculatePosition(radius, angle)
                        onLayoutDrawn.invoke(centerPoint, secondLastPositionData, position,positionData)
                    }
                    if (stopForwardScroll.not()) {
                        viewCalculation[position].angle = angle
                        viewCalculation.get(position).currentRadius = radius
                    } else {
                        viewCalculation[position].angle = angle
                        viewCalculation.get(position).currentRadius = radius
                    }
                }
                updateViews(recycler)
            }
            stopBackScroll.not() && isScrollBackward -> {
                horizontalScrollOffset += travel
                for (position in 0 until viewCalculation.size()) {
                    val pos = secondLastVisiblePos - position
                    val lastAngle1 = -45.0 * secondLastVisiblePos
                    var angle = -45.0 * pos + horizontalScrollOffset * 0.1
                    angle = if (angle < 0) angle else 0.0
                    val radius: Double = spiralRatio.times(angle)
                    if (position == 0)
                        stopBackScroll =
                            (radius <= lastRadius + 5 && radius >= lastRadius - 5) || radius > lastRadius + 5
                    if (position == viewCalculation.size() - 1)
                        stopForwardScroll =
                            (radius <= lastRadius + 5 && radius >= lastRadius - 5) || radius <= lastRadius
                    if (lastAngle1 >= angle - 25 && lastAngle1 <= angle + 25) {
                        val positionData = calculatePosition(radius, angle)
                        onLayoutDrawn.invoke(centerPoint, secondLastPositionData, position,positionData)
                    }
                    if (stopBackScroll.not()) {
                        viewCalculation[position].angle = angle
                        viewCalculation.get(position).currentRadius = radius
                    } else {
                        horizontalScrollOffset = 0
                        var angle1 = -45.0 * pos
                        angle1 = if (angle1 < 0) angle1 else 0.0
                        val radius1: Double = spiralRatio.times(angle1)
                        viewCalculation[position].angle = angle1
                        viewCalculation.get(position).currentRadius = radius1

                    }

                }
                updateViews(recycler)
            }
            else -> {
                travel = 0
            }
        }

        return travel
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        val travel = dy

        verticalScrollOffset += travel

        offsetChildrenVertical(-travel)
        for (position in 0 until viewCalculation.size()) {
            if (shouldItemMove(position).not()) {
                continue
            }
            val angle = -45.0 * position + verticalScrollOffset * 0.1
            val radius: Double = spiralRatio.times(angle)
            viewCalculation[position].angle = angle
            viewCalculation.get(position).currentRadius = radius
        }
//        calculateScaleData()
        updateViews(recycler)
        return travel
    }

    inner class ItemData(
        val initialRadius: Double? = 0.0,
        var currentRadius: Double? = 0.0,
        var angle: Double? = 0.0
    )

    inner class PositionData(val left: Int, val top: Int, val right: Int, val bottom: Int)


}