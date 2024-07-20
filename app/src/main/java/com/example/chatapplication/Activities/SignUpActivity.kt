package com.example.chatapplication.Activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.chatapplication.MainActivity
import com.example.chatapplication.Utils.Constants.Companion.KEY_COLLECTION_USERS
import com.example.chatapplication.Utils.Constants.Companion.KEY_EMAIL
import com.example.chatapplication.Utils.Constants.Companion.KEY_IMAGE
import com.example.chatapplication.Utils.Constants.Companion.KEY_IS_SIGNED_IN
import com.example.chatapplication.Utils.Constants.Companion.KEY_NAME
import com.example.chatapplication.Utils.Constants.Companion.KEY_PASSWORD
import com.example.chatapplication.Utils.Constants.Companion.KEY_USER_ID
import com.example.chatapplication.Utils.PreferenceManager
import com.example.chatapplication.databinding.ActivitySignUpBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import kotlin.math.ceil

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding

    private var encodeImage: String? = null

    private lateinit var db: FirebaseFirestore

    private lateinit var preferenceManager: PreferenceManager


    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                if (result.data != null) {
                    val imageUri = result.data!!.data
                    try {
                        val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.tvAddImage.visibility = View.GONE
                        binding.imageProfile.setImageBitmap(bitmap)
                        encodeImage = encodeImage(bitmap)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)


        db = FirebaseFirestore.getInstance()

        preferenceManager = PreferenceManager(applicationContext)
        preferenceManager.instance()



        binding.tvHaveAccount.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        binding.btnSignIn.setOnClickListener {
            runBlocking {
                isValidSignUpDetails()
            }
        }

        binding.imageAdd.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
    }

    private fun signUp() {
        isLoading(true)
        val user = hashMapOf<String, Any?>(
            KEY_NAME to binding.edtName.text.toString(),
            KEY_EMAIL to binding.edtEmail.text.toString(),
            KEY_PASSWORD to binding.edtPassword.text.toString(),
            KEY_IMAGE to encodeImage
        )

        db.collection(KEY_COLLECTION_USERS).add(user).addOnSuccessListener {
            isLoading(false)
            preferenceManager.putBoolean(KEY_IS_SIGNED_IN, true)
            preferenceManager.putString(KEY_USER_ID, it.id)
            preferenceManager.putString(KEY_NAME, binding.edtName.text.toString())
            encodeImage?.let { it1 -> preferenceManager.putString(KEY_IMAGE, it1) }
            val intent = Intent(this@SignUpActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            Toast.makeText(this@SignUpActivity, "Add User Successfully", Toast.LENGTH_SHORT).show()
        }
            .addOnFailureListener {
                isLoading(false)
                Toast.makeText(this@SignUpActivity, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun showToast(message: String) {
        Toast.makeText(this@SignUpActivity, message, Toast.LENGTH_SHORT).show()
    }

    private suspend fun isValidSignUpDetails(): Boolean {
        if (encodeImage == null) {
            showToast("Select Profile Image")
            return false
        } else if (binding.edtName.text.toString().trim().isEmpty()) {
            showToast("Please Enter name")
            return false
        } else if (binding.edtEmail.text.toString().trim().isEmpty()) {
            showToast("Please Enter email")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.edtEmail.text.toString())
                .matches()
        ) {
            showToast("Email not valid")
            return false
        } else if (binding.edtPassword.text.toString().trim().isEmpty()) {
            showToast("Please Enter Password")
            return false
        } else if (binding.edtConfirmPassword.text.toString().trim().isEmpty()) {
            showToast("Please Enter Confirm Password")
            return false
        } else if (binding.edtConfirmPassword.text.toString()
                .trim() != binding.edtPassword.text.toString().trim()
        ) {
            showToast("Password and Confirm Password must be the same")
            return false
        }
        isEmailExists(binding.edtEmail.text.toString())
        return true
    }


    private suspend fun isEmailExists(email: String) {
        val docRef = db.collection(KEY_COLLECTION_USERS).whereEqualTo("email", email)
        val result = docRef.get().await()
        if (!result.isEmpty) {
            showToast("Email is Used")
        } else {
            signUp()
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