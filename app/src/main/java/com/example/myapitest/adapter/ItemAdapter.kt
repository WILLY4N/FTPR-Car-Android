package com.example.myapitest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.model.Car
import com.example.myapitest.model.ItemCar
import com.example.myapitest.ui.loadUrl

class ItemAdapter(
    private val cars: List<ItemCar>,
    private val onItemClick: (ItemCar) -> Unit,
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val modelTextView = view.findViewById<TextView>(R.id.model)
        val licenseTextView = view.findViewById<TextView>(R.id.license)
        val yearTextView = view.findViewById<TextView>(R.id.year)
        val imageView = view.findViewById<ImageView>(R.id.image)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val car = cars[position]
        holder.modelTextView.text = car.name
        holder.licenseTextView.text = car.licence
        holder.yearTextView.text = car.year
        holder.imageView.loadUrl(car.imageUrl)
        holder.itemView.setOnClickListener {
            onItemClick(car)
        }
    }

    override fun getItemCount(): Int  = cars.size

}
