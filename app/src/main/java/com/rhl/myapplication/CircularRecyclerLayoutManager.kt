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
    private var lastViewInVisibleRadius: Double = 0.0
    private val spiralRatio: Double = 0.65
    private var horizontalScrollOffset = 0
    var allowScroll: Boolean = true
    private var verticalScrollOffset = 0
    private val arrayOfRect = mutableListOf<ItemsWithRect>()
    private var bottomView: ItemsWithRect? = null
    var scaleRatio = 0f
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
        centerPoint = PointF(width / 2f, height / 2f)
        for (position in 0 until itemCount) {
            fillAndLayoutItem(position, recycler)
        }
        calculateScaleData()
        scaleViews()
    }

    private fun fillAndLayoutItem(position: Int, recycler: RecyclerView.Recycler?) {
        Log.e("TAG", "fillAndLayoutItem: $position")

        val circleRadius =
            spiralRatio.times(-45 * position)//firstCircleRadius + (itemWidth * 1.5 * circleOrderPosition)

        val angle = -45.0 * position//(angleCalculation + angleOffset + initialAngle)
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

        if (isViewVisible(positionData)) {
            recycler?.getViewForPosition(position)?.let { viewForPosition ->
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


            lastViewInVisibleRadius =
                if (isViewVisible(positionData).not() && lastViewInVisibleRadius > (bottomData?.currentRadius
                        ?: 0.0)
                ) (data?.currentRadius ?: 0.0) else lastViewInVisibleRadius
            Log.e(
                "TAG",
                "layoutItemIfNeeded: position $position isViewVisible ${isViewVisible(positionData)}  " +
                        "currentRadius ${data?.currentRadius ?: 0.0} scaleRatio $scaleRatio  bottomView?.rect?.top ${bottomData?.currentRadius ?: 0.0} lastViewInVisibleRadius $lastViewInVisibleRadius "
            )
            if (isViewVisible(positionData) && data?.currentRadius ?: 0.0 > lastViewInVisibleRadius && data?.currentRadius ?: 0.0 < 0.0) {
                addView(viewForPosition)
                val rect = Rect()
                viewForPosition.getGlobalVisibleRect(rect)
                if (bottomView?.rect == rect) {
                    if ((scaleRatio < 1)) {
                        scaleRatio = 1f
                    }
                    viewForPosition.scaleX = scaleRatio
                    viewForPosition.scaleY = scaleRatio
                    scaleRatio += 0.1f
                } else {
                    viewForPosition.scaleX = scaleRatio
                    viewForPosition.scaleY = scaleRatio
                    scaleRatio += 0.1f
                }
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
        Log.e("TAG", "updateAllChild: childCount $childCount")
//        scaleRatio = 0f
        val bottomData = viewCalculation[arrayOfRect.indexOf(bottomView)]

        for (position in 0 until childCount) {
            getChildAt(position)?.let { childAt ->
                val childPosition = getPosition(childAt)
                val data = viewCalculation[childPosition]

                val positionData = calculatePosition(data.currentRadius ?: 0.0, data.angle ?: 0.0)
                val rect = Rect()
                childAt.getGlobalVisibleRect(rect)
                if (bottomView?.rect == rect) {
                    if ((scaleRatio < 1)) {
                        scaleRatio = 1f
                    }
                Log.e(
                    "TAG",
                    "updateAllChild: childAt.scaleX ${
                        (data.currentRadius ?: 0.0).div(bottomData.currentRadius ?: 0.0).toFloat()
                    }"
                )
//                val scale =
//                    bottomData.currentRadius?.let { data.currentRadius?.div(it) }?.toFloat() ?: 0f
//                if (scale>= -3.4028235E38) {
//                    childAt.scaleX = scale
//                    childAt.scaleY = scale
//                }
                childAt.scaleX = childCount.div(10)+position.div(10f)
                childAt.scaleY =childCount.div(10)+ position.div(10f)
                scaleRatio += 0.1f
                } else {
                    childAt.scaleX = scaleRatio
                    childAt.scaleY = scaleRatio
                    scaleRatio += 0.1f
                }
                if (isViewVisible(positionData).not() || data.currentRadius == 0.0) {
                    viewsForDetaching.add(childAt)
                } else {
                    childAt.updatePosition(positionData)
                }
                updatedPositions.add(childPosition)

            }
        }
    }

    private fun updateCalculation(dy: Int) {
        Log.e("TAG", "updateCalculation: ")

        for (position in 0 until viewCalculation.size()) {
            if (shouldItemMove(position).not()) {
                continue
            }
            val data = viewCalculation.get(position)
            data?.currentRadius = data.currentRadius?.plus(dy * 0.2)
            if (data.currentRadius ?: 0.0 < 0) data.currentRadius = 0.0
            if (data.currentRadius ?: 0.0 > data.initialRadius ?: 0.0) data.currentRadius =
                data.initialRadius
        }
    }

    private fun shouldItemMove(index: Int): Boolean {
        Log.e("TAG", "shouldItemMove: ")

        val itemCurrentRadius = viewCalculation.get(index)?.currentRadius ?: 0.0
        for (position in 0 until itemCount) {
            val currentRadius = viewCalculation.get(position)?.currentRadius ?: 0.0
            val isRadiusBigEnough = currentRadius <= 0.0
            if (isRadiusBigEnough && itemCurrentRadius == 0.0) {
                return false
            }
        }
        Log.e("TAG", "shouldItemMove: $itemCurrentRadius")

        return true
    }


    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        Log.e("TAG", "scrollHorizontallyBy: $dx")
        var travel = dx
        Log.e("TAG", "scrollHorizontallyBy: $dx $horizontalScrollOffset")
        if (horizontalScrollOffset + dx > 0) {
            travel = -horizontalScrollOffset
        } else if (horizontalScrollOffset + dx > totalDistance) {
            travel = (totalDistance - horizontalScrollOffset).toInt()
        }

        horizontalScrollOffset += travel
        Log.e("TAG", "scrollHorizontallyBy: $dx $horizontalScrollOffset")

        offsetChildrenHorizontal(-travel)
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
//        scaleItems()
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

        //将水平方向的偏移量+travel
        verticalScrollOffset += travel

        // 平移容器内的item
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
        updateCalculation(travel)
        updateViews(recycler)
//        scaleItems()
        return travel
    }

    inner class ItemData(
        val initialRadius: Double? = 0.0,
        var currentRadius: Double? = 0.0,
        var angle: Double? = 0.0
    )

    inner class PositionData(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private fun calculateScaleData() {
//        val arrayOfRect = mutableListOf<ItemsWithRect>()
        arrayOfRect.clear()
        for (i in 0 until childCount) {
            val rect = Rect()
            getChildAt(i)?.apply {
                getGlobalVisibleRect(rect)
                arrayOfRect.add(ItemsWithRect(this, rect))
            }
        }


        arrayOfRect.apply {
            bottomView = maxBy { g ->
                g.rect.top //- g.rect.left
            }

            scaleRatio = when {
                arrayOfRect.indexOf(bottomView) < 2 -> {
                    .66f
                }
                arrayOfRect.indexOf(bottomView) < 3 -> {
                    .55f
                }
                arrayOfRect.indexOf(bottomView) < 4 -> {
                    .45f
                }
                arrayOfRect.indexOf(bottomView) < 5 -> {
                    .40f
                }
                arrayOfRect.indexOf(bottomView) < 6 -> {
                    .35f
                }
                arrayOfRect.indexOf(bottomView) < 7 -> {
                    .30f
                }
                arrayOfRect.indexOf(bottomView) < 8 -> {
                    .25f
                }
                else -> {
                    .0f
                }

            }
            Log.e("scaleRatio", "scaleRatio: $scaleRatio  ${arrayOfRect.indexOf(bottomView)}")

            allowScroll = indexOf(bottomView) != childCount - 1
        }


    }

    private fun scaleViews() {
        for (itemsWithRect in arrayOfRect) {
            if (bottomView?.rect == itemsWithRect.rect) {
                if ((scaleRatio < 1)) {
                    scaleRatio = 1f
                }
                itemsWithRect.view.scaleX = scaleRatio
                itemsWithRect.view.scaleY = scaleRatio
                scaleRatio += 0.1f
            } else {
                itemsWithRect.view.scaleX = scaleRatio
                itemsWithRect.view.scaleY = scaleRatio
                scaleRatio += 0.1f
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