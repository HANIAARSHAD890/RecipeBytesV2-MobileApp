package com.example.recipebytes.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipebytes.R
import com.example.recipebytes.models.User
import de.hdodenhof.circleimageview.CircleImageView

class LikersAdapter(
    private val context: Context,
    private val likers: Map<String, User>
) : RecyclerView.Adapter<LikersAdapter.LikerViewHolder>() {

    private val likerList: List<User> = likers.values.toList()

    class LikerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: CircleImageView = itemView.findViewById(R.id.civProfilePic)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LikerViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_liker, parent, false)
        return LikerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LikerViewHolder, position: Int) {
        val liker = likerList[position]
        holder.username.text = liker.username
        if (liker.profileImage.isNotEmpty()) {
            Glide.with(context)
                .load(liker.profileImage)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(holder.profilePic)
        } else {
            holder.profilePic.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount(): Int = likerList.size
}