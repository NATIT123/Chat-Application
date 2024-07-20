package com.example.chatapplication.Activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.example.chatapplication.MainActivity
import com.example.chatapplication.R
import com.example.chatapplication.Utils.Constants
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_USERS
import com.example.chatapplication.Utils.Constants.Companion.KEY_EMAIL
import com.example.chatapplication.Utils.Constants.Companion.KEY_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_PASSWORD
import com.example.chatapplication.Utils.PreferenceManager
import com.example.chatapplication.databinding.ActivitySignInBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var preferenceManager: PreferenceManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()

        binding.tvCreateNewAccount.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        binding.btnSignIn.setOnClickListener {
            isValidSignIn()
        }

    }


    private fun isValidSignIn() {
        if (binding.edtEmail.text.toString().isEmpty()) {
            showToast("Please enter Email")
            return
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.edtEmail.text.toString()).matches()) {
            showToast("Email not valid")
            return
        } else if (binding.edtPassword.text.toString().isEmpty()) {
            showToast("Please enter Password")
            return
        } else {
            isLoading(true)
            signIn()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@SignInActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun signIn() {
        val email = binding.edtEmail.text.toString().trim()
        val password = binding.edtPassword.text.toString().trim()
        db.collection(KEY_COLLECTION_USERS).whereEqualTo(KEY_EMAIL, email)
            .whereEqualTo(KEY_PASSWORD, password).get().addOnSuccessListener { result ->
                if (result.isEmpty) {
                    isLoading(false)
                    showToast("Email or Password is not correct")
                } else {
                    isLoading(false)
                    val documentSnapShot = result.documents[0]
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                    preferenceManager.putString(
                        Constants.KEY_USER_ID,
                        documentSnapShot.id
                    )
                    preferenceManager.putString(KEY_EMAIL, binding.edtEmail.text.toString())
                    preferenceManager.putString(KEY_NAME, documentSnapShot.get("name").toString())
                    preferenceManager.putString(KEY_IMAGE, documentSnapShot.get("image").toString())
                    val intent = Intent(this@SignInActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    showToast("Login Successfully")
                }
            }

    }


    private fun isLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSignIn.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnSignIn.visibility = View.VISIBLE
        }
    }

}