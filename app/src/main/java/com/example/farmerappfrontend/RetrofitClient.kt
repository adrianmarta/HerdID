// RetrofitClient.kt
package com.example.farmerappfrontend

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.100.42:8080" // Ensure this is your backend URL

    private val gson = GsonBuilder()
        .setLenient()  // Enables lenient mode for non-standard JSON
        .create()

    // Single Retrofit instance with lenient Gson
    val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val apiService: ApiService by lazy {
        instance.create(ApiService::class.java)
    }
}
