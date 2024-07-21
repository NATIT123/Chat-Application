package com.example.chatapplication.Activities

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_CHAT
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_CONVERSATIONS
import com.example.chatapplication.Utils.Constants.Companion.KEY_DATE
import com.example.chatapplication.Utils.Constants.Companion.KEY_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_MESSAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_RECEIVED_ID
import com.example.chatapplication.Utils.Constants.Companion.KEY_RECEIVED_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_RECEIVED_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_SENDER_ID
import com.example.chatapplication.Utils.Constants.Companion.KEY_SENDER_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_SENDER_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER_ID
import com.example.chatapplication.Utils.PreferenceManager
import com.example.chatapplication.Adapters.ChatAdapter
import com.example.chatapplication.Utils.Constants.Companion.KEY_AVAILABILITY
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_USERS
import com.example.chatapplication.databinding.ActivityChatBinding
import com.example.chatapplication.models.ChatMessage
import com.example.chatapplication.models.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.gson.Gson
import java.util.Date
import java.util.Objects

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding

    private var receiverUser: User? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var mChatMessageAdapter: ChatAdapter
    private var listChatMessage = mutableListOf<ChatMessage>()

    private var conversionId: String? = null

    private var isReceiverAvailable = false


    @SuppressLint("NotifyDataSetChanged")
    private val eventListener =
        EventListener<QuerySnapshot> { value, error ->
            if (error != null) {
                return@EventListener
            }
            if (value != null) {
                val count = listChatMessage.size
                for (documentChange in value.documentChanges) {
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val message = documentChange.document.toObject(ChatMessage::class.java)
                        listChatMessage.add(message)
                    }
                }
                listChatMessage.sortedWith(compareBy { it.date })
                mChatMessageAdapter.diff.submitList(listChatMessage)
                if (count == 0) {
                    mChatMessageAdapter.notifyDataSetChanged()
                } else {
                    mChatMessageAdapter.notifyItemRangeChanged(
                        listChatMessage.size,
                        listChatMessage.size
                    )
                    binding.rcvChatMessage.smoothScrollToPosition(listChatMessage.size - 1)
                }
                binding.rcvChatMessage.visibility = View.VISIBLE
            }
            binding.progressBar.visibility = View.GONE
            if (conversionId == null) {
                checkForConversion()
            }
        }

    private fun listenAvailabilityOfReceiver() {
        db.collection(KEY_COLLECTION_USERS).document(receiverUser?.id!!)
            .addSnapshotListener(this@ChatActivity) { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    if (value.getLong(KEY_AVAILABILITY) != null) {
                        val availability = Objects.requireNonNull(
                            value.getLong(KEY_AVAILABILITY)
                        )?.toInt()
                        isReceiverAvailable = availability == 1
                    }
                }
                if (isReceiverAvailable) {
                    binding.textAvailability.visibility = View.VISIBLE
                } else {
                    binding.textAvailability.visibility = View.GONE
                }
            }
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }

    private fun listenMessages() {
        db.collection(KEY_COLLECTION_CHAT)
            .whereEqualTo(KEY_SENDER_ID, preferenceManager.getString(KEY_USER_ID))
            .whereEqualTo(KEY_RECEIVED_ID, receiverUser?.id).addSnapshotListener(eventListener)

        db.collection(KEY_COLLECTION_CHAT)
            .whereEqualTo(KEY_SENDER_ID, receiverUser?.id)
            .whereEqualTo(KEY_RECEIVED_ID, preferenceManager.getString(KEY_USER_ID))
            .addSnapshotListener(eventListener)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)


        db = FirebaseFirestore.getInstance()
        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()
        binding.btnBack.setOnClickListener {
            finish()
        }

        loadUser()
        prepareRecyclerView()

        listenMessages()

        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

    }

    private fun prepareRecyclerView() {
        mChatMessageAdapter = ChatAdapter(
            preferenceManager.getString(KEY_USER_ID)!!,
            getUserImage(receiverUser?.image!!)
        )

        binding.rcvChatMessage.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = mChatMessageAdapter
            addItemDecoration(
                DividerItemDecoration(
                    applicationContext,
                    DividerItemDecoration.VERTICAL
                )
            )

        }
    }

    private fun sendMessage() {
        val message = binding.edtMessage.text.toString()
        if (message.isEmpty()) {
            Toast.makeText(this@ChatActivity, "Please input your message", Toast.LENGTH_SHORT)
                .show()
        } else {
            val chatData = hashMapOf(
                KEY_SENDER_ID to preferenceManager.getString(KEY_USER_ID),
                KEY_RECEIVED_ID to receiverUser?.id!!,
                KEY_MESSAGE to message,
                KEY_DATE to Date()
            )
            db.collection(KEY_COLLECTION_CHAT).add(chatData).addOnSuccessListener {
                binding.edtMessage.setText("")
            }
            if (conversionId != null) {
                updateConversion(message)
            } else {
                val conversion = hashMapOf<String, Any>(
                    KEY_SENDER_ID to preferenceManager.getString(KEY_USER_ID)!!,
                    KEY_SENDER_NAME to preferenceManager.getString(KEY_NAME)!!,
                    KEY_SENDER_IMAGE to preferenceManager.getString(KEY_IMAGE)!!,
                    KEY_RECEIVED_ID to receiverUser?.id!!,
                    KEY_RECEIVED_NAME to receiverUser?.name!!,
                    KEY_RECEIVED_IMAGE to receiverUser?.image!!,
                    KEY_MESSAGE to message,
                    KEY_DATE to Date()
                )
                addConversion(conversion)
            }
        }

    }

    private fun addConversion(conversion: HashMap<String, Any>) {
        db.collection(KEY_COLLECTION_CONVERSATIONS).add(conversion).addOnSuccessListener {
            conversionId = it.id
        }
    }

    private fun updateConversion(message: String) {
        val hashMap = hashMapOf(
            KEY_MESSAGE to message,
            KEY_DATE to Date()
        )
        db.collection(KEY_COLLECTION_CONVERSATIONS).document(conversionId!!)
            .update(hashMap as Map<String, Any>)
    }

    private fun getUserImage(url: String): Bitmap {
        val bytes = Base64.decode(url, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    }


    private fun loadUser() {
        binding.progressBar.visibility = View.VISIBLE
        val data = intent?.getStringExtra(KEY_USER)
        val gson = Gson()
        val user = gson.fromJson(data, User::class.java)
        receiverUser = user
        if (user != null) {
            binding.progressBar.visibility = View.GONE
            binding.tvNameUser.text = user.name
        }
    }

    private fun checkForConversion() {
        if (listChatMessage.size != 0) {
            checkForConversionRemotely(
                preferenceManager.getString(KEY_USER_ID)!!,
                receiverUser?.id!!
            )
            checkForConversionRemotely(
                receiverUser?.id!!,
                preferenceManager.getString(KEY_USER_ID)!!
            )
        }
    }

    private fun checkForConversionRemotely(senderId: String, receiverId: String) {
        db.collection(KEY_COLLECTION_CONVERSATIONS).whereEqualTo(KEY_SENDER_ID, senderId)
            .whereEqualTo(KEY_RECEIVED_ID, receiverId).get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }

    private val conversionOnCompleteListener =
        OnCompleteListener<QuerySnapshot> { task ->
            if (task.isSuccessful && task.result != null && task.result.documents.size > 0) {
                val documentSnapShot = task.result.documents[0]
                conversionId = documentSnapShot.id
            }

        }


}