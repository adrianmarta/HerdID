package com.example.farmerappfrontend

// Models.kt


data class LoginRequest(val id: String, val cnp: String)

data class LoginResponse(val token: String)

data class UserProfile(val id: String, val name: String, val dob: String, val address: String,val phoneNumber:String)

data class Animal(val id: String, val gender: String, val birthDate: String)
data class FolderRequest(
    val name: String,    // Folder name
    val ownerId: String  // Owner ID
)
data class Folder(
    val id: String,
    val name: String,
    val ownerId: String
)

