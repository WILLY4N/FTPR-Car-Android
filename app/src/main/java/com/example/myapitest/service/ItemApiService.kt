package com.example.myapitest.service

import com.example.myapitest.model.Car
import com.example.myapitest.model.ItemCar
import retrofit2.http.GET
import retrofit2.http.Path

interface ItemApiService {
    @GET("car")
    suspend fun getCars(): List<ItemCar>

    @GET("car/{id}")
    suspend fun getItem(@Path("id") id: String): Car
}