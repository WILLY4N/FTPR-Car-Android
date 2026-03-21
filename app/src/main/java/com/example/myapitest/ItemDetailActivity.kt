package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityItemDetailBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapitest.service.Result
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class ItemDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityItemDetailBinding

    private lateinit var item: Car

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        loadItem()
        setupGoogleMap()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        if(::item.isInitialized){
            loadItemInGoogleMap()
        }
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.deleteCTA.setOnClickListener {
            deleteCar()
        }
        binding.editCTA.setOnClickListener {
            editCar()
        }

    }

    private fun loadItem() {
        val itemId = intent.getStringExtra(ARG_ID) ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getItem(itemId) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        item = result.data
                        handleSuccess()
                    }
                    is Result.Error -> {
                        Toast.makeText(this@ItemDetailActivity, R.string.error_fetch_item, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupGoogleMap(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun handleSuccess() {
        binding.name.text = item.value.name
        binding.year.text = item.value.year
        binding.licence.setText(item.value.licence)
        binding.image.loadUrl(item.value.imageUrl)
        loadItemInGoogleMap()
    }

    private fun loadItemInGoogleMap(){
        if(!::mMap.isInitialized) return
        item.value.place?.let {
            binding.googleMapContent.visibility = View.VISIBLE
            val place = LatLng(it.lat, it.long)
            mMap.addMarker(
                MarkerOptions()
                    .position(place)
                    .title(item.value.name)
            )
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    place,
                    18f
                )
            )
        }
    }

    private fun deleteCar() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(item.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> handleSuccessDelete()
                    is Result.Error -> {
                        Toast.makeText(this@ItemDetailActivity, R.string.error_delete, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun editCar(){
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.updateCar(
                    item.id,
                    item.value.copy(licence = binding.licence.text.toString())
                )
            }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success -> {
                        Toast.makeText(this@ItemDetailActivity, R.string.sucess_update, Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is Result.Error -> {
                        Toast.makeText(this@ItemDetailActivity, R.string.error_update, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun handleSuccessDelete() {
        Toast.makeText(this, R.string.sucess_delete, Toast.LENGTH_LONG)
            .show()
        finish()
    }


    companion object{
        const val ARG_ID = "arg_id"

        fun newIntent(context: Context, itemId: String): Intent {
            return Intent(context, ItemDetailActivity::class.java).apply {
                putExtra(ARG_ID, itemId)
            }
        }
    }
}