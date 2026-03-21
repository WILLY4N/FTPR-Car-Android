package com.example.myapitest

import android.Manifest
import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapitest.databinding.ActivityNewItemBinding
import com.example.myapitest.model.ItemCar
import com.example.myapitest.model.ItemPlace
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NewItemActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewItemBinding

    private lateinit var mMap: GoogleMap
    private var selectedMarker: Marker? = null

    private lateinit var imageUri: Uri

    private var imageFile: File? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            binding.imageUrl.setText("Imagem Obtida")
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewItemBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        requestLocationPermission()
        setupGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        binding.mapContent.visibility = View.VISIBLE
        getDeviceLocation()
        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Lat: ${latLng.latitude}, Lng: ${latLng.longitude}")
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    loadCurrentLocation()
                } else {
                    Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
                }
            }
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    openCamera()
                }
            }
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.saveCta.setOnClickListener { saveCar() }
        binding.takePictureCta.setOnClickListener { takePicture() }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun openCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

        return FileProvider.getUriForFile(this, "com.example.myapitest.fileprovider", imageFile!!)

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationPermission() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLong = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, 16f))
            }
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun getDeviceLocation(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            loadCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation(){
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        fusedLocationClient
    }

    private fun saveCar() {
        if (!validateForm()) return

        uploadImageToFirebase()

    }

    private fun saveData() {
        val itemPlace = selectedMarker?.position?.let {
            ItemPlace(it.latitude, it.longitude)
        }
        CoroutineScope(Dispatchers.IO).launch {
            val id = SecureRandom().nextInt().toString()
            val itemCar = ItemCar(
                id = id,
                name = binding.name.text.toString(),
                year = binding.year.text.toString(),
                licence = binding.licence.text.toString(),
                imageUrl = binding.imageUrl.text.toString(),
                place = itemPlace
            )

            val result = safeApiCall { RetrofitClient.apiService.addItem(itemCar) }
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> handleOnSuccess()
                    is Result.Error -> handleOnError()
                }
            }
        }
    }

    private fun uploadImageToFirebase(){
        imageFile?.let{
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

            val baos = ByteArrayOutputStream()
            val imageBitmap = BitmapFactory.decodeFile(it.path)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            onLoadImage(true)

            imageRef.putBytes(data)
                .addOnFailureListener {
                    onLoadImage(false)
                    Toast.makeText(
                        this,
                        R.string.error_upload_image,
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnSuccessListener {
                    onLoadImage(false)
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        binding.imageUrl.setText(uri.toString())
                        saveData()
                    }
                }
        }
    }

    fun onLoadImage(isLoading: Boolean) {
        binding.loadImageProgress.visibility = if(isLoading) View.VISIBLE else View.GONE
        binding.takePictureCta.isEnabled = !isLoading
        binding.saveCta.isEnabled = !isLoading
    }

    private fun handleOnError() {
        Toast.makeText(
            this,
            R.string.error_add_item,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun handleOnSuccess() {
        Toast.makeText(
            this,
            R.string.sucess_add_item,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }


    private fun validateForm(): Boolean {
        var hasError = false
        if (binding.name.text.isNullOrBlank()) {
            binding.name.error = getString(R.string.required_field)
            hasError = true
        }
        if (binding.year.text.isNullOrBlank()) {
            binding.year.error = getString(R.string.required_field)
            hasError = true
        }
        if (binding.licence.text.isNullOrBlank()) {
            binding.licence.error = getString(R.string.required_field)
            hasError = true
        }
        if (binding.imageUrl.text.isNullOrBlank()) {
            binding.imageUrl.error = getString(R.string.required_field)
            hasError = true
        }
        return !hasError
    }

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1002

        fun newIntent(context: Context): Intent {
            return Intent(context, NewItemActivity::class.java)
        }
    }
}