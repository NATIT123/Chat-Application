package com.example.chatapplication.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapplication.models.User
import com.example.chatapplication.databinding.LayoutContainerUserBinding

class UserAdapter(private val mOnClickUserListener : onClickUserListerner) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {


    private fun getUserImage(url: String): Bitmap {
        val bytes = Base64.decode(url, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    }

    interface onClickUserListerner{
        fun onClickUser(user: User)
    }

    inner class UserViewHolder(val layoutContainerUserBinding: LayoutContainerUserBinding) :
        RecyclerView.ViewHolder(layoutContainerUserBinding.root)

    private val diffUtil = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }

    }

    val differ = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view =
            LayoutContainerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = differ.currentList[position]
        holder.layoutContainerUserBinding.user = user
        holder.layoutContainerUserBinding.imageUser.setImageBitmap(getUserImage(user.image))
        holder.layoutContainerUserBinding.executePendingBindings()
        holder.itemView.setOnClickListener {
            mOnClickUserListener.onClickUser(user)
        }
    }
}