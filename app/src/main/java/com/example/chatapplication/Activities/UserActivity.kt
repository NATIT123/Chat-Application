package com.example.chatapplication.Activities

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapplication.models.User
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_USERS
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER_ID
import com.example.chatapplication.Utils.PreferenceManager
import com.example.chatapplication.Adapters.UserAdapter
import com.example.chatapplication.databinding.ActivityUserBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class UserActivity : BaseActivity(), UserAdapter.onClickUserListerner {

    private lateinit var binding: ActivityUserBinding
    private lateinit var mUserActivity: UserAdapter
    private val listUser = mutableListOf<User>()
    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()

        binding.imageBack.setOnClickListener {
            finish()
        }

        getListUser()


    }

    private fun getListUser() {
        binding.isError = false
        binding.isLoading = true
        db.collection(KEY_COLLECTION_USERS).get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                for (data in result) {
                    val user = data.toObject(User::class.java)
                    user.id = data.id
                    if (data.id != preferenceManager.getString(KEY_USER_ID)) {
                        this.listUser.add(user)
                    }
                }
                if (this.listUser.size > 0) {
                    binding.isLoading = false
                    binding.isError = false
                    prepareRecyclerView(this.listUser)
                }
            }

        }.addOnFailureListener {
            binding.tvError.text = it.message
            binding.isError = true
        }
    }

    private fun prepareRecyclerView(listUser: MutableList<User>) {
        mUserActivity = UserAdapter(this)
        binding.rcvUserActivity.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            mUserActivity.differ.submitList(listUser)
            adapter = mUserActivity
            addItemDecoration(
                DividerItemDecoration(
                    applicationContext,
                    DividerItemDecoration.VERTICAL
                )
            )

        }
    }

    override fun onClickUser(user: User) {
        val intent = Intent(this@UserActivity, ChatActivity::class.java)
        val gson = Gson()
        intent.putExtra(KEY_USER, gson.toJson(user))
        startActivity(intent)
    }
}