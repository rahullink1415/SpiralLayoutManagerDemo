package com.rhl.myapplication

import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CircularRecyclerLayoutManager(
    private val canScrollHorizontally: Boolean = true,
    private val canScrollVertically: Boolean = false
) : RecyclerView.LayoutManager() {
    private var isScrollBackward: Boolean =false

    //    private var lastViewInVisibleRadius: Double = 0.0
    private val spiralRatio: Double = 0.65
    private var horizontalScrollOffset = 0
    private var verticalScrollOffset = 0
    private val arrayOfRect = mutableListOf<ItemsWithRect>()
    private var bottomView: ItemsWithRect? = null


    override fun generateDefaultLayoutParams() =
        RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.MATCH_PARENT
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

    private var itemWidth = 200
    private var totalDistance = 0f

    private val viewCalculation = SparseArray<ItemData>(itemCount)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        Log.e("TAG", "onLayoutChildren: ")
        centerPoint = PointF(width / 1.85f, height / 2f)
        for (position in 0 until itemCount) {
            fillAndLayoutItem(position, recycler)
        }
        calculateScaleData()
    }

    private fun fillAndLayoutItem(position: Int, recycler: RecyclerView.Recycler?) {
        Log.e("TAG", "fillAndLayoutItem: $position")


        val angle = -45.0 * position
        val circleRadius =
            spiralRatio.times(angle)
        val positionData = calculatePosition(circleRadius, angle)
        if (position == 0) {
            totalDistance += (0.32f * getUnitSpace())
        } else if (position == itemCount - 1) {
            totalDistance += (-0.29f * getUnitSpace())
        }

        if (position > 0) {
            totalDistance += (.09f * getUnitSpace())
        }
        viewCalculation.put(position, ItemData(circleRadius, circleRadius, angle))
        val calculatedRatio =
            circleRadius.div(spiralRatio).div(-45).toInt().div(10f)
        if (isViewVisible(positionData)&&calculatedRatio<2.0) {
            recycler?.getViewForPosition(position)?.let { viewForPosition ->

                viewForPosition.scaleX=calculatedRatio
                viewForPosition.scaleY=calculatedRatio
                addView(viewForPosition)

                measureChildWithMargins(viewForPosition, itemWidth, itemWidth)
                layoutDecoratedWithMargins(viewForPosition, positionData)

            }
        }
    }

    private fun calculatePosition(radius: Double, angle: Double): PositionData {
        val xCoordinate = (radius * cosAngle(angle)) + centerPoint.x
        val yCoordinate = (radius * sinAngle(angle)) + centerPoint.y
        val top = yCoordinate - (itemWidth / 2)
        val left = xCoordinate - (itemWidth / 2)
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


    private fun updateViews(recycler: RecyclerView.Recycler?) {
        val updatedPositions = mutableListOf<Int>()
        val viewsForDetaching = mutableListOf<View>()
        Log.e("TAG", "updateViews: ")

        updateAllChild(viewsForDetaching, updatedPositions)
        val bottomData = viewCalculation[arrayOfRect.indexOf(bottomView)]

        for (position in 0 until itemCount) {
            if (updatedPositions.contains(position)) continue
            val data = viewCalculation[position]
            val positionData = calculatePosition(data.currentRadius ?: 0.0, data.angle ?: 0.0)
            layoutItemIfNeeded(positionData, data, recycler, position)
        }

        recycler?.let { viewsForDetaching.forEach { detachAndScrapView(it, recycler) } }
        viewsForDetaching.clear()
    }

    private fun layoutItemIfNeeded(
        positionData: PositionData,
        data: ItemData?,
        recycler: RecyclerView.Recycler?,
        position: Int
    ) {
//        Log.e("TAG", "layoutItemIfNeeded: ")
        val bottomData = viewCalculation[arrayOfRect.indexOf(bottomView)]

        recycler?.getViewForPosition(position)?.let { viewForPosition ->

            val calculatedRatio =
                ( data?.currentRadius ?: 0.0).div(spiralRatio).div(-45).toInt().div(10f)
            Log.e(
                "TAG",
                "layoutItemIfNeeded: position $position isViewVisible ${isViewVisible(positionData)}  " +
                        "currentRadius ${data?.currentRadius ?: 0.0}   bottomView?.rect?.top ${bottomData?.currentRadius ?: 0.0}  "
            )
            if (isViewVisible(positionData) && calculatedRatio <2.0) {
                addView(viewForPosition)
                viewForPosition.scaleX = calculatedRatio
                viewForPosition.scaleY = calculatedRatio

                measureChildWithMargins(viewForPosition, itemWidth, itemWidth)
                layoutDecoratedWithMargins(viewForPosition, positionData)

                Log.e("TAG", "layoutItemIfNeeded: $position ")

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

                Log.e(
                    "TAG",
                    "updateAllChild: childAt.scaleX  calculatedRatio $calculatedRatio  pos $position  "
                )
                if (isViewVisible(positionData).not() || data.currentRadius == 0.0||calculatedRatio>2.0) {
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
        Log.e("TAG", "shouldItemMove: $itemCurrentRadius calculatedRatio $calculatedRatio index $index isScrollBackward $isScrollBackward")

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
//        Log.e("TAG", "scrollHorizontallyBy: $dx")
        var travel = dx
        isScrollBackward = dx >= 0
        Log.e("TAG", "scrollHorizontallyBy: $dx $horizontalScrollOffset")
        if (childCount>2 || isScrollBackward) {
            if (horizontalScrollOffset + dx > 0) {
                travel = -horizontalScrollOffset
            } else if (horizontalScrollOffset + dx > totalDistance) {
                travel = (totalDistance - horizontalScrollOffset).toInt()
            }

            horizontalScrollOffset += travel
            Log.e("TAG", "scrollHorizontallyBy: $dx $horizontalScrollOffset")
            for (position in 0 until viewCalculation.size()) {
                if (shouldItemMove(position).not()) {
                    continue
                }
                val angle = -45.0 * position + horizontalScrollOffset * 0.1
                val radius: Double = spiralRatio.times(angle)
                viewCalculation[position].angle = angle
                viewCalculation.get(position).currentRadius = radius
            }
            calculateScaleData()
            updateViews(recycler)
        }else{
            travel =0
        }
//        offsetChildrenHorizontal(-travel)

        return travel
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        Log.e("TAG", "scrollVerticallyBy: $dy")

        var travel = dy

        if (verticalScrollOffset + dy < 0) {
            travel = -verticalScrollOffset
        } else if (verticalScrollOffset + dy > totalDistance) {
            travel = (totalDistance - verticalScrollOffset).toInt()
        }

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
        calculateScaleData()
        updateViews(recycler)
        return travel
    }

    inner class ItemData(
        val initialRadius: Double? = 0.0,
        var currentRadius: Double? = 0.0,
        var angle: Double? = 0.0
    )

    inner class PositionData(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun calculateScaleData() {
        arrayOfRect.clear()
        for (i in 0 until childCount) {
            val rect = Rect()
            getChildAt(i)?.apply {
                getGlobalVisibleRect(rect)
                arrayOfRect.add(ItemsWithRect(this, rect))
            }
        }

    }


    data class ItemsWithRect(
        val view: View,
        val rect: Rect
    )

    private fun getUnitSpace(): Int {
        return (if (canScrollHorizontally()) {
            getHorizontalSpace()
        } else {
            getVerticalSpace()
        })
    }

    private fun getHorizontalSpace(): Int {
        return width - paddingLeft - paddingRight
    }

    private fun getVerticalSpace(): Int {
        return height - paddingBottom - paddingTop
    }
}