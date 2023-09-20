@file:Suppress("DEPRECATION")

package com.example.rmas

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rmas.data.LocationData
import com.example.rmas.databinding.FragmentMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import kotlin.math.pow

class MapFragment : Fragment(), OnMapReadyCallback {


    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var binding: FragmentMapBinding
    private lateinit var gMap: GoogleMap
    private lateinit var savedMarkers: MutableList<LocationData>
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var addMarker: FloatingActionButton
    private lateinit var btnFilter: FloatingActionButton
    private lateinit var btnLeaderboard: FloatingActionButton
    private lateinit var database: FirebaseDatabase




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Map"

        savedMarkers = mutableListOf()
        addMarker = view.findViewById(R.id.addMarker)
        btnFilter = view.findViewById(R.id.btnFilter)
        btnLeaderboard = view.findViewById(R.id.leaderBoard)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance("https://projekat-rmas-default-rtdb.europe-west1.firebasedatabase.app/")

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        readMarkersList()

        addMarker.setOnClickListener {
            showAddMarkerDialog()
            addPointsToCurrent()
        }


        btnFilter.setOnClickListener {
            currentLocation { currentLoc ->
                if (currentLoc != null) {
                    showFilterDialog(currentLoc)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Error getting your location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


        btnLeaderboard.setOnClickListener{

            val navController = findNavController()
            navController.navigate(R.id.action_mapFragment_to_leaderboardFragment)



        }
    }//end of onViewCreated

    private fun addPointsToCurrent()
    {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val pointsRef = database.getReference("users/$userId/brPoena")

            // Increment user points by 10
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
    }


    private fun showAddMarkerDialog() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Get the FusedLocationProviderClient
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())

            // Request the last known location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // Create and display the "Add Marker" dialog with the current location
                        showAddMarkerDialogWithLocation(location)
                    } else {
                        // Handle the case where the location is null
                        Toast.makeText(
                            requireContext(),
                            "Unable to determine your current location.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            // If location permission is not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showAddMarkerDialogWithLocation(location: Location) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker_info, null)

        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val addressEditText = dialogView.findViewById<EditText>(R.id.addressEditText)





        val latitude = location.latitude
        val longitude = location.longitude

        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user is authenticated
        if (currentUser != null) {
            val username = currentUser.displayName // Retrieve the username if available
            val uid = currentUser.uid
            val dialogBuilder = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setTitle("Add Marker")
                .setPositiveButton("Save") { dialog, _ ->
                    val name = nameEditText.text.toString()
                    val address = addressEditText.text.toString()


                    if (name.isNotEmpty() && address.isNotEmpty()) {
                        val locationData = LocationData(
                            id = "",
                            name = name,
                            address = address,
                            longitude = longitude,
                            latitude = latitude,
                            photos =  "",
                            timeCreated = Timestamp.now(),
                            author = username ?: "",
                            authorId = uid
                        )

                        val db = FirebaseFirestore.getInstance()
                        val collectionRef = db.collection("locations")

                        collectionRef.add(locationData)
                            .addOnSuccessListener { documentReference ->
                                val data = hashMapOf("id" to documentReference.id)
                                locationData.id = documentReference.id
                                db.collection("locations").document(documentReference.id)
                                    .set(data, SetOptions.merge())}

                        // Add a marker on the map with the specified location
                        gMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(locationData.latitude, locationData.longitude))
                                .title(locationData.name)
                                .snippet(locationData.address)
                        )

                        dialog.dismiss()
                    } else {
                        // Show an error message or handle invalid input
                        Toast.makeText(
                            requireContext(),
                            "Please fill in all fields and select an image.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)

            val dialog = dialogBuilder.create()
            dialog.show()
        } else {
            Toast.makeText(
                requireContext(),
                "User is not authenticated. Please log in.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        checkLocationPermission()
        showSavedMarkersOnMap()

        gMap.setOnMarkerClickListener { marker ->

            val clickedLocation = marker.tag as? LocationData
            if (clickedLocation != null) {

                    val args = Bundle()
                    args.putString("clickedLocationId", clickedLocation.id)
                    findNavController().navigate(R.id.action_mapFragment_to_locationFragment, args)


            }
            true // Consume the click event
        }
    }

    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gMap.isMyLocationEnabled = true
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener(
                requireActivity()
            ) { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val userLocation = LatLng(latitude, longitude)
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun checkLocationPermission() {
        if (!isAdded) {
            return
        }
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            }
        }
    }



    private fun showSavedMarkersOnMap() {
        gMap.clear()

        if (savedMarkers.isEmpty()) return

        for (location in savedMarkers) {
            val latLng = LatLng(location.latitude, location.longitude)
            val markerTitle = location.name
            val marker = gMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
            if (marker != null) {
                marker.tag = location
            }
        }

    }
    private fun readMarkersList() {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("locations") // Use the correct collection name

        // Listen for changes in the Firestore collection
        collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Handle the error
                Log.e(TAG,"Error listening for changes: $error")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val markers: MutableList<LocationData> = mutableListOf()
                for (document in snapshot) {
                    val location = document.toObject(LocationData::class.java)
                    markers.add(location)
                }

                // Update the savedMarkers list and show them on the map
                onMarkersReady(markers)
            } else {
                Log.e(TAG, "Snapshot is null")
            }
        }
    }
    private fun onMarkersReady(markers: List<LocationData>) {
        savedMarkers = markers.toMutableList()

        checkLocationPermission()
        showSavedMarkersOnMap()
    }


    private fun showFilterDialog(currentLoc:LatLng) {
        readMarkersList()
        val dialogView = layoutInflater.inflate(R.layout.filter_dialog, null)
        val authorEditText = dialogView.findViewById<EditText>(R.id.filter_marker_author_edittext)
        val numberPicker = dialogView.findViewById<SeekBar>(R.id.filter_marker_radius_seekbar)
        val selectedRadiusTextView = dialogView.findViewById<TextView>(R.id.selected_radius_text)

        numberPicker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Update a TextView to display the selected radius
                selectedRadiusTextView.text = "Radius: $progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })




        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Filter Locations")
            .setPositiveButton("Filter"){_,_->
                val author= authorEditText.text.toString()
                val selectedRadius = numberPicker.progress
                applyFilters(author,selectedRadius,currentLoc)
            }.setNegativeButton("Cancel",null)

        val dialog = dialogBuilder.create()
        dialog.show()

    }
    private fun applyFilters(filterAuthor: String, selectedRadius:Int,currentLoc: LatLng) {

        val filteredList = savedMarkers.filter { location ->
            var authorMatch = location.author.contains(filterAuthor, ignoreCase = true)

            if(filterAuthor=="")
                authorMatch=true

            Log.d("Filter Values:","Author Matched: $authorMatch")
            authorMatch &&calculateDistance(currentLoc.latitude,currentLoc.longitude,location.latitude,location.longitude)<selectedRadius
        }

        savedMarkers=filteredList.toMutableList()
        showSavedMarkersOnMap()
    }




    private fun currentLocation(callback: (LatLng?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    // Create a LatLng object and pass it to the callback
                    val currentLatLng = LatLng(latitude, longitude)
                    Log.d("Current Location", "Latitude: $latitude, Longitude: $longitude")
                    callback(currentLatLng)
                } else {
                    // Handle the case where location is null
                    Log.d("Current Location", "Location not available.")
                    callback(null)
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            callback(null)
        }
    }
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radiusOfEarth = 6371 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad
        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radiusOfEarth * c
    }
}