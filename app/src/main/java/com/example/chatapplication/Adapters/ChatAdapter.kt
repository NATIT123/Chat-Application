package com.example.chatapplication.Adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapplication.Utils.Constants.Companion.VIEW_TYPE_RECEIVED
import com.example.chatapplication.Utils.Constants.Companion.VIEW_TYPE_SENT
import com.example.chatapplication.databinding.ItemContainerReceivedMessageBinding
import com.example.chatapplication.databinding.ItemContainerSentMessageBinding
import com.example.chatapplication.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(val senderId: String, val imageReceiver: Bitmap) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    inner class SentMessageViewHolder(val itemContainerSentMessageBinding: ItemContainerSentMessageBinding) :
        RecyclerView.ViewHolder(itemContainerSentMessageBinding.root)

    inner class ReceiveMessageViewHolder(val itemContainerReceivedMessageBinding: ItemContainerReceivedMessageBinding) :
        RecyclerView.ViewHolder(itemContainerReceivedMessageBinding.root)

    private val diffUtil = object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.date == newItem.date
        }
    }

    val diff = AsyncListDiffer(this, diffUtil)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val bindingFeature = ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SentMessageViewHolder(bindingFeature)
            }

            else -> {
                val bindingFeature = ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReceiveMessageViewHolder(bindingFeature)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chatMessage = diff.currentList[position]
        if (chatMessage.senderId == senderId) {
            return VIEW_TYPE_SENT
        }
        return VIEW_TYPE_RECEIVED
    }

    override fun getItemCount(): Int {
        return diff.currentList.size
    }

    private fun convertDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy - hh::mm::ss ", Locale.getDefault()).format(date)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = diff.currentList[position]
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            val sentViewHolder = holder as SentMessageViewHolder
            sentViewHolder.itemContainerSentMessageBinding.apply {
                tvSentMessage.text = chatMessage.message
                tvDateTime.text = convertDate(chatMessage.date)
            }
        } else {
            val receivedViewHolder = holder as ReceiveMessageViewHolder
            receivedViewHolder.itemContainerReceivedMessageBinding.apply {
                imageUser.setImageBitmap(imageReceiver)
                tvReceiveMessage.text = chatMessage.message
                tvDateTime.text = convertDate(chatMessage.date)
            }
        }


    }
}