package com.example.farmerappfrontend

import com.google.gson.annotations.SerializedName

// Models.kt


data class LoginRequest(val id: String, val email: String, val password:String)

data class LoginResponse(val token: String)

data class UserProfile(val id: String, val name: String, val dob: String, val address: String,val phoneNumber:String)
data class Animal(val id: String, val gender: String, val birthDate: String,val species:String)
data class AnimalDetails(val id: String, val gender: String, val birthDate: String,val species:String,val producesMilk:Boolean)
data class FolderRequest(
    val name: String // Owner ID
)
data class ScannedAnimalStatus(
    val animal: Animal? = null,
    val id: String,
    val status: AnimalStatus
)

enum class AnimalStatus {
    IN_FOLDER,
    NOT_IN_FOLDER_EXISTS_GLOBALLY,
    NEW_ANIMAL
}
data class AnimalEvent(
    val id: String,
    val animalId: String,
    val eventType: String,
    val eventDate: String,
    val details: Map<String, Any>
)

data class RegisterRequest(
    val id: String,
    val email: String,
    val password: String,
    val name: String,
    val dob: String,
    val address: String,
    val phoneNumber: String
)

data class Folder(
    val id: String,
    val name: String,
    val ownerId: String
)
data class FolderResponse(
    val id:String,
    val name:String,
    val ownerId: String
)
data class CompareFoldersResponse(
    val animals: List<String>
)

data class StatisticsResponse(
    @SerializedName("milkProducerCount") val milkProducerCount: Int = 0,
    @SerializedName("diseaseCounts") val diseaseCounts: Map<String, Int>? = emptyMap(),
    @SerializedName("vaccineCounts") val vaccineCounts: Map<String, Int>? = emptyMap(),
    @SerializedName("birthsByYear") val birthsByYear: Map<String, Int>? = emptyMap(),
    @SerializedName("totalAnimalCount") val totalAnimalCount: Int = 0,

    @SerializedName("animalBirthYears") val animalBirthYears: Map<String, Int>? = emptyMap(),
    @SerializedName("animalGenders") val animalGenders: Map<String, String>? = emptyMap(),
    @SerializedName("animalsGaveBirthByYear") val animalsGaveBirthByYear: Map<String, Set<String>>? = emptyMap(),
    @SerializedName("eligibleFemalesByYear") val eligibleFemalesByYear: Map<String, Int>? = emptyMap(),
    @SerializedName("femalesGaveBirthByYear") val femalesGaveBirthByYear: Map<String, Set<String>>? = emptyMap()
)


data class AnimalUpdateRequest(
    val species: String? = null,
    val gender: String? = null,
    val birthDate: String? = null,
    val producesMilk: Boolean? = null
)
