package com.example.amazonwhat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class Item(
    val imageUrl: String,
    val brand: String,
    val name: String,
    val price: Double
)

class OptionAdapter(
    private val options: MutableList<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<OptionAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size

    fun updateOptions(newOptions: List<Item>) {
        options.clear()
        options.addAll(newOptions)
        notifyDataSetChanged()
    }

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivOptionImage: ImageView = itemView.findViewById(R.id.ivOptionImage)
        private val tvOptionBrand: TextView = itemView.findViewById(R.id.tvOptionBrand)
        private val tvOptionName: TextView = itemView.findViewById(R.id.tvOptionName)

        fun bind(item: Item) {
            tvOptionBrand.text = item.brand
            tvOptionName.text = item.name

            Glide.with(itemView.context)
                .load(item.imageUrl)
                .into(ivOptionImage)

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
