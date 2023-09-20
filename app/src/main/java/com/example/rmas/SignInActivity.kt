package com.example.rmas

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rmas.databinding.ActivitySignInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts





class SignInActivity : AppCompatActivity() {

    private var pickedImageUri: Uri? = null

    private val PROFILE_IMG_STORAGE = "profile_images"

    private lateinit var imageView: ImageView


    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val selectedImageUri: Uri? = intent.data

                    pickedImageUri = selectedImageUri
                    selectedImageUri?.let {
                        imageView.setImageURI(selectedImageUri)

                    }
                }
            }
        }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }



    private lateinit var binding: ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseStorage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignInBinding.inflate(layoutInflater)

        setContentView(binding.root)
        val customDatabaseUrl = "https://projekat-rmas-default-rtdb.europe-west1.firebasedatabase.app/"

        imageView = findViewById(R.id.imageView)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance(customDatabaseUrl)
        firebaseStorage = FirebaseStorage.getInstance()
        binding.txtLogIn.setOnClickListener {
            val intent = Intent(this, LogInActivity::class.java)
            startActivity(intent)
        }

        binding.imageView.setOnClickListener {
            openGallery()
        }



        binding.btnSignUp.setOnClickListener {
            val userName = binding.edtUsername.text.toString()
            val email = binding.edtEmail.text.toString()
            val pass = binding.edtPassword.text.toString()
            val confirmPass = binding.edtConfirmPassword.text.toString()
            val number = binding.edtMobile.text.toString()
            val brPoena = 0

            if (number.isNotBlank() && number.isNotEmpty() && userName.isNotBlank() && email.isNotBlank() && pass.isNotEmpty() && pass.isNotBlank() && confirmPass.isNotBlank() && confirmPass.isNotEmpty()) {
                if (number.length != 10) {
                    Toast.makeText(
                        this@SignInActivity,
                        "Mobile phone number must be in length of 10",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (pass == confirmPass) {
                        checkIfUsernameExists(userName) { isTaken ->
                            if (!isTaken) {
                                checkIfNumberExists(number) { exists ->
                                    if (!exists) {
                                        firebaseAuth.createUserWithEmailAndPassword(email, pass)
                                            .addOnCompleteListener {
                                                if (it.isSuccessful) {

                                                    val user = firebaseAuth.currentUser
                                                    val request = UserProfileChangeRequest.Builder()
                                                        .setDisplayName(userName).build()
                                                    user?.updateProfile(request)

                                                    if (pickedImageUri != null) {
                                                        val photoRef =
                                                            firebaseStorage.reference.child(
                                                                PROFILE_IMG_STORAGE
                                                            ).child("$userName.jpg")
                                                        photoRef.putFile(pickedImageUri!!)
                                                            .addOnCompleteListener {
                                                                Toast.makeText(
                                                                    this@SignInActivity,
                                                                    "Fotografija dodata",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }.addOnFailureListener {
                                                                Toast.makeText(
                                                                    this@SignInActivity,
                                                                    "Neuspesno dodavanje fotografije!",
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                    }

                                                    saveUserDataToDatabase(

                                                        userName,
                                                        number,
                                                        brPoena

                                                    )

                                                    Toast.makeText(
                                                        this@SignInActivity,

                                                        "Profil kreiran",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    val intent =
                                                        Intent(this, LogInActivity::class.java)
                                                    startActivity(intent)
                                                } else {
                                                    Toast.makeText(
                                                        this,
                                                        it.exception.toString(),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(
                                            this@SignInActivity,
                                            "Korisnik sa tim brojem telefona vec postoji!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    this@SignInActivity,
                                    "Korisnicko ime je zauzeto!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Lozinke se ne poklapaju!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Sva polja moraju biti popunjena!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }




    private fun checkIfNumberExists(number: String, onResult: (Boolean) -> Unit) {
        val databaseRef = firebaseDatabase.reference.child("numbers").child(number)
        databaseRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                onResult(dataSnapshot.exists())
            } else {
                onResult(false)
            }
        }
    }

    private fun checkIfUsernameExists(userName: String, onResult: (Boolean) -> Unit) {
        val databaseReference = firebaseDatabase.reference.child("userNames").child(userName)
        databaseReference.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val dataSnapshot = task.result
                    onResult(dataSnapshot.exists())
                } else {
                    onResult(false)
                }
            }
    }

    private fun saveUserDataToDatabase(userName: String, number: String, brPoena: Number) {
                    val currentUser = firebaseAuth.currentUser
                    currentUser?.let { user ->
                        val uid = user.uid
                        val databaseReference = firebaseDatabase.reference.child("users").child(uid)
                        val usernameReference = firebaseDatabase.reference.child("userNames")
                        val numberRef = firebaseDatabase.reference.child("numbers")
                        val userData = com.example.rmas.data.model.UserData(userName, number, brPoena )
            
                        databaseReference.setValue(userData)
                            .addOnSuccessListener {
                                usernameReference.child(userName).setValue(true).addOnSuccessListener {
            
                                    numberRef.child(number).setValue(true).addOnSuccessListener {
                                        val intent = Intent(this, SignInActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                this@SignInActivity,
                                                "Korisnik sa tim brojem telefona vec postoji!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            "Korisnicko ime je zauzeto!",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()
                                    }
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, exception.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                }
   
   
   
   
    }





       







