// RetrofitClient.kt
package com.example.farmerappfrontend

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.131:8080" // Ensure this is your backend URL

    private val gson = GsonBuilder()
        .setLenient()  // Enables lenient mode for non-standard JSON
        .create()

    // Single Retrofit instance with lenient Gson
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}
