package com.rhl.myapplication

import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.rhl.myapplication.CircularRecyclerLayoutManager.Companion.lastPositionData

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val thingsList : List<String> = listOf("1","2", "3","4","5")//,"6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24","25","26")

        val recyclerView: RecyclerView = findViewById(R.id.recyclerview)

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y

        recyclerView.layoutParams.height = screenHeight /// 2
        recyclerView.layoutParams.width = screenWidth /// 2

        //This is how you would usually set up a LinearLayoutManager
//        recyclerview.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        //This is using our CustomLayoutManager.
        //Try changing between CustomLayoutManager1, CustomLayoutManager2 etc. to see the differences
        recyclerView.layoutManager =CircularRecyclerLayoutManager(this,
            canScrollHorizontally = true,
            canScrollVertically = false
        )
        recyclerView.adapter = ThingAdapter(thingsList
        )
//        recyclerView.smoothScrollToPosition(18)
         val itemWidth = (resources?.getDimension(R.dimen.item_width) ?: 200f).toInt()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                Log.e("TAG", "onScrolled: called $dx")
                lastPositionData?.let {
                    recyclerView.scrollTo(it.left+itemWidth/2,it.top+itemWidth/2)
                }
            }

        })


    }
}

class ThingAdapter(private val thingsList: List<String>) : Adapter<ThingHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThingHolder {
        val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false)
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
