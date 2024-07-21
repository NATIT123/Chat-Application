package com.example.chatapplication.Adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapplication.databinding.ItemContainerRecentConversionBinding
import com.example.chatapplication.models.ChatMessage
import com.example.chatapplication.models.User

class RecentConversationsAdapter(private val mOnClickUserListener: onClickUserListerner) :
    RecyclerView.Adapter<RecentConversationsAdapter.ConversationViewHolder>() {

    interface onClickUserListerner {
        fun onClickUser(user: User)
    }

    private fun getUserImage(url: String): Bitmap {
        val bytes = Base64.decode(url, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    }

    inner class ConversationViewHolder(val itemContainerRecentConversionBinding: ItemContainerRecentConversionBinding) :
        RecyclerView.ViewHolder(itemContainerRecentConversionBinding.root)


    private val diffUtil = object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.conversionId == newItem.conversionId
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }

    }

    val diff = AsyncListDiffer(this, diffUtil)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = ItemContainerRecentConversionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConversationViewHolder(view)
    }

    override fun getItemCount(): Int {
        return diff.currentList.size
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val message = diff.currentList[position]
        holder.itemContainerRecentConversionBinding.message = message
        holder.itemContainerRecentConversionBinding.imageUser.setImageBitmap(getUserImage(message.conversionImage))
        holder.itemContainerRecentConversionBinding.executePendingBindings()
        holder.itemView.setOnClickListener {
            val user = User()
            user.id = message.conversionId
            user.name = message.conversionName
            user.image = message.conversionImage
            mOnClickUserListener.onClickUser(user)
        }
    }
}