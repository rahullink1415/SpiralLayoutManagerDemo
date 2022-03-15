package com.rhl.myapplication

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

class CircularRecyclerLayoutManager(
    private val context: Context?,
    private val canScrollHorizontally: Boolean = true,
    private val canScrollVertically: Boolean = false
) : RecyclerView.LayoutManager() {
    companion object {
         var lastPositionData: PositionData? = null
    }
    private var lastVisiblePos: Int = 0
    private var isScrollBackward: Boolean = false
    private var stopBackScroll: Boolean = true

    //    private var lastViewInVisibleRadius: Double = 0.0
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

    private val itemWidth = (context?.resources?.getDimension(R.dimen.item_width) ?: 200f).toInt()

    private val viewCalculation = SparseArray<ItemData>(itemCount)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        Log.e("TAG", "onLayoutChildren: itemWidth $itemWidth")
        centerPoint = PointF(width / 1.85f, height / 2f)
        for (pos in 0..30) {
            val angle = -45.0 * pos
            val circleRadius =
                spiralRatio.times(angle)
            val data = calculatePosition(circleRadius, angle)
            val calculatedRatio =
                circleRadius.div(spiralRatio).div(-45).toInt().div(10f)
            if (isViewVisible(data) && calculatedRatio < 2.0) {
                lastVisiblePos = pos - 1
                lastPositionData = calculatePosition(circleRadius, angle)
            }
        }
        for (position in 0 until itemCount) {
            fillAndLayoutItem(position, recycler)
        }

    }

    private fun fillAndLayoutItem(position: Int, recycler: RecyclerView.Recycler?) {
        Log.e("TAG", "fillAndLayoutItem: $position")

        val pos = lastVisiblePos - position

//        val angle = -45.0 * if (pos >= 0) pos else 0
        var angle = -45.0 * pos
        angle = if (angle < 0) angle else 0.0
        Log.e("TAG", "fillAndLayoutItem: pos $pos angle $angle")
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

    override fun isAutoMeasureEnabled(): Boolean {
        return false
    }
/*
    override fun onDetachedFromWindow(view: RecyclerView?, recycler: Recycler) {
        super.onDetachedFromWindow(view, recycler)
//        if (mRecycleChildrenOnDetach) {
        removeAndRecycleAllViews(recycler)
        recycler.clear()
//        }
    }*/

    private fun updateViews(recycler: RecyclerView.Recycler?) {
        val updatedPositions = mutableListOf<Int>()
        val viewsForDetaching = mutableListOf<View>()

        updateAllChild(viewsForDetaching, updatedPositions)
        Log.e("TAG", "updateViews: ")

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
            Log.e(
                "TAG",
                "layoutItemIfNeeded: position $position isViewVisible ${isViewVisible(positionData)}  " +
                        "currentRadius ${data?.currentRadius ?: 0.0}   "
            )
            if (isViewVisible(positionData) && calculatedRatio > 0.0 && calculatedRatio < 2.0) {
                addView(viewForPosition)
                viewForPosition.scaleX = calculatedRatio
                viewForPosition.scaleY = calculatedRatio
                viewsForDetaching.remove(viewForPosition)
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
                Log.e("TAG", "  $calculatedRatio  pos $position  ")
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
        Log.e(
            "TAG",
            "shouldItemMove: $itemCurrentRadius calculatedRatio $calculatedRatio index $index isScrollBackward $isScrollBackward"
        )

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
        val lastAngle = -45.0 * lastVisiblePos + 1
        val lastRadius = spiralRatio.times(lastAngle)


        Log.e(
            "TAG",
            "scrollHorizontallyBy: $dx $horizontalScrollOffset childCount $childCount itemCount $itemCount lastRadius $lastRadius stopBackScroll $stopBackScroll "
        )
        if ((childCount > 2 && isScrollBackward.not()) || (stopBackScroll.not() && isScrollBackward)) {

            horizontalScrollOffset += travel
            for (position in 0 until viewCalculation.size()) {
                val pos = lastVisiblePos  - position
                val lastAngle = -45.0 * lastVisiblePos
//                var angle = -45.0 * (if (pos >= 0) pos else 0) + horizontalScrollOffset * 0.1
                var angle = -45.0 * pos + horizontalScrollOffset * 0.1
                angle = if (angle < 0) angle else 0.0
                val radius: Double = spiralRatio.times(angle)
                if (position == 0)
                    stopBackScroll =( radius <= lastRadius + 5 && radius >= lastRadius - 5)||radius>lastRadius+5


                Log.e(
                    "TAG",
                    "scrollHorizontallyBy : position $position  pos $pos  angle $angle lastRadius $lastRadius radius $radius lastAngle $lastAngle  horizontalScrollOffset $horizontalScrollOffset"
                )
//                if (lastAngle.toInt()!=angle.toInt()) {
                if (stopBackScroll.not()) {
                    viewCalculation[position].angle = angle
                    viewCalculation.get(position).currentRadius = radius
                }else{
                    horizontalScrollOffset =0
                    var angle1 = -45.0 * pos
                    angle1 = if (angle1 < 0) angle1 else 0.0
                    val radius1: Double = spiralRatio.times(angle1)
                    viewCalculation[position].angle = angle1
                    viewCalculation.get(position).currentRadius = radius1

                }
//                }

            }
            updateViews(recycler)
        } else {
            travel = 0
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

/*    private fun calculateScaleData() {
        arrayOfRect.clear()
        for (i in 0 until childCount) {
            val rect = Rect()
            getChildAt(i)?.apply {
                getGlobalVisibleRect(rect)
                arrayOfRect.add(ItemsWithRect(this, rect))
            }
        }

    }*/


}