// ApiService.kt
package com.example.farmerappfrontend


import com.google.android.gms.common.api.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @GET("/api/users/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): UserProfile
    // ApiService.kt
    @GET("/owner/{ownerId}")
    suspend fun getAnimalsByOwnerId(
        @Path("ownerId") ownerId: String,
        @Header("Authorization") token: String
    ): Response<List<Animal>>

}
