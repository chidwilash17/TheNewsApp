package com.example.thenewsapp.models

import java.io.Serializable // Add if passing Source directly or if Article is only Serializable


data class Source(
    val id: String?,
    val name: String?
) : Serializable // Or Parcelable
