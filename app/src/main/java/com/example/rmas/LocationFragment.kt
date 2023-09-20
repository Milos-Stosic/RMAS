

package com.example.rmas

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory.decodeFile
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.rmas.data.LocationData
import com.example.rmas.databinding.FragmentLocationBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import java.util.UUID




@Suppress("DEPRECATION")
class LocationFragment : Fragment() {
    private lateinit var tvLocationAddress: TextView
    private lateinit var author: TextView
    private lateinit var likes: TextView
    private lateinit var btnLike: ImageView
    private lateinit var btnAddPhoto: ImageView
    private lateinit var storage: FirebaseStorage
    private lateinit var binding: FragmentLocationBinding
    private lateinit var imageView: ImageView
    private var pickedImageUri: Uri? = null
    private val LOCATION_IMG_STORAGE = "location_images"
    private lateinit var database: FirebaseDatabase


    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvLocationAddress = view.findViewById(R.id.tvLocationAddress)
        btnLike = view.findViewById(R.id.like_button)
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        storage = FirebaseStorage.getInstance()
        imageView = view.findViewById(R.id.locationImageView)
        author= view.findViewById(R.id.location_author)
        likes = view.findViewById(R.id.location_likes)
        database = FirebaseDatabase.getInstance()


        // Add OnClickListener for btnAddPhoto

        val locationId = arguments?.getString("clickedLocationId")
        if (locationId != null) {
            val db = Firebase.firestore
            val locationRef = db.collection("locations").document(locationId)
            locationRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val locationData = documentSnapshot.toObject(LocationData::class.java)
                        if (locationData != null) {
                            updateUI(locationData)
                        } else {
                            Log.e("MyApp", "Error getting location data: Location is null")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MyApp", "Error getting location data: $e")
                }
        }

        btnLike.setOnClickListener {



            val locationId = arguments?.getString("clickedLocationId")
            if (locationId != null) {
                val db = Firebase.firestore
                val locationRef = db.collection("locations").document(locationId)
                locationRef.get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            val currentUser = FirebaseAuth.getInstance().currentUser

                            val locationData = documentSnapshot.toObject(LocationData::class.java)
                            if (locationData != null) {
                                if(currentUser != null){
                                    val currentUserName = currentUser.displayName
                                    if(currentUserName != locationData.author){
                                        updateLikesInFirestore(locationData)
                                        addPointsForLikes(locationData)
                                        updateUI(locationData)
                                    }else{
                                        btnLike.isEnabled=false
                                    }
                                }

                            } else {
                                Log.e("MyApp", "Error getting location data: Location is null")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyApp", "Error getting location data: $e")
                    }
            }
        }

    }



    private fun updateLikesInFirestore(locationData: LocationData) {


        val db = FirebaseFirestore.getInstance()
        val locationDocument = db.collection("locations").document(locationData.id)
        val updates = mapOf(
            "likes" to FieldValue.increment(1)
        )
        locationDocument.update(updates)

    }


    private fun addPointsForLikes(locationData: LocationData)
    {

            val userId = locationData.authorId
            val pointsRef = database.getReference("users/$userId/brPoena")


            pointsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentPoints = snapshot.getValue(Long::class.java) ?: 0
                    val newPoints = currentPoints + 10
                    pointsRef.setValue(newPoints)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

    }





    private fun updateUI(locationData: LocationData) {

        if (!isAdded) {
            return
        }
        val adresa = locationData.address
        tvLocationAddress.text = "Adresa: $adresa"
        var autor = locationData.author
        author.text = "Autor:  $autor"
        val lajkovi = locationData.likes.toString()
        likes.text = "Likes: $lajkovi"

        val currentUser = FirebaseAuth.getInstance().currentUser


            (requireActivity() as AppCompatActivity).supportActionBar?.title = locationData.name

        val imageUrl = locationData.photos // Assuming locationData.photos is the image URL
        val storageRef = storage.getReference("/location_images/$imageUrl")

        val localFile = createTempFile("tempfile", ".jpg")

        try {
            storageRef.getFile(localFile).addOnSuccessListener {
                // Image download successful, now decode and display it
                var bitmap = decodeFile(localFile.absolutePath)
                imageView.setImageBitmap(bitmap)
            }.addOnFailureListener { exception ->
                // Handle the download failure
                Log.e("ImageDownload", "Failed to download image: ${exception.message}")
            }
        } catch (e: Exception) {
            // Handle other exceptions
            Log.e("ImageDownload", "An error occurred: ${e.message}")
        }

        imageView.visibility = View.VISIBLE

        btnAddPhoto.setOnClickListener {
            // Check if the user is authenticated
            if (currentUser != null) {
                val username = currentUser.displayName
                if(username==locationData.author) {
                    openGallery()
                    updateUI(locationData)
                }else{
                    Toast.makeText(context, "Samo autor moze da menja fotografiju", Toast.LENGTH_SHORT).show()

                }

            }else{

            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent != null) {
                val selectedImageUri: Uri? = intent.data

                pickedImageUri = selectedImageUri
                selectedImageUri?.let {
                    saveImage(selectedImageUri)
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
    private fun saveImage(uri: Uri) {
        val locationId = arguments?.getString("clickedLocationId")
        var naziv = UUID.randomUUID().toString()
        if (locationId != null) {
            val db = Firebase.firestore
            val locationRef = db.collection("locations").document(locationId)
            locationRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val locationData = documentSnapshot.toObject(LocationData::class.java)
                        if (locationData != null) {
                            naziv = locationData.name

                        }
                    }
                }
        }
        val storage = FirebaseStorage.getInstance()

        val db = FirebaseFirestore.getInstance()
        val markerRef = db.collection("locations").document(locationId!!)

        val selectedImageUri: Uri? = uri
        if (selectedImageUri != null) {
            val photoRef =
                storage.reference.child(
                    LOCATION_IMG_STORAGE
                ).child("$naziv.jpg")
            photoRef.putFile(pickedImageUri!!)
            var photo = "$naziv.jpg"
            val updates = mapOf(
                "photos" to photo
            )
            markerRef.update(updates)
        }
    }



}





