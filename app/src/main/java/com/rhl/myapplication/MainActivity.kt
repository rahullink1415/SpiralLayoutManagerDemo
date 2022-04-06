package com.rhl.myapplication

import android.R.attr.x
import android.R.attr.y
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
        ) { _, secondLastPositionData,lastPos ->
            Log.e("TAG", "onCreate: CircularRecyclerLayoutManager" + "  invoke")
            secondLastPositionData?.let {
                mSecondLastPositionData = it
                binding.root.post {
                    binding.centerMainView.x = it.left.toFloat().minus(itemWidth.div(3))

                    binding.centerMainView.y = it.bottom.toFloat().plus(35)
                }
                lastPos.let {
                    binding.name.text =thingsList[lastPos]
                }
                Log.e(
                    "TAG",
                    "onCreate: CircularRecyclerLayoutManager" + "  inside view " + it.bottom
                )
            }
        }
        binding.recyclerview.adapter = ThingAdapter(
            thingsList
        )


//        recyclerView.smoothScrollToPosition(18)
//        val itemWidth = (resources?.getDimension(R.dimen.item_width) ?: 200f).toInt()

        binding.recyclerview.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                mSecondLastPositionData?.let {
                    val itemView = binding.recyclerview.findChildViewUnder(
                        it.top.toFloat().minus(itemWidth),
                        it.left.toFloat().plus(itemWidth.div(2)).toFloat()
                    )
                    Log.e("TAG", "onScrolled: itemView $itemView " +
                            "${it.top.toFloat().minus(itemWidth.div(3))} ${it.left.toFloat().plus(itemWidth.div(3))}" +
                            "top ${it.top} left ${it.left} ")

                    if (itemView != null) {
                        val pos = binding.recyclerview.getChildLayoutPosition(itemView)
                        Log.e("TAG", "onScrolled: pos $pos")

                        binding.name.text =thingsList[pos]
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
