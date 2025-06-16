// ApiService.kt
package com.example.farmerappfrontend



import com.google.android.gms.common.internal.safeparcel.SafeParcelable.Param
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiService {

    @POST("/api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse
    @GET("/api/users/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): UserProfile
    // ApiService.kt
    @GET("api/animals/owner-animals")
    suspend fun getAnimalsByOwnerId(
        @Header("Authorization") token: String
    ): Response<List<Animal>>


    @DELETE("/api/animals/{id}")
    suspend fun deleteAnimal(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<Void>

    @POST("/api/folders")
    suspend fun createFolder(
        @Header("Authorization") token: String,
        @Body folderRequest: FolderRequest
    ): Response<FolderResponse>
    @GET("/api/folders/user")
    suspend fun getFolders(
        @Header("Authorization") token: String,
    ): Response<List<Folder>>
    @GET("/api/folders/{folderId}")
    suspend fun getFolder(
        @Path("folderId") folderId: String,
        @Header("Authorization") token: String
    ): Response<Folder>
    @GET("/api/folders/{folderId}/animals")
    suspend fun getAnimalsByFolderId(
        @Path("folderId") folderId: String,
        @Header("Authorization") token: String
    ): Response<List<Animal>>
    @PUT("/api/folders/{folderId}/add-existing-animal/{animalId}")
    suspend fun addAnimalToFolder(
        @Path("folderId") folderId: String,
        @Path("animalId") animalId:String,
        @Header("Authorization") token: String

    ): Response<Void>
    @GET("api/folders/compare/{folderId1}/{folderId2}")
    suspend fun compareFolders(
        @Path("folderId1") folderId1: String,
        @Path("folderId2") folderId2: String,
        @Header("Authorization") token: String
    ): Response<List<String>>

    @DELETE("/api/folders/{id}")
    suspend fun deleteFolder(
        @Path("id") folderId: String,
        @Header("Authorization") token: String
    ): Response<Void>

    @GET("/api/animals/exists/{id}")
    suspend fun checkAnimalExists(
        @Path("id") animalId: String,
        @Header("Authorization") token: String
    ): Response<Boolean>
    @GET("/api/animals/list")
    suspend fun getAnimalsByIds(
        @Query("ids") ids: List<String>,
        @Header("Authorization") token: String
    ): Response<List<Animal>>

    @PUT("/api/folders/{folderId}/remove-animals")
    suspend fun removeAnimalsFromFolder(
        @Path("folderId") folderId: String,
        @Body animalIds: List<String>,
        @Header("Authorization") token: String
    ): Response<Void>
    @PUT("/api/folders/{folderId}/add-animals")
    suspend fun addAnimalsToFolder(
        @Path("folderId") folderId: String,
        @Body animalIds: List<String>,
        @Header("Authorization") authorization: String
    ): Response<Unit>
    @GET("api/animals/{id}")
    suspend fun getAnimalDetails(
        @Path("id") animalId: String,
        @Header("Authorization") token: String
    ): Response<AnimalDetails>
    @GET("api/animals/{id}")
    suspend fun getAnimal(
        @Path("id") animalId: String,
        @Header("Authorization") token: String
    ): Response<Animal>
    @PUT("api/animals/{id}")
    suspend fun updateAnimal(
        @Path("id") animalId: String,
        @Header("Authorization") token: String,
        @Body updateRequest: AnimalUpdateRequest
    ): Response<AnimalDetails>
    @POST("api/events/animal/{animalId}")
    suspend fun postEvent(
        @Path("animalId") animalId: String,
        @Header("Authorization") token: String,
        @Body event: Map<String, @JvmSuppressWildcards Any>
    ): Response<Void>
    @GET("api/events/animal/{animalId}")
    suspend fun getEventsForAnimal(
        @Path("animalId") animalId: String,
        @Header("Authorization") token: String
    ): Response<List<AnimalEvent>>

    @POST("/api/auth/register")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<Void>

    @POST("/api/animals/batch")
    suspend fun addAnimalsBatch(
        @Header("Authorization") token: String,
        @Body animals: List<AnimalDetails>
    ): Response<Map<String, Any>>
    @GET("/api/animals/species")
    suspend fun getSpecies(): Response<List<Map<String, String>>>

    @GET("/api/events/types")
    suspend fun getEventTypes(): Response<Map<String, Map<String, String>>>

    @GET("/api/animals/search")
    suspend fun searchAnimals(
        @Query("query") query: String,
        @Header("Authorization") token: String
    ): Response<List<Animal>>

    @GET("/api/animals/by-birth-date")
    suspend fun getAnimalsByBirthDate(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Header("Authorization") token: String
    ): Response<List<Animal>>

    @GET("/api/animals/by-sickness")
    suspend fun getAnimalsBySickness(
        @Query("sicknessName") sicknessName: String,
        @Header("Authorization") token: String
    ): Response<List<Animal>>

    @GET("/api/animals/by-vaccination")
    suspend fun getAnimalsByVaccination(
        @Query("vaccineName") vaccineName: String,
        @Header("Authorization") token: String
    ): Response<List<Animal>>

    // Get all animal events
    @GET("api/events")
    suspend fun getAllEvents(
        @Header("Authorization") token: String
    ): Response<List<AnimalEvent>>

    @GET("api/statistics")
    suspend fun getStatistics(
        @Header("Authorization") token: String,
        @Query("folderId") folderId: String? = null
    ): Response<StatisticsResponse>

    @POST("/api/animals")
    suspend fun addAnimal(
        @Header("Authorization") token: String,
        @Body animal: AnimalDetails
    ): Response<Unit>

    @GET("/api/events/sickness-names")
    suspend fun getSicknessNames(@Header("Authorization") token: String): Response<List<String>>

    @GET("/api/events/vaccine-names")
    suspend fun getVaccineNames(@Header("Authorization") token: String): Response<List<String>>
}
