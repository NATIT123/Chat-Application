package com.example.chatapplication.Activities

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapplication.Utils.Constants.Companion.KEY_AVAILABILITY
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_USERS
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER_ID
import com.example.chatapplication.Utils.PreferenceManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

open class BaseActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var dbRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()
        db = FirebaseFirestore.getInstance()
        dbRef = db.collection(KEY_COLLECTION_USERS).document(preferenceManager.getString(KEY_USER_ID)!!)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()
        db = FirebaseFirestore.getInstance()
        dbRef = db.collection(KEY_COLLECTION_USERS).document(preferenceManager.getString(KEY_USER_ID)!!)
        dbRef.update(KEY_AVAILABILITY, 1)
    }

    override fun onPause() {
        super.onPause()
        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()
        db = FirebaseFirestore.getInstance()
        dbRef = db.collection(KEY_COLLECTION_USERS).document(preferenceManager.getString(KEY_USER_ID)!!)
        dbRef.update(KEY_AVAILABILITY, 0)
    }
}