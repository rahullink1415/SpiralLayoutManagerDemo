package com.rhl.myapplication

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.rhl.myapplication.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var mSecondLastPositionData: CircularRecyclerLayoutManager.PositionData? = null
    var mSnapPositionData: CircularRecyclerLayoutManager.PositionData? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val thingsList: List<String> = listOf(
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24",
            "25",
            "26"
        )
        val itemWidth =
            (resources?.getDimension(R.dimen.item_width) ?: 200f).toInt()

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y

        binding.recyclerview.layoutParams.height = screenHeight /// 2)//
        binding.recyclerview.layoutParams.width = screenWidth /// 2

        //This is how you would usually set up a LinearLayoutManager
//        recyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        //This is using our CustomLayoutManager.
        //Try changing between CustomLayoutManager1, CustomLayoutManager2 etc. to see the differences
        binding.recyclerview.layoutManager = CircularRecyclerLayoutManager(
            canScrollHorizontally = true,
            canScrollVertically = false,
            itemWidth = itemWidth
        ) { _, secondLastPositionData,lastPos,snapPosData ->
            secondLastPositionData?.let {
                Log.e("TAG", "onScrolled: snapPosData  " +
                        "${snapPosData?.top} ${snapPosData?.left}" +
                        "top ${it.top} left ${it.left} ")
                mSecondLastPositionData = it
                mSnapPositionData = snapPosData
                binding.root.post {
                    binding.centerMainView.x = it.left.toFloat().minus(itemWidth.div(3))

                    binding.centerMainView.y = it.bottom.toFloat().plus(35)
                }
                lastPos.let {
                    binding.name.text =thingsList[lastPos]
                }
            }
        }
        binding.recyclerview.adapter = ThingAdapter(thingsList)

        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == 0)
                    mSecondLastPositionData?.let {lastPosData->
                        mSnapPositionData?.let {snapData->
                            val snapX = snapData.left-lastPosData.left
                            val snapY = lastPosData.top-snapData.top
                            recyclerView.smoothScrollBy(snapX,snapY)
//                            recyclerView.scrollBy(snapX,snapY)

                        }
                    }
            }

        })

    }

    override fun onResume() {
        super.onResume()


    }
}

class ThingAdapter(private val thingsList: List<String>) : Adapter<ThingHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThingHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
        return ThingHolder(itemView)
    }

    override fun onBindViewHolder(holder: ThingHolder, position: Int) {
        holder.textView.text = thingsList[position]
    }

    override fun getItemCount(): Int {
        return thingsList.size
    }

}

class ThingHolder(itemView: View) : ViewHolder(itemView) {
    val textView: TextView = itemView.findViewById(R.id.text)
}
