package com.quranapp.android.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResourcesVersions(
    @SerialName("urls") val urlsVersion: Long,
    @SerialName("translations") val translationsVersion: Long,
    @SerialName("recitations") val recitationsVersion: Long,
)