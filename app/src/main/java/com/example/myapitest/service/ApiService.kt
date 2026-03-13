package com.example.myapitest.service

import com.example.myapitest.model.Car
import retrofit2.http.GET

interface ApiService {
    @GET("cars")
    suspend fun getCars(): List<Car>
}