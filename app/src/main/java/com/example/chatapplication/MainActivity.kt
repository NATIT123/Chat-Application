package com.example.chatapplication

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.Contacts
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapplication.Activities.ChatActivity
import com.example.chatapplication.Activities.SignInActivity
import com.example.chatapplication.Activities.UserActivity
import com.example.chatapplication.Utils.Constants
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_CONVERSATIONS
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_USERS
import com.example.chatapplication.Utils.Constants.Companion.KEY_FCM_TOKEN
import com.example.chatapplication.Utils.Constants.Companion.KEY_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_RECEIVED_ID
import com.example.chatapplication.Utils.Constants.Companion.KEY_RECEIVED_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_RECEIVED_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_SENDER_ID
import com.example.chatapplication.Utils.Constants.Companion.KEY_SENDER_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_SENDER_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER_ID
import com.example.chatapplication.Utils.PreferenceManager
import com.example.chatapplication.adapters.RecentConversationsAdapter
import com.example.chatapplication.databinding.ActivityMainBinding
import com.example.chatapplication.models.ChatMessage
import com.example.chatapplication.models.User
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson

class MainActivity : AppCompatActivity(), RecentConversationsAdapter.onClickUserListerner {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var db: FirebaseFirestore
    private val conversations = mutableListOf<ChatMessage>()
    private lateinit var mRecentConversationsAdapter: RecentConversationsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()

        db = FirebaseFirestore.getInstance()

        loadUserDetail()

        getToken()

        binding.btnSignOut.setOnClickListener {
            signOut()
        }

        binding.btnFloatingActionButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, UserActivity::class.java))
        }

        prepareRecyclerView()
        listenConversations()


    }

    private fun listenConversations() {
        db.collection(KEY_COLLECTION_CONVERSATIONS).whereEqualTo(
            KEY_SENDER_ID, preferenceManager.getString(
                KEY_USER_ID
            )
        ).addSnapshotListener(eventListener)

        db.collection(KEY_COLLECTION_CONVERSATIONS).whereEqualTo(
            KEY_RECEIVED_ID, preferenceManager.getString(
                KEY_RECEIVED_ID
            )
        ).addSnapshotListener(eventListener)
    }

    private fun prepareRecyclerView() {
        mRecentConversationsAdapter = RecentConversationsAdapter(this)
        binding.rcvActivity.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = mRecentConversationsAdapter
            addItemDecoration(
                DividerItemDecoration(
                    applicationContext,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val eventListener =
        EventListener<QuerySnapshot> { value, error ->
            if (error != null) {
                return@EventListener
            }
            if (value != null) {
                for (documentChange in value.documentChanges) {
                    val message = documentChange.document.toObject(ChatMessage::class.java)
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        if (preferenceManager.getString(KEY_USER_ID) == message.senderId) {
                            message.conversionImage = documentChange.document.getString(
                                KEY_RECEIVED_IMAGE
                            ).toString()
                            message.conversionName =
                                documentChange.document.getString(KEY_RECEIVED_NAME).toString()

                            message.conversionId = message.receiverId
                        } else {
                            message.conversionImage = documentChange.document.getString(
                                KEY_SENDER_IMAGE
                            ).toString()
                            message.conversionName =
                                documentChange.document.getString(KEY_SENDER_NAME).toString()

                            message.conversionId = message.senderId
                        }
                        conversations.add(message)
                    } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                        for (i in 0..conversations.size) {
                            if (conversations[i].senderId == message.senderId && conversations[i].receiverId == message.receiverId) {
                                conversations[i].message = message.message
                                conversations[i].date = message.date
                                break
                            }
                        }
                    }
                }
                conversations.sortedWith(compareBy { it.date })
                mRecentConversationsAdapter.diff.submitList(conversations)
                mRecentConversationsAdapter.notifyDataSetChanged()
                binding.rcvActivity.smoothScrollToPosition(0)
                binding.rcvActivity.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE

            }
        }

    private fun loadUserDetail() {
        binding.progressBar.visibility = View.GONE
        binding.tvNameUser.text = preferenceManager.getString(KEY_NAME)
        val bytes = Base64.decode(preferenceManager.getString(KEY_IMAGE), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imageUser.setImageBitmap(bitmap)
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(this::updateToken)
    }


    private fun updateToken(token: String) {
        val dbRef = db.collection(KEY_COLLECTION_USERS).document(
            preferenceManager.getString(
                KEY_USER_ID
            )!!
        )
        dbRef.update(KEY_FCM_TOKEN, token)
            .addOnSuccessListener { showToast("Token Update Successfully") }
            .addOnFailureListener {
                showToast("Unable to update token")
            }
    }

    private fun signOut() {

        val dialog = AlertDialog.Builder(this)
        dialog.apply {
            setTitle("Confirm Logout")
            setMessage("Do you want to log out")
            setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }

            setPositiveButton("Yes") { _, _ ->
                showToast("Signing out...")
                val dbRef = db.collection(KEY_COLLECTION_USERS).document(
                    preferenceManager.getString(
                        KEY_USER_ID
                    )!!
                )
                val hashMap = hashMapOf(
                    KEY_FCM_TOKEN to FieldValue.delete()
                )
                dbRef.update(hashMap as Map<String, Any>).addOnSuccessListener {
                    preferenceManager.clear()
                    startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                    finish()
                }.addOnFailureListener {
                    showToast("Unable to sign out")
                }
            }
        }
        dialog.show()


    }

    override fun onClickUser(user: User) {
        val intent = Intent(this@MainActivity, ChatActivity::class.java)
        val gson = Gson()
        intent.putExtra(Constants.KEY_USER, gson.toJson(user))
        startActivity(intent)
    }


}