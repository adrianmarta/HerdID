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
    @GET("api/animals/owner/{ownerId}")
    suspend fun getAnimalsByOwnerId(
        @Path("ownerId") ownerId: String,
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
    @GET("/api/folders/user/{ownerId}")
    suspend fun getFolders(
        @Header("Authorization") token: String,
        @Path("ownerId") ownerId: String
    ): Response<List<Folder>>
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
        @Path("id") animalId:String,
        @Header("Authorization") token: String
    ): Response<Animal>
}
